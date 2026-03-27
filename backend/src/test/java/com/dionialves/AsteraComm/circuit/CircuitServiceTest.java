package com.dionialves.AsteraComm.circuit;

import com.dionialves.AsteraComm.asterisk.provisioning.AsteriskProvisioningService;
import com.dionialves.AsteraComm.call.CallRepository;
import com.dionialves.AsteraComm.circuit.dto.CircuitCreateDTO;
import com.dionialves.AsteraComm.circuit.dto.CircuitSummaryDTO;
import com.dionialves.AsteraComm.customer.Customer;
import com.dionialves.AsteraComm.customer.CustomerRepository;
import com.dionialves.AsteraComm.did.DIDRepository;
import com.dionialves.AsteraComm.exception.BusinessException;
import com.dionialves.AsteraComm.exception.NotFoundException;
import com.dionialves.AsteraComm.plan.Plan;
import com.dionialves.AsteraComm.plan.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CircuitServiceTest {

    @Mock
    private CircuitRepository circuitRepository;

    @Mock
    private DIDRepository didRepository;

    @Mock
    private CallRepository callRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private AsteriskProvisioningService asteriskProvisioningService;

    @InjectMocks
    private CircuitService circuitService;

    private Circuit testCircuit;
    private Customer testCustomer;
    private Plan testPlan;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setName("Acme Corp");
        testCustomer.setEnabled(true);

        testPlan = new Plan();
        testPlan.setId(10L);
        testPlan.setName("Plano Basic");

        testCircuit = new Circuit();
        testCircuit.setId(1L);
        testCircuit.setNumber("100000");
        testCircuit.setPassword("secret");
        testCircuit.setTrunkName("opasuite");
        testCircuit.setCustomer(testCustomer);
        testCircuit.setActive(true);
    }

    @Test
    void getAll_shouldDelegateToRepository() {
        Page<CircuitProjection> page = new PageImpl<>(List.of());
        when(circuitRepository.findAllCircuits(anyString(), isNull(), isNull(), any(Pageable.class))).thenReturn(page);

        Page<CircuitProjection> result = circuitService.getAll("", null, null, PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        verify(circuitRepository).findAllCircuits("", null, null, PageRequest.of(0, 10));
    }

    @Test
    void findByNumber_shouldReturnCircuit_whenExists() {
        when(circuitRepository.findByNumber("100000")).thenReturn(Optional.of(testCircuit));

        Optional<Circuit> result = circuitService.findByNumber("100000");

        assertThat(result).isPresent();
        assertThat(result.get().getNumber()).isEqualTo("100000");
    }

    @Test
    void findByNumber_shouldReturnEmpty_whenNotExists() {
        when(circuitRepository.findByNumber("999999")).thenReturn(Optional.empty());

        Optional<Circuit> result = circuitService.findByNumber("999999");

        assertThat(result).isEmpty();
    }

    @Test
    void create_shouldGenerateCodeStartingAt100000_whenNoCircuitsExist() {
        CircuitCreateDTO dto = new CircuitCreateDTO("mypassword", "opasuite", 1L, 10L, null);
        when(circuitRepository.findMaxCode()).thenReturn(Optional.empty());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(planRepository.findById(10L)).thenReturn(Optional.of(testPlan));
        when(circuitRepository.save(any(Circuit.class))).thenReturn(testCircuit);

        circuitService.create(dto);

        verify(circuitRepository).save(argThat(c -> c.getNumber().equals("100000")
                && c.getPassword().equals("mypassword")
                && c.getTrunkName().equals("opasuite")
                && c.getCustomer().getId().equals(1L)));
        verify(asteriskProvisioningService).provision(any(Circuit.class));
    }

    @Test
    void create_shouldIncrementCode_whenCircuitsExist() {
        CircuitCreateDTO dto = new CircuitCreateDTO("mypassword", "opasuite", 1L, 10L, null);
        when(circuitRepository.findMaxCode()).thenReturn(Optional.of("100004"));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(planRepository.findById(10L)).thenReturn(Optional.of(testPlan));
        when(circuitRepository.save(any(Circuit.class))).thenReturn(testCircuit);

        circuitService.create(dto);

        verify(circuitRepository).save(argThat(c -> c.getNumber().equals("100005")));
    }

    @Test
    void create_shouldThrowNotFoundException_whenCustomerNotExists() {
        CircuitCreateDTO dto = new CircuitCreateDTO("mypassword", "opasuite", 99L, 10L, null);
        when(circuitRepository.findMaxCode()).thenReturn(Optional.empty());
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> circuitService.create(dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cliente não encontrado");

        verify(circuitRepository, never()).save(any());
    }

    @Test
    void create_shouldSetActiveTrueByDefault_whenActiveIsNull() {
        CircuitCreateDTO dto = new CircuitCreateDTO("mypassword", "opasuite", 1L, 10L, null);
        when(circuitRepository.findMaxCode()).thenReturn(Optional.empty());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(planRepository.findById(10L)).thenReturn(Optional.of(testPlan));
        when(circuitRepository.save(any(Circuit.class))).thenReturn(testCircuit);

        circuitService.create(dto);

        verify(circuitRepository).save(argThat(Circuit::isActive));
    }

    @Test
    void create_shouldSetActiveTrue_whenActiveTrueInDTO() {
        CircuitCreateDTO dto = new CircuitCreateDTO("mypassword", "opasuite", 1L, 10L, true);
        when(circuitRepository.findMaxCode()).thenReturn(Optional.empty());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(planRepository.findById(10L)).thenReturn(Optional.of(testPlan));
        when(circuitRepository.save(any(Circuit.class))).thenReturn(testCircuit);

        circuitService.create(dto);

        verify(circuitRepository).save(argThat(Circuit::isActive));
    }

    @Test
    void update_shouldUpdatePasswordAndCallReprovision() {
        CircuitCreateDTO dto = new CircuitCreateDTO("newpassword", "opasuite", 1L, 10L, null);
        when(circuitRepository.findByNumber("100000")).thenReturn(Optional.of(testCircuit));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(planRepository.findById(10L)).thenReturn(Optional.of(testPlan));
        when(circuitRepository.save(any(Circuit.class))).thenReturn(testCircuit);

        circuitService.update("100000", dto);

        verify(circuitRepository).save(argThat(c -> c.getPassword().equals("newpassword")));
        verify(asteriskProvisioningService).reprovision(any(Circuit.class), eq("opasuite"));
    }

    @Test
    void update_shouldThrowNotFoundException_whenCircuitNotExists() {
        CircuitCreateDTO dto = new CircuitCreateDTO("password", "opasuite", 1L, 10L, null);
        when(circuitRepository.findByNumber("999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> circuitService.update("999999", dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Circuito não encontrado");
    }

    @Test
    void update_shouldThrowNotFoundException_whenCustomerNotExists() {
        CircuitCreateDTO dto = new CircuitCreateDTO("password", "opasuite", 99L, 10L, null);
        when(circuitRepository.findByNumber("100000")).thenReturn(Optional.of(testCircuit));
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> circuitService.update("100000", dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cliente não encontrado");
    }

    @Test
    void update_shouldPersistActiveFalse_whenDtoHasActiveFalse() {
        CircuitCreateDTO dto = new CircuitCreateDTO("secret", "opasuite", 1L, 10L, false);
        when(circuitRepository.findByNumber("100000")).thenReturn(Optional.of(testCircuit));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(planRepository.findById(10L)).thenReturn(Optional.of(testPlan));
        when(circuitRepository.save(any(Circuit.class))).thenReturn(testCircuit);

        circuitService.update("100000", dto);

        verify(circuitRepository).save(argThat(c -> !c.isActive()));
    }

    @Test
    void update_shouldKeepActiveTrueWhenDtoHasActiveNull() {
        testCircuit.setActive(true);
        CircuitCreateDTO dto = new CircuitCreateDTO("secret", "opasuite", 1L, 10L, null);
        when(circuitRepository.findByNumber("100000")).thenReturn(Optional.of(testCircuit));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(planRepository.findById(10L)).thenReturn(Optional.of(testPlan));
        when(circuitRepository.save(any(Circuit.class))).thenReturn(testCircuit);

        circuitService.update("100000", dto);

        verify(circuitRepository).save(argThat(Circuit::isActive));
    }

    // === Novos testes US-018 — vínculo com plano ===

    @Test
    void create_shouldSetPlan_whenPlanIdProvided() {
        CircuitCreateDTO dto = new CircuitCreateDTO("mypassword", "opasuite", 1L, 10L, null);
        when(circuitRepository.findMaxCode()).thenReturn(Optional.empty());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(planRepository.findById(10L)).thenReturn(Optional.of(testPlan));
        when(circuitRepository.save(any(Circuit.class))).thenReturn(testCircuit);

        circuitService.create(dto);

        verify(circuitRepository).save(argThat(c -> testPlan.equals(c.getPlan())));
    }

    @Test
    void create_shouldThrowBusinessException_whenPlanIdIsNull() {
        CircuitCreateDTO dto = new CircuitCreateDTO("mypassword", "opasuite", 1L, null, null);
        when(circuitRepository.findMaxCode()).thenReturn(Optional.empty());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        assertThatThrownBy(() -> circuitService.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Plano é obrigatório");

        verify(circuitRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowNotFoundException_whenPlanNotFound() {
        CircuitCreateDTO dto = new CircuitCreateDTO("mypassword", "opasuite", 1L, 99L, null);
        when(circuitRepository.findMaxCode()).thenReturn(Optional.empty());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> circuitService.create(dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Plano não encontrado");

        verify(circuitRepository, never()).save(any());
    }

    @Test
    void update_shouldChangePlan_whenNewPlanIdProvided() {
        CircuitCreateDTO dto = new CircuitCreateDTO("secret", "opasuite", 1L, 10L, null);
        when(circuitRepository.findByNumber("100000")).thenReturn(Optional.of(testCircuit));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(planRepository.findById(10L)).thenReturn(Optional.of(testPlan));
        when(circuitRepository.save(any(Circuit.class))).thenReturn(testCircuit);

        circuitService.update("100000", dto);

        verify(circuitRepository).save(argThat(c -> testPlan.equals(c.getPlan())));
    }

    @Test
    void update_shouldThrowBusinessException_whenPlanIdIsNull() {
        testCircuit.setPlan(testPlan);
        CircuitCreateDTO dto = new CircuitCreateDTO("secret", "opasuite", 1L, null, null);
        when(circuitRepository.findByNumber("100000")).thenReturn(Optional.of(testCircuit));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        assertThatThrownBy(() -> circuitService.update("100000", dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Plano é obrigatório");

        verify(circuitRepository, never()).save(any());
    }

    // === delete ===

    @Test
    void delete_shouldCallDeprovisionAndDeleteCircuit_whenNoLinks() {
        when(circuitRepository.findByNumber("100000")).thenReturn(Optional.of(testCircuit));
        when(didRepository.existsByCircuit(testCircuit)).thenReturn(false);
        when(callRepository.existsByCircuitNumber("100000")).thenReturn(false);

        Optional<Circuit> result = circuitService.delete("100000");

        assertThat(result).isEmpty();
        verify(asteriskProvisioningService).deprovision(testCircuit);
        verify(circuitRepository).delete(testCircuit);
    }

    @Test
    void delete_shouldThrowBusinessException_whenCircuitHasLinkedDids() {
        when(circuitRepository.findByNumber("100000")).thenReturn(Optional.of(testCircuit));
        when(didRepository.existsByCircuit(testCircuit)).thenReturn(true);

        assertThatThrownBy(() -> circuitService.delete("100000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DID");

        verifyNoInteractions(asteriskProvisioningService);
        verify(circuitRepository, never()).delete(any());
    }

    @Test
    void delete_whenCallsExist_shouldDeactivateAndReturnCircuit() {
        when(circuitRepository.findByNumber("100000")).thenReturn(Optional.of(testCircuit));
        when(didRepository.existsByCircuit(testCircuit)).thenReturn(false);
        when(callRepository.existsByCircuitNumber("100000")).thenReturn(true);
        when(circuitRepository.save(testCircuit)).thenReturn(testCircuit);

        Optional<Circuit> result = circuitService.delete("100000");

        assertThat(result).isPresent();
        assertThat(result.get().isActive()).isFalse();
        verify(circuitRepository).save(argThat(c -> !c.isActive()));
        verifyNoInteractions(asteriskProvisioningService);
        verify(circuitRepository, never()).delete(any());
    }

    @Test
    void getAll_withOnlineTrue_shouldPassFilterToRepository() {
        Page<CircuitProjection> page = new PageImpl<>(List.of());
        when(circuitRepository.findAllCircuits(anyString(), eq(true), isNull(), any(Pageable.class))).thenReturn(page);

        circuitService.getAll("", true, null, PageRequest.of(0, 10));

        verify(circuitRepository).findAllCircuits("", true, null, PageRequest.of(0, 10));
    }

    @Test
    void getAll_withActiveFalse_shouldPassFilterToRepository() {
        Page<CircuitProjection> page = new PageImpl<>(List.of());
        when(circuitRepository.findAllCircuits(anyString(), isNull(), eq(false), any(Pageable.class))).thenReturn(page);

        circuitService.getAll("", null, false, PageRequest.of(0, 10));

        verify(circuitRepository).findAllCircuits("", null, false, PageRequest.of(0, 10));
    }

    @Test
    void getAll_withOnlineFalseAndActiveTrue_shouldPassBothFiltersToRepository() {
        Page<CircuitProjection> page = new PageImpl<>(List.of());
        when(circuitRepository.findAllCircuits(anyString(), eq(false), eq(true), any(Pageable.class))).thenReturn(page);

        circuitService.getAll("", false, true, PageRequest.of(0, 10));

        verify(circuitRepository).findAllCircuits("", false, true, PageRequest.of(0, 10));
    }

    @Test
    void delete_shouldThrowNotFoundException_whenNotExists() {
        when(circuitRepository.findByNumber("999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> circuitService.delete("999999"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Circuito não encontrado");
    }

    // --- findAllSummary ---

    @Test
    void findAllSummary_shouldDelegateToRepository() {
        var summaries = List.of(new CircuitSummaryDTO("100001", "Empresa Alpha"));
        when(circuitRepository.findAllSummary()).thenReturn(summaries);

        List<CircuitSummaryDTO> result = circuitService.findAllSummary();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).number()).isEqualTo("100001");
        assertThat(result.get(0).customerName()).isEqualTo("Empresa Alpha");
        verify(circuitRepository).findAllSummary();
    }

    @Test
    void findAllSummary_shouldReturnEmptyList_whenNoCircuits() {
        when(circuitRepository.findAllSummary()).thenReturn(List.of());

        List<CircuitSummaryDTO> result = circuitService.findAllSummary();

        assertThat(result).isEmpty();
    }
}
