package com.dionialves.AsteraComm.circuit;

import com.dionialves.AsteraComm.asterisk.aors.AorRepository;
import com.dionialves.AsteraComm.asterisk.aors.Aors;
import com.dionialves.AsteraComm.asterisk.auth.Auth;
import com.dionialves.AsteraComm.asterisk.auth.AuthRepository;
import com.dionialves.AsteraComm.asterisk.endpoint.Endpoint;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Testa o provisionamento de circuitos:
 * - ps_endpoints.context = "internal-<tronco>"
 * - Nenhuma extension criada no provision (DIDs controlam extensions — US-007)
 * - Troca de tronco no reprovision atualiza contexto do endpoint
 */
@ExtendWith(MockitoExtension.class)
class CircuitProvisioningContextTest {

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

    private Circuit circuit;

    @BeforeEach
    void setUp() {
        circuit = new Circuit();
        circuit.setNumber("100000");
        circuit.setPassword("secret");
        circuit.setTrunkName("opasuite");
    }

    // === Contexto do ps_endpoint do circuito ===

    @Test
    void provision_shouldSetEndpointContextToInternalTrunk() {
        provisioningService.provision(circuit);

        verify(endpointRepository).save(argThat(e ->
                e.getId().equals("100000")
                && e.getContext().equals("internal-opasuite")));
    }

    @Test
    void provision_shouldNotCreateAnyExtensions() {
        provisioningService.provision(circuit);

        verifyNoInteractions(extensionRepository);
    }

    // === Reprovisionamento — troca de tronco ===

    @Test
    void reprovision_shouldUpdateEndpointContextWhenTrunkChanges() {
        Circuit updated = new Circuit();
        updated.setNumber("100000");
        updated.setPassword("secret");
        updated.setTrunkName("tellcheap");

        Endpoint endpoint = new Endpoint();
        endpoint.setId("100000");
        when(authRepository.findById("100000")).thenReturn(Optional.of(new Auth()));
        when(endpointRepository.findById("100000")).thenReturn(Optional.of(endpoint));

        provisioningService.reprovision(updated, "opasuite");

        verify(endpointRepository).save(argThat(e ->
                e.getId().equals("100000")
                && e.getContext().equals("internal-tellcheap")));
    }

    @Test
    void reprovision_shouldNotMoveExtensionsWhenTrunkUnchanged() {
        when(authRepository.findById("100000")).thenReturn(Optional.of(new Auth()));

        provisioningService.reprovision(circuit, "opasuite");

        verify(extensionRepository, never()).deleteByExtenAndContext(any(), any());
        verifyNoInteractions(endpointRepository);
    }

    // === Deprovisionamento ===

    @Test
    void deprovision_shouldNotTouchExtensionRepository() {
        Endpoint endpoint = new Endpoint();
        endpoint.setId("100000");
        endpoint.setAors(new Aors());
        endpoint.setAuth(new Auth());
        when(endpointRepository.findById("100000")).thenReturn(Optional.of(endpoint));

        provisioningService.deprovision(circuit);

        verifyNoInteractions(extensionRepository);
    }
}
