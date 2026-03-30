package com.dionialves.AsteraComm.trunk;

import com.dionialves.AsteraComm.asterisk.aors.AorRepository;
import com.dionialves.AsteraComm.asterisk.auth.AuthRepository;
import com.dionialves.AsteraComm.asterisk.endpoint.EndpointRepository;
import com.dionialves.AsteraComm.asterisk.endpoint.EndpointStatusRepository;
import com.dionialves.AsteraComm.asterisk.extension.ExtensionRepository;
import com.dionialves.AsteraComm.asterisk.dialplan.DialplanGeneratorService;
import com.dionialves.AsteraComm.asterisk.endpointidip.PsEndpointIdIp;
import com.dionialves.AsteraComm.asterisk.endpointidip.PsEndpointIdIpRepository;
import com.dionialves.AsteraComm.asterisk.provisioning.AmiService;
import com.dionialves.AsteraComm.asterisk.provisioning.AsteriskProvisioningService;
import com.dionialves.AsteraComm.asterisk.registration.PsRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Testa o provisionamento de troncos:
 * - CREDENTIAL: ps_endpoints.context = "pstn-<nome>", extensions de saída em "internal-<nome>"
 * - IP_AUTH: ps_identify criado, sem ps_auths/ps_registrations
 */
@ExtendWith(MockitoExtension.class)
class TrunkProvisioningContextTest {

    @Mock private AorRepository aorRepository;
    @Mock private AuthRepository authRepository;
    @Mock private EndpointRepository endpointRepository;
    @Mock private ExtensionRepository extensionRepository;
    @Mock private EndpointStatusRepository endpointStatusRepository;
    @Mock private PsRegistrationRepository psRegistrationRepository;
    @Mock private PsEndpointIdIpRepository psEndpointIdIpRepository;
    @Mock private AmiService amiService;
    @Mock private DialplanGeneratorService dialplanGeneratorService;

    @InjectMocks
    private AsteriskProvisioningService provisioningService;

    private Trunk trunkCredential;
    private Trunk trunkCredentialComPrefix;
    private Trunk trunkIpAuth;

    @BeforeEach
    void setUp() {
        trunkCredential = new Trunk();
        trunkCredential.setName("opasuite");
        trunkCredential.setHost("sip.opasuite.com.br");
        trunkCredential.setUsername("user123");
        trunkCredential.setPassword("senha123");
        trunkCredential.setPrefix(null);
        trunkCredential.setAuthType(TrunkAuthType.CREDENTIAL);

        trunkCredentialComPrefix = new Trunk();
        trunkCredentialComPrefix.setName("tellcheap-cred");
        trunkCredentialComPrefix.setHost("sip.tellcheap.com");
        trunkCredentialComPrefix.setUsername("user456");
        trunkCredentialComPrefix.setPassword("senha456");
        trunkCredentialComPrefix.setPrefix("8712");
        trunkCredentialComPrefix.setAuthType(TrunkAuthType.CREDENTIAL);

        trunkIpAuth = new Trunk();
        trunkIpAuth.setName("tellcheap");
        trunkIpAuth.setHost("sip.tellcheap.com.br");
        trunkIpAuth.setAuthType(TrunkAuthType.IP_AUTH);
        trunkIpAuth.setIdentifyMatch("sip.tellcheap.com.br");
        trunkIpAuth.setPrefix(null);
    }

    // ── CREDENTIAL: contexto do endpoint ──────────────────────────────────────

    @Test
    void provisionTrunk_credential_shouldSetEndpointContextToPstnPrefix() {
        provisioningService.provisionTrunk(trunkCredential);

        verify(endpointRepository).save(argThat(e ->
                e.getId().equals("opasuite")
                && e.getContext().equals("pstn-opasuite")));
    }

    // ── CREDENTIAL: extensions de saída — sem prefix ──────────────────────────

    @Test
    void provisionTrunk_credential_shouldCreateNoOpExtensionInInternalContext() {
        provisioningService.provisionTrunk(trunkCredential);

        verify(extensionRepository).save(argThat(e ->
                e.getContext().equals("internal-opasuite")
                && e.getExten().equals("_X.")
                && e.getPriority() == 1
                && e.getApp().equals("NoOp")));
    }

    @Test
    void provisionTrunk_credential_shouldCreateDialExtensionWithoutPrefix() {
        provisioningService.provisionTrunk(trunkCredential);

        verify(extensionRepository).save(argThat(e ->
                e.getContext().equals("internal-opasuite")
                && e.getExten().equals("_X.")
                && e.getPriority() == 2
                && e.getApp().equals("Dial")
                && e.getAppdata().equals("PJSIP/${EXTEN}@opasuite,60")));
    }

    @Test
    void provisionTrunk_credential_shouldCreateHangupExtension() {
        provisioningService.provisionTrunk(trunkCredential);

        verify(extensionRepository).save(argThat(e ->
                e.getContext().equals("internal-opasuite")
                && e.getExten().equals("_X.")
                && e.getPriority() == 3
                && e.getApp().equals("Hangup")));
    }

    @Test
    void provisionTrunk_credential_shouldCreateDialExtensionWithPrefix() {
        provisioningService.provisionTrunk(trunkCredentialComPrefix);

        verify(extensionRepository).save(argThat(e ->
                e.getContext().equals("internal-tellcheap-cred")
                && e.getPriority() == 2
                && e.getApp().equals("Dial")
                && e.getAppdata().equals("PJSIP/8712${EXTEN}@tellcheap-cred,60")));
    }

    // ── CREDENTIAL: cria ps_auths e ps_registrations ──────────────────────────

    @Test
    void provisionTrunk_credential_shouldCreateAuth() {
        provisioningService.provisionTrunk(trunkCredential);

        verify(authRepository).save(argThat(a ->
                a.getId().equals("opasuite")
                && a.getUsername().equals("user123")
                && a.getPassword().equals("senha123")));
    }

    @Test
    void provisionTrunk_credential_shouldCreateRegistration() {
        provisioningService.provisionTrunk(trunkCredential);

        verify(psRegistrationRepository).save(argThat(r ->
                r.getId().equals("opasuite")
                && r.getServerUri().equals("sip:sip.opasuite.com.br")
                && r.getClientUri().equals("sip:user123@sip.opasuite.com.br")));
    }

    @Test
    void provisionTrunk_credential_shouldNotCreatePsIdentify() {
        provisioningService.provisionTrunk(trunkCredential);

        verify(psEndpointIdIpRepository, never()).save(any());
    }

    // ── IP_AUTH: cria ps_identify, sem ps_auths / ps_registrations ────────────

    @Test
    void provisionTrunk_ipAuth_shouldCreatePsIdentifyWithCorrectMatch() {
        provisioningService.provisionTrunk(trunkIpAuth);

        verify(psEndpointIdIpRepository).save(argThat(i ->
                i.getId().equals("tellcheap")
                && i.getEndpoint().equals("tellcheap")
                && i.getMatch().equals("sip.tellcheap.com.br")));
    }

    @Test
    void provisionTrunk_ipAuth_shouldNotCreateAuth() {
        provisioningService.provisionTrunk(trunkIpAuth);

        verify(authRepository, never()).save(any());
    }

    @Test
    void provisionTrunk_ipAuth_shouldNotCreateRegistration() {
        provisioningService.provisionTrunk(trunkIpAuth);

        verify(psRegistrationRepository, never()).save(any());
    }

    @Test
    void provisionTrunk_ipAuth_shouldCreateAorWithStaticContact() {
        provisioningService.provisionTrunk(trunkIpAuth);

        verify(aorRepository).save(argThat(a ->
                a.getId().equals("tellcheap")
                && a.getContact().equals("sip:sip.tellcheap.com.br:5060")));
    }

    @Test
    void provisionTrunk_ipAuth_shouldSetEndpointWithoutAuthFields() {
        provisioningService.provisionTrunk(trunkIpAuth);

        verify(endpointRepository).save(argThat(e ->
                e.getId().equals("tellcheap")
                && e.getAuth() == null
                && e.getOutboundAuth() == null
                && e.getContext().equals("pstn-tellcheap")));
    }

    @Test
    void provisionTrunk_ipAuth_shouldCreateOutboundExtensions() {
        provisioningService.provisionTrunk(trunkIpAuth);

        verify(extensionRepository).save(argThat(e ->
                e.getContext().equals("internal-tellcheap")
                && e.getPriority() == 2
                && e.getApp().equals("Dial")));
    }

    // ── deprovisionTrunk: IP_AUTH não toca em auth/registration ───────────────

    @Test
    void deprovisionTrunk_ipAuth_shouldDeletePsIdentify() {
        provisioningService.deprovisionTrunk(trunkIpAuth);

        verify(psEndpointIdIpRepository).findById("tellcheap");
    }

    @Test
    void deprovisionTrunk_ipAuth_shouldNotTouchAuthOrRegistration() {
        provisioningService.deprovisionTrunk(trunkIpAuth);

        verify(authRepository, never()).findById(any());
        verify(psRegistrationRepository, never()).findById(any());
    }

    @Test
    void deprovisionTrunk_credential_shouldDeleteOutboundExtensions() {
        provisioningService.deprovisionTrunk(trunkCredential);

        verify(extensionRepository).deleteByExtenAndContext("_X.", "internal-opasuite");
    }

    // ── reprovisionTrunk ──────────────────────────────────────────────────────

    @Test
    void reprovisionTrunk_credential_shouldDeleteAndRecreateOutboundExtensions() {
        provisioningService.reprovisionTrunk(trunkCredential);

        verify(extensionRepository).deleteByExtenAndContext("_X.", "internal-opasuite");
        verify(extensionRepository).save(argThat(e -> e.getPriority() == 2 && e.getApp().equals("Dial")));
    }

    @Test
    void reprovisionTrunk_ipAuth_shouldNotTouchAuthOrRegistration() {
        provisioningService.reprovisionTrunk(trunkIpAuth);

        verify(authRepository, never()).findById(any());
        verify(psRegistrationRepository, never()).findById(any());
    }

    @Test
    void reprovisionTrunk_ipAuth_shouldUpdatePsIdentifyMatch() {
        PsEndpointIdIp existing = new PsEndpointIdIp();
        existing.setId("tellcheap");
        existing.setEndpoint("tellcheap");
        existing.setMatch("old.host.com");

        when(psEndpointIdIpRepository.findById("tellcheap"))
                .thenReturn(java.util.Optional.of(existing));

        trunkIpAuth.setIdentifyMatch("new.host.com");
        trunkIpAuth.setHost("new.host.com");

        provisioningService.reprovisionTrunk(trunkIpAuth);

        verify(psEndpointIdIpRepository).save(argThat(i -> i.getMatch().equals("new.host.com")));
    }
}
