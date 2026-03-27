package com.dionialves.AsteraComm.asterisk.provisioning;

import com.dionialves.AsteraComm.asterisk.aors.AorRepository;
import com.dionialves.AsteraComm.asterisk.aors.Aors;
import com.dionialves.AsteraComm.asterisk.auth.Auth;
import com.dionialves.AsteraComm.asterisk.auth.AuthRepository;
import com.dionialves.AsteraComm.asterisk.dialplan.DialplanGeneratorService;
import com.dionialves.AsteraComm.asterisk.endpoint.Endpoint;
import com.dionialves.AsteraComm.asterisk.endpoint.EndpointRepository;
import com.dionialves.AsteraComm.asterisk.endpoint.EndpointStatusRepository;
import com.dionialves.AsteraComm.asterisk.extension.Extension;
import com.dionialves.AsteraComm.asterisk.extension.ExtensionRepository;
import com.dionialves.AsteraComm.asterisk.registration.PsRegistration;
import com.dionialves.AsteraComm.asterisk.registration.PsRegistrationRepository;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.did.DID;
import com.dionialves.AsteraComm.trunk.Trunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AsteriskProvisioningService {

    private final AorRepository aorRepository;
    private final AuthRepository authRepository;
    private final EndpointRepository endpointRepository;
    private final ExtensionRepository extensionRepository;
    private final EndpointStatusRepository endpointStatusRepository;
    private final PsRegistrationRepository psRegistrationRepository;
    private final AmiService amiService;
    private final DialplanGeneratorService dialplanGeneratorService;

    // =========================================================
    // Circuit provisioning
    // =========================================================

    @Transactional
    public void provision(Circuit circuit) {
        String number = circuit.getNumber();
        String trunkName = circuit.getTrunkName();

        Aors aors = new Aors();
        aors.setId(number);
        aors.setDefaultExpiration("60");
        aors.setMaxContacts("1");
        aors.setRemoveExisting("yes");
        aors.setQualifyFrequency("30");
        aorRepository.save(aors);

        Auth auth = new Auth();
        auth.setId(number);
        auth.setAuthType("userpass");
        auth.setUsername(number);
        auth.setPassword(circuit.getPassword());
        authRepository.save(auth);

        Endpoint endpoint = new Endpoint();
        endpoint.setId(number);
        endpoint.setAors(aors);
        endpoint.setAuth(auth);
        endpoint.setContext("internal-" + trunkName);
        endpoint.setDisallow("all");
        endpoint.setAllow("ulaw,alaw");
        endpoint.setDirect_media("no");
        endpoint.setForce_rport("yes");
        endpoint.setRewriteContact("yes");
        endpoint.setRtpSymmetric("yes");
        endpoint.setCallerid(number);
        endpointRepository.save(endpoint);

        amiService.sendCommand("pjsip reload");
    }

    @Transactional
    public void reprovision(Circuit circuit, String previousTrunkName) {
        String number = circuit.getNumber();
        String newTrunkName = circuit.getTrunkName();

        authRepository.findById(number).ifPresent(auth -> {
            auth.setPassword(circuit.getPassword());
            authRepository.save(auth);
        });

        if (!previousTrunkName.equals(newTrunkName)) {
            endpointRepository.findById(number).ifPresent(endpoint -> {
                endpoint.setContext("internal-" + newTrunkName);
                endpointRepository.save(endpoint);
            });
        }

        amiService.sendCommand("pjsip reload");
    }

    @Transactional
    public void deprovision(Circuit circuit) {
        String number = circuit.getNumber();

        endpointRepository.findById(number).ifPresent(endpoint -> {
            endpointStatusRepository.deleteByEndpoint(endpoint);
            endpointRepository.delete(endpoint);
            authRepository.delete(endpoint.getAuth());
            aorRepository.delete(endpoint.getAors());
        });

        amiService.sendCommand("pjsip reload");
        amiService.sendCommand("dialplan reload");
    }

    // =========================================================
    // Trunk provisioning
    // =========================================================

    @Transactional
    public void provisionTrunk(Trunk trunk) {
        String name = trunk.getName();
        String host = trunk.getHost();

        Aors aors = new Aors();
        aors.setId(name);
        aors.setContact("sip:" + host);
        aors.setQualifyFrequency("30");
        aorRepository.save(aors);

        Auth auth = new Auth();
        auth.setId(name);
        auth.setAuthType("userpass");
        auth.setUsername(trunk.getUsername());
        auth.setPassword(trunk.getPassword());
        authRepository.save(auth);

        Endpoint endpoint = new Endpoint();
        endpoint.setId(name);
        endpoint.setAors(aors);
        endpoint.setContext("pstn-" + name);
        endpoint.setDisallow("all");
        endpoint.setAllow("ulaw,alaw");
        endpoint.setDirect_media("no");
        endpoint.setOutboundAuth(name);
        endpointRepository.save(endpoint);

        PsRegistration registration = new PsRegistration();
        registration.setId(name);
        registration.setServerUri("sip:" + host);
        registration.setClientUri("sip:" + trunk.getUsername() + "@" + host);
        registration.setOutboundAuth(name);
        registration.setRetryInterval("60");
        psRegistrationRepository.save(registration);

        createOutboundExtensions(trunk);

        amiService.sendCommand("pjsip reload");
        dialplanGeneratorService.generateAndReload();
    }

    @Transactional
    public void reprovisionTrunk(Trunk trunk) {
        String name = trunk.getName();
        String host = trunk.getHost();

        authRepository.findById(name).ifPresent(auth -> {
            auth.setPassword(trunk.getPassword());
            authRepository.save(auth);
        });

        psRegistrationRepository.findById(name).ifPresent(reg -> {
            reg.setServerUri("sip:" + host);
            reg.setClientUri("sip:" + trunk.getUsername() + "@" + host);
            psRegistrationRepository.save(reg);
        });

        extensionRepository.deleteByExtenAndContext("_X.", "internal-" + name);
        createOutboundExtensions(trunk);

        amiService.sendCommand("pjsip reload");
    }

    @Transactional
    public void deprovisionTrunk(Trunk trunk) {
        String name = trunk.getName();

        extensionRepository.deleteByExtenAndContext("_X.", "internal-" + name);
        psRegistrationRepository.findById(name).ifPresent(psRegistrationRepository::delete);
        endpointRepository.findById(name).ifPresent(endpointRepository::delete);
        authRepository.findById(name).ifPresent(authRepository::delete);
        aorRepository.findById(name).ifPresent(aorRepository::delete);

        amiService.sendCommand("pjsip reload");
        dialplanGeneratorService.generateAndReload();
    }

    // =========================================================
    // DID provisioning
    // =========================================================

    @Transactional
    public void provisionDid(DID did, Circuit circuit) {
        String context = "pstn-" + circuit.getTrunkName();
        String exten = did.getNumber();
        String circuitNumber = circuit.getNumber();

        Extension dial = new Extension();
        dial.setContext(context);
        dial.setExten(exten);
        dial.setPriority(1);
        dial.setApp("Dial");
        dial.setAppdata("PJSIP/" + circuitNumber + ",60");
        extensionRepository.save(dial);

        Extension hangup = new Extension();
        hangup.setContext(context);
        hangup.setExten(exten);
        hangup.setPriority(2);
        hangup.setApp("Hangup");
        extensionRepository.save(hangup);

        amiService.sendCommand("dialplan reload");
    }

    @Transactional
    public void deprovisionDid(String didNumber, String trunkName) {
        extensionRepository.deleteByExtenAndContext(didNumber, "pstn-" + trunkName);
        amiService.sendCommand("dialplan reload");
    }

    private void createOutboundExtensions(Trunk trunk) {
        String name = trunk.getName();
        String context = "internal-" + name;
        String prefix = trunk.getPrefix() != null ? trunk.getPrefix() : "";

        Extension noop = new Extension();
        noop.setContext(context);
        noop.setExten("_X.");
        noop.setPriority(1);
        noop.setApp("NoOp");
        noop.setAppdata("Chamada para ${EXTEN}");
        extensionRepository.save(noop);

        Extension dial = new Extension();
        dial.setContext(context);
        dial.setExten("_X.");
        dial.setPriority(2);
        dial.setApp("Dial");
        dial.setAppdata("PJSIP/" + prefix + "${EXTEN}@" + name + ",60");
        extensionRepository.save(dial);

        Extension hangup = new Extension();
        hangup.setContext(context);
        hangup.setExten("_X.");
        hangup.setPriority(3);
        hangup.setApp("Hangup");
        extensionRepository.save(hangup);
    }
}
