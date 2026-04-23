package com.dionialves.AsteraComm.report.callhistory;

import com.dionialves.AsteraComm.call.Call;
import com.dionialves.AsteraComm.call.CallDirection;
import com.dionialves.AsteraComm.call.CallRepository;
import com.dionialves.AsteraComm.call.CallStatus;
import com.dionialves.AsteraComm.call.CallType;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallHistoryServiceTest {

    @Mock
    private CallRepository callRepository;

    @Mock
    private CircuitRepository circuitRepository;

    @InjectMocks
    private CallHistoryService callHistoryService;

    private static final String CIRCUIT_NUMBER = "1001";
    private static final int    MONTH          = 3;
    private static final int    YEAR           = 2026;

    private Circuit circuit;

    @BeforeEach
    void setUp() {
        circuit = new Circuit();
        circuit.setNumber(CIRCUIT_NUMBER);
    }

    private Call buildCall(String uniqueId, LocalDateTime callDate, int billSeconds,
                           CallType callType, CallDirection direction) {
        Call call = new Call();
        call.setUniqueId(uniqueId);
        call.setCallDate(callDate);
        call.setCallerNumber("11933334444");
        call.setDst("1133334444");
        call.setDurationSeconds(billSeconds);
        call.setBillSeconds(billSeconds);
        call.setDisposition("ANSWERED");
        call.setCallType(callType);
        call.setCallStatus(CallStatus.PROCESSED);
        call.setCircuit(circuit);
        call.setDirection(direction);
        return call;
    }

    // -------------------------------------------------------------------------
    // getHistory_throwsNotFoundException_whenCircuitNotFound
    // -------------------------------------------------------------------------

    @Test
    void getHistory_throwsNotFoundException_whenCircuitNotFound() {
        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> callHistoryService.getHistory(CIRCUIT_NUMBER, MONTH, YEAR))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(CIRCUIT_NUMBER);
    }

    // -------------------------------------------------------------------------
    // getHistory_returnsEmpty_whenNoCalls
    // -------------------------------------------------------------------------

    @Test
    void getHistory_returnsEmpty_whenNoCalls() {
        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(Collections.emptyList());

        CallHistoryResultDTO result = callHistoryService.getHistory(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.circuitNumber()).isEqualTo(CIRCUIT_NUMBER);
        assertThat(result.month()).isEqualTo(MONTH);
        assertThat(result.year()).isEqualTo(YEAR);
        assertThat(result.totalCalls()).isZero();
        assertThat(result.totalMinutes()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.lines()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // getHistory_returnsCorrectLinesForMixedDirections
    // -------------------------------------------------------------------------

    @Test
    void getHistory_returnsCorrectLinesForMixedDirections() {
        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));

        List<Call> calls = List.of(
                buildCall("call-1", LocalDateTime.of(2026, 3, 1, 10, 0), 60,
                        CallType.FIXED_LOCAL, CallDirection.OUTBOUND),
                buildCall("call-2", LocalDateTime.of(2026, 3, 2, 11, 0), 90,
                        CallType.MOBILE_LOCAL, CallDirection.INBOUND),
                buildCall("call-3", LocalDateTime.of(2026, 3, 3, 12, 0), 45,
                        CallType.FIXED_LONG_DISTANCE, CallDirection.OUTBOUND)
        );
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(calls);

        CallHistoryResultDTO result = callHistoryService.getHistory(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.lines()).hasSize(3);
        assertThat(result.lines().get(0).uniqueId()).isEqualTo("call-1");
        assertThat(result.lines().get(0).direction()).isEqualTo(CallDirection.OUTBOUND);
        assertThat(result.lines().get(1).uniqueId()).isEqualTo("call-2");
        assertThat(result.lines().get(1).direction()).isEqualTo(CallDirection.INBOUND);
        assertThat(result.lines().get(2).uniqueId()).isEqualTo("call-3");
        assertThat(result.lines().get(2).direction()).isEqualTo(CallDirection.OUTBOUND);
    }

    // -------------------------------------------------------------------------
    // getHistory_calculatesTotalMinutesCorrectly
    // -------------------------------------------------------------------------

    @Test
    void getHistory_calculatesTotalMinutesCorrectly() {
        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));

        // billSeconds=65 → ceil(65/30) = 3 → 3/2 = 1.5 minutos por chamada
        // 3 chamadas → totalBillSec = 195 → ceil(195/30) = 7 → 7/2 = 3.5 minutos
        List<Call> calls = List.of(
                buildCall("call-1", LocalDateTime.of(2026, 3, 1, 10, 0), 65,
                        CallType.FIXED_LOCAL, CallDirection.OUTBOUND),
                buildCall("call-2", LocalDateTime.of(2026, 3, 2, 11, 0), 65,
                        CallType.FIXED_LOCAL, CallDirection.OUTBOUND),
                buildCall("call-3", LocalDateTime.of(2026, 3, 3, 12, 0), 65,
                        CallType.FIXED_LOCAL, CallDirection.OUTBOUND)
        );
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(calls);

        CallHistoryResultDTO result = callHistoryService.getHistory(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.totalCalls()).isEqualTo(3);
        // 195 bill seconds total → ceil(195/30) = 7 → 7/2 = 3.5
        assertThat(result.totalMinutes()).isEqualByComparingTo(new BigDecimal("3.5"));
    }

    // -------------------------------------------------------------------------
    // translateDisposition_returnsPortugueseLabels
    // -------------------------------------------------------------------------

    @Test
    void translateDisposition_returnsPortugueseLabels() {
        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));

        List<Call> calls = List.of(
                buildCall("call-answered",  LocalDateTime.of(2026, 3, 1, 10, 0), 60, CallType.FIXED_LOCAL, CallDirection.OUTBOUND),
                buildCall("call-noanswer", LocalDateTime.of(2026, 3, 2, 11, 0), 60, CallType.FIXED_LOCAL, CallDirection.OUTBOUND),
                buildCall("call-busy",     LocalDateTime.of(2026, 3, 3, 12, 0), 60, CallType.FIXED_LOCAL, CallDirection.OUTBOUND),
                buildCall("call-failed",   LocalDateTime.of(2026, 3, 4, 13, 0), 60, CallType.FIXED_LOCAL, CallDirection.OUTBOUND)
        );
        calls.get(0).setDisposition("ANSWERED");
        calls.get(1).setDisposition("NO ANSWER");
        calls.get(2).setDisposition("BUSY");
        calls.get(3).setDisposition("FAILED");
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR)).thenReturn(calls);

        CallHistoryResultDTO result = callHistoryService.getHistory(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.lines().get(0).dispositionLabel()).isEqualTo("Atendida");
        assertThat(result.lines().get(1).dispositionLabel()).isEqualTo("Não Atendeu");
        assertThat(result.lines().get(2).dispositionLabel()).isEqualTo("Ocupado");
        assertThat(result.lines().get(3).dispositionLabel()).isEqualTo("Falhou");
    }

    @Test
    void translateDisposition_returnsOriginalForUnknownValue() {
        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));

        Call call = buildCall("call-unknown", LocalDateTime.of(2026, 3, 1, 10, 0), 60, CallType.FIXED_LOCAL, CallDirection.OUTBOUND);
        call.setDisposition("CANCELLED");
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR)).thenReturn(List.of(call));

        CallHistoryResultDTO result = callHistoryService.getHistory(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.lines().get(0).dispositionLabel()).isEqualTo("CANCELLED");
    }

    @Test
    void translateDisposition_returnsDashForNull() {
        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));

        Call call = buildCall("call-null", LocalDateTime.of(2026, 3, 1, 10, 0), 60, CallType.FIXED_LOCAL, CallDirection.OUTBOUND);
        call.setDisposition(null);
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR)).thenReturn(List.of(call));

        CallHistoryResultDTO result = callHistoryService.getHistory(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.lines().get(0).dispositionLabel()).isEqualTo("—");
    }

    // -------------------------------------------------------------------------
    // generatePdf_returnsNonEmptyBytes
    // -------------------------------------------------------------------------

    @Test
    void generatePdf_returnsNonEmptyBytes() {
        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));

        Call call = buildCall("call-1", LocalDateTime.of(2026, 3, 1, 10, 0), 60, CallType.FIXED_LOCAL, CallDirection.OUTBOUND);
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR)).thenReturn(List.of(call));

        byte[] pdf = callHistoryService.generatePdf(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(pdf).isNotEmpty();
        assertThat((int) pdf[0]).isEqualTo(0x25); // %PDF signature
    }
}