package com.dionialves.AsteraComm.trunk;

import com.dionialves.AsteraComm.asterisk.aors.AorRepository;
import com.dionialves.AsteraComm.asterisk.auth.AuthRepository;
import com.dionialves.AsteraComm.asterisk.endpoint.EndpointRepository;
import com.dionialves.AsteraComm.asterisk.endpoint.EndpointStatusRepository;
import com.dionialves.AsteraComm.asterisk.extension.ExtensionRepository;
import com.dionialves.AsteraComm.asterisk.dialplan.DialplanGeneratorService;
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
import static org.mockito.Mockito.verify;

/**
 * Testa o provisionamento de troncos conforme US-009:
 * - ps_endpoints.context = "pstn-<nome>"
 * - Extensions de saída criadas em "internal-<nome>"
 * - Dial com prefixo opcional
 */
@ExtendWith(MockitoExtension.class)
class TrunkProvisioningContextTest {

    @Mock private AorRepository aorRepository;
    @Mock private AuthRepository authRepository;
    @Mock private EndpointRepository endpointRepository;
    @Mock private ExtensionRepository extensionRepository;
    @Mock private EndpointStatusRepository endpointStatusRepository;
    @Mock private PsRegistrationRepository psRegistrationRepository;
    @Mock private AmiService amiService;
    @Mock private DialplanGeneratorService dialplanGeneratorService;

    @InjectMocks
    private AsteriskProvisioningService provisioningService;

    private Trunk trunkSemPrefix;
    private Trunk trunkComPrefix;

    @BeforeEach
    void setUp() {
        trunkSemPrefix = new Trunk();
        trunkSemPrefix.setName("opasuite");
        trunkSemPrefix.setHost("sip.opasuite.com.br");
        trunkSemPrefix.setUsername("user123");
        trunkSemPrefix.setPassword("senha123");
        trunkSemPrefix.setPrefix(null);

        trunkComPrefix = new Trunk();
        trunkComPrefix.setName("tellcheap");
        trunkComPrefix.setHost("sip.tellcheap.com");
        trunkComPrefix.setUsername("user456");
        trunkComPrefix.setPassword("senha456");
        trunkComPrefix.setPrefix("8712");
    }

    // === Contexto do ps_endpoint do tronco ===

    @Test
    void provisionTrunk_shouldSetEndpointContextToPstnPrefix() {
        provisioningService.provisionTrunk(trunkSemPrefix);

        verify(endpointRepository).save(argThat(e ->
                e.getId().equals("opasuite")
                && e.getContext().equals("pstn-opasuite")));
    }

    // === Extensions de saída (rota auto-criada) — sem prefix ===

    @Test
    void provisionTrunk_shouldCreateNoOpExtensionInInternalContext() {
        provisioningService.provisionTrunk(trunkSemPrefix);

        verify(extensionRepository).save(argThat(e ->
                e.getContext().equals("internal-opasuite")
                && e.getExten().equals("_X.")
                && e.getPriority() == 1
                && e.getApp().equals("NoOp")));
    }

    @Test
    void provisionTrunk_shouldCreateDialExtensionWithoutPrefix() {
        provisioningService.provisionTrunk(trunkSemPrefix);

        verify(extensionRepository).save(argThat(e ->
                e.getContext().equals("internal-opasuite")
                && e.getExten().equals("_X.")
                && e.getPriority() == 2
                && e.getApp().equals("Dial")
                && e.getAppdata().equals("PJSIP/${EXTEN}@opasuite,60")));
    }

    @Test
    void provisionTrunk_shouldCreateHangupExtension() {
        provisioningService.provisionTrunk(trunkSemPrefix);

        verify(extensionRepository).save(argThat(e ->
                e.getContext().equals("internal-opasuite")
                && e.getExten().equals("_X.")
                && e.getPriority() == 3
                && e.getApp().equals("Hangup")));
    }

    // === Extensions de saída com prefix ===

    @Test
    void provisionTrunk_shouldCreateDialExtensionWithPrefix() {
        provisioningService.provisionTrunk(trunkComPrefix);

        verify(extensionRepository).save(argThat(e ->
                e.getContext().equals("internal-tellcheap")
                && e.getPriority() == 2
                && e.getApp().equals("Dial")
                && e.getAppdata().equals("PJSIP/8712${EXTEN}@tellcheap,60")));
    }

    // === Reprovisionamento ===

    @Test
    void reprovisionTrunk_shouldDeleteAndRecreatOutboundExtensions() {
        provisioningService.reprovisionTrunk(trunkSemPrefix);

        verify(extensionRepository).deleteByExtenAndContext("_X.", "internal-opasuite");
        verify(extensionRepository).save(argThat(e -> e.getPriority() == 2 && e.getApp().equals("Dial")));
    }

    // === Deprovisionamento ===

    @Test
    void deprovisionTrunk_shouldDeleteOutboundExtensions() {
        provisioningService.deprovisionTrunk(trunkSemPrefix);

        verify(extensionRepository).deleteByExtenAndContext("_X.", "internal-opasuite");
    }
}
