package com.dionialves.AsteraComm.call;

import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.cdr.CdrRecord;
import com.dionialves.AsteraComm.cdr.CdrRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallCircuitReconciliationServiceTest {

    @Mock
    private CallRepository callRepository;
    @Mock
    private CdrRepository cdrRepository;
    @Mock
    private CircuitRepository circuitRepository;
    private ChannelParser channelParser;
    private CallCircuitReconciliationService service;

    @BeforeEach
    void setUp() {
        channelParser = new ChannelParser();
        service = new CallCircuitReconciliationService(callRepository, cdrRepository, circuitRepository, channelParser);
    }

    @Test
    void findOrphanCalls_returnsResolvable_whenChannelMatchesCircuit() {
        Call orphan = new Call();
        orphan.setId(1L);
        orphan.setUniqueId("abc123");
        orphan.setCallDate(LocalDateTime.now());
        orphan.setDst("11999998888");

        CdrRecord cdr = new CdrRecord();
        cdr.setUniqueId("abc123");
        cdr.setChannel("PJSIP/4933401714-000045f0");

        Circuit circuit = new Circuit();
        circuit.setNumber("4933401714");

        when(callRepository.findByCircuitIsNull()).thenReturn(List.of(orphan));
        when(cdrRepository.findByUniqueId("abc123")).thenReturn(Optional.of(cdr));
        when(circuitRepository.findByNumber("4933401714")).thenReturn(Optional.of(circuit));

        List<OrphanCallDTO> result = service.findOrphanCalls();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).resolvable()).isTrue();
        assertThat(result.get(0).circuitCode()).isEqualTo("4933401714");
    }

    @Test
    void findOrphanCalls_returnsNotResolvable_whenChannelEmpty() {
        Call orphan = new Call();
        orphan.setId(1L);
        orphan.setUniqueId("abc123");
        orphan.setCallDate(LocalDateTime.now());
        orphan.setDst("11999998888");

        CdrRecord cdr = new CdrRecord();
        cdr.setUniqueId("abc123");
        cdr.setChannel("PJSIP"); // sem código após barra

        when(callRepository.findByCircuitIsNull()).thenReturn(List.of(orphan));
        when(cdrRepository.findByUniqueId("abc123")).thenReturn(Optional.of(cdr));

        List<OrphanCallDTO> result = service.findOrphanCalls();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).resolvable()).isFalse();
        assertThat(result.get(0).reasonOrDst()).isEqualTo("Canal vazio");
    }

    @Test
    void findOrphanCalls_returnsNotResolvable_whenCircuitMissing() {
        Call orphan = new Call();
        orphan.setId(1L);
        orphan.setUniqueId("abc123");
        orphan.setCallDate(LocalDateTime.now());
        orphan.setDst("11999998888");

        CdrRecord cdr = new CdrRecord();
        cdr.setUniqueId("abc123");
        cdr.setChannel("PJSIP/9999999999-000045f0");

        when(callRepository.findByCircuitIsNull()).thenReturn(List.of(orphan));
        when(cdrRepository.findByUniqueId("abc123")).thenReturn(Optional.of(cdr));
        when(circuitRepository.findByNumber("9999999999")).thenReturn(Optional.empty());

        List<OrphanCallDTO> result = service.findOrphanCalls();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).resolvable()).isFalse();
        assertThat(result.get(0).circuitCode()).isEqualTo("9999999999");
    }

    @Test
    void reconcile_linksOnlyResolvableCalls() {
        Call resolvableCall = new Call();
        resolvableCall.setId(1L);
        resolvableCall.setUniqueId("resolvable");
        resolvableCall.setCallDate(LocalDateTime.now());
        resolvableCall.setDst("11999998888");

        Call nonResolvableCall = new Call();
        nonResolvableCall.setId(2L);
        nonResolvableCall.setUniqueId("not-resolvable");
        nonResolvableCall.setCallDate(LocalDateTime.now());
        nonResolvableCall.setDst("21888887777");

        CdrRecord cdrResolvable = new CdrRecord();
        cdrResolvable.setUniqueId("resolvable");
        cdrResolvable.setChannel("PJSIP/4933401714-000045f0");

        CdrRecord cdrNonResolvable = new CdrRecord();
        cdrNonResolvable.setUniqueId("not-resolvable");
        cdrNonResolvable.setChannel("PJSIP"); // canal vazio

        Circuit circuit = new Circuit();
        circuit.setNumber("4933401714");

        when(callRepository.findByCircuitIsNull()).thenReturn(List.of(resolvableCall, nonResolvableCall));
        when(cdrRepository.findByUniqueId("resolvable")).thenReturn(Optional.of(cdrResolvable));
        when(cdrRepository.findByUniqueId("not-resolvable")).thenReturn(Optional.of(cdrNonResolvable));
        when(circuitRepository.findByNumber("4933401714")).thenReturn(Optional.of(circuit));
        when(callRepository.findById(1L)).thenReturn(Optional.of(resolvableCall));

        ReconciliationResultDTO result = service.reconcile();

        assertThat(result.linked()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        verify(callRepository, times(1)).save(any(Call.class));
    }

    @Test
    void reconcile_preservesCostAndStatus() {
        Call orphan = new Call();
        orphan.setId(1L);
        orphan.setUniqueId("abc123");
        orphan.setCallDate(LocalDateTime.now());
        orphan.setDst("11999998888");
        orphan.setCost(java.math.BigDecimal.valueOf(10.50));
        orphan.setMinutesFromQuota(java.math.BigDecimal.valueOf(5));
        orphan.setCallStatus(CallStatus.PROCESSED);

        CdrRecord cdr = new CdrRecord();
        cdr.setUniqueId("abc123");
        cdr.setChannel("PJSIP/4933401714-000045f0");

        Circuit circuit = new Circuit();
        circuit.setNumber("4933401714");

        when(callRepository.findByCircuitIsNull()).thenReturn(List.of(orphan));
        when(cdrRepository.findByUniqueId("abc123")).thenReturn(Optional.of(cdr));
        when(circuitRepository.findByNumber("4933401714")).thenReturn(Optional.of(circuit));
        when(callRepository.findById(1L)).thenReturn(Optional.of(orphan));

        service.reconcile();

        verify(callRepository).save(argThat(call -> {
            assertThat(call.getCircuit()).isEqualTo(circuit);
            assertThat(call.getCost()).isEqualByComparingTo(java.math.BigDecimal.valueOf(10.50));
            assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(java.math.BigDecimal.valueOf(5));
            assertThat(call.getCallStatus()).isEqualTo(CallStatus.PROCESSED);
            return true;
        }));
    }
}