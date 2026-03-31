package com.dionialves.AsteraComm.call;

import com.dionialves.AsteraComm.asterisk.endpoint.Endpoint;
import com.dionialves.AsteraComm.asterisk.endpoint.EndpointRepository;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.plan.PackageType;
import com.dionialves.AsteraComm.plan.Plan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallCostingServiceTest {

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private CallRepository callRepository;

    @InjectMocks
    private CallCostingService callCostingService;

    private static final String CIRCUIT_NUMBER   = "1001";
    private static final String OUTBOUND_CONTEXT = "from-internal";
    private static final BigDecimal FIXED_LOCAL_RATE = new BigDecimal("0.0900");

    private Circuit circuit;
    private Plan plan;

    @BeforeEach
    void setUp() {
        plan = new Plan();
        plan.setId(1L);
        plan.setName("Basic Plan");
        plan.setMonthlyPrice(new BigDecimal("99.90"));
        plan.setFixedLocal(FIXED_LOCAL_RATE);
        plan.setFixedLongDistance(new BigDecimal("0.2100"));
        plan.setMobileLocal(new BigDecimal("0.4500"));
        plan.setMobileLongDistance(new BigDecimal("0.5500"));
        plan.setPackageType(PackageType.NONE);

        circuit = new Circuit();
        circuit.setNumber(CIRCUIT_NUMBER);
        circuit.setPlan(plan);
    }

    private Call buildCall(int billSeconds, CallType callType) {
        Call call = new Call();
        call.setUniqueId("test.001");
        call.setCallDate(LocalDateTime.of(2026, 3, 15, 10, 0, 0));
        call.setCallerNumber("11933334444");
        call.setDst("1133334444");
        call.setDurationSeconds(billSeconds);
        call.setBillSeconds(billSeconds);
        call.setDisposition("ANSWERED");
        call.setCallType(callType);
        call.setProcessedAt(LocalDateTime.now());
        call.setCircuit(circuit);
        return call;
    }

    private Endpoint buildEndpoint(String context) {
        Endpoint ep = new Endpoint();
        ep.setId(CIRCUIT_NUMBER);
        ep.setContext(context);
        return ep;
    }

    // -------------------------------------------------------------------------
    // NO_CIRCUIT
    // -------------------------------------------------------------------------

    @Test
    void applyCosting_setsNoCircuit_whenCircuitIsNull() {
        Call call = buildCall(60, CallType.FIXED_LOCAL);
        call.setCircuit(null);

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.NO_CIRCUIT);
        assertThat(call.getCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(endpointRepository, callRepository);
    }

    // -------------------------------------------------------------------------
    // OUT_OF_SCOPE
    // -------------------------------------------------------------------------

    @Test
    void applyCosting_setsOutOfScope_whenDcontextDoesNotMatchEndpointContext() {
        Call call = buildCall(60, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));

        callCostingService.applyCosting(call, "incoming");

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.OUT_OF_SCOPE);
        assertThat(call.getCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(callRepository);
    }

    @Test
    void applyCosting_setsOutOfScope_whenEndpointNotFound() {
        Call call = buildCall(60, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER)).thenReturn(Optional.empty());

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.OUT_OF_SCOPE);
        assertThat(call.getCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(callRepository);
    }

    // -------------------------------------------------------------------------
    // NO_PLAN
    // -------------------------------------------------------------------------

    @Test
    void applyCosting_setsNoPlan_whenPlanIsNull() {
        circuit.setPlan(null);
        Call call = buildCall(60, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.NO_PLAN);
        assertThat(call.getCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(callRepository);
    }

    // -------------------------------------------------------------------------
    // UNIFIED quota
    // -------------------------------------------------------------------------

    @Test
    void applyCosting_setsZeroCost_whenCallFullyWithinUnifiedQuota() {
        plan.setPackageType(PackageType.UNIFIED);
        plan.setPackageTotalMinutes(100);
        // quota = 100 min; 30 min usados; remaining = 70 min; ligação 60s = 1.0 min → coberta
        Call call = buildCall(60, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));
        when(callRepository.sumQuotaMinutes(CIRCUIT_NUMBER, 3, 2026))
                .thenReturn(new BigDecimal("30.0"));

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.PROCESSED);
        assertThat(call.getCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(new BigDecimal("1.0"));
    }

    @Test
    void applyCosting_chargesPartialCost_whenCallPartiallyExceedsUnifiedQuota() {
        plan.setPackageType(PackageType.UNIFIED);
        plan.setPackageTotalMinutes(100);
        // quota = 100 min; 98.0 min usados; remaining = 2.0 min; ligação 180s = 3.0 min
        // billableSeconds = 180 - (2.0 * 60) = 60s → ceil(60/30)/2 = 1.0 min → 1*(0.09/2)*2 = 0.09
        Call call = buildCall(180, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));
        when(callRepository.sumQuotaMinutes(CIRCUIT_NUMBER, 3, 2026))
                .thenReturn(new BigDecimal("98.0"));

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.PROCESSED);
        assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(new BigDecimal("2.0"));
        assertThat(call.getCost()).isEqualByComparingTo(new BigDecimal("0.09"));
    }

    @Test
    void applyCosting_chargesFullCost_whenUnifiedQuotaFullyExhausted() {
        plan.setPackageType(PackageType.UNIFIED);
        plan.setPackageTotalMinutes(100);
        // quota = 100 min; 100.0 min usados (esgotado); remaining = 0; ligação 60s → 1.0 min → 0.09
        Call call = buildCall(60, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));
        when(callRepository.sumQuotaMinutes(CIRCUIT_NUMBER, 3, 2026))
                .thenReturn(new BigDecimal("100.0"));

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.PROCESSED);
        assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(call.getCost()).isEqualByComparingTo(new BigDecimal("0.09"));
    }

    // -------------------------------------------------------------------------
    // NONE — direct billing, no quota
    // -------------------------------------------------------------------------

    @Test
    void applyCosting_chargesDirectly_whenPlanHasNoQuota() {
        plan.setPackageType(PackageType.NONE);
        // 90s → ceil(90/30)/2 = 1.5 min → 3*(0.09/2) = 0.135
        Call call = buildCall(90, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.PROCESSED);
        assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(call.getCost()).isEqualByComparingTo(new BigDecimal("0.135"));
        verifyNoInteractions(callRepository);
    }

    // -------------------------------------------------------------------------
    // PER_CATEGORY quota
    // -------------------------------------------------------------------------

    @Test
    void applyCosting_usesPerTypeQuota_whenPlanIsPerCategory() {
        plan.setPackageType(PackageType.PER_CATEGORY);
        plan.setPackageFixedLocal(50);   // 50 min para FIXED_LOCAL
        plan.setPackageMobileLocal(20);  // 20 min para MOBILE_LOCAL (não usado)
        // 40.0 min usados; remaining = 10.0 min; ligação 60s = 1.0 min → coberta
        Call call = buildCall(60, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));
        when(callRepository.sumQuotaMinutesByType(CIRCUIT_NUMBER, CallType.FIXED_LOCAL.name(), 3, 2026))
                .thenReturn(new BigDecimal("40.0"));

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.PROCESSED);
        assertThat(call.getCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(new BigDecimal("1.0"));
        verify(callRepository, never()).sumQuotaMinutes(any(), anyInt(), anyInt());
    }

    @Test
    void applyCosting_consumes1Point5Minutes_for61SecondCallWithinQuota() {
        plan.setPackageType(PackageType.UNIFIED);
        plan.setPackageTotalMinutes(100);
        // ligação de 61s: ceil(61/30)/2 = 3/2 = 1.5 min debitados do plano
        // quota = 100 min; 0 usados; remaining = 100 min; coberta por completo
        Call call = buildCall(61, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));
        when(callRepository.sumQuotaMinutes(CIRCUIT_NUMBER, 3, 2026))
                .thenReturn(BigDecimal.ZERO);

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.PROCESSED);
        assertThat(call.getCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(new BigDecimal("1.5"));
    }

    @Test
    void applyCosting_consumes7Point5Minutes_for7min27secCallWithinQuota() {
        plan.setPackageType(PackageType.UNIFIED);
        plan.setPackageTotalMinutes(100);
        // ligação de 7:27 = 447s: ceil(447/30)/2 = 15/2 = 7.5 min debitados do plano
        // quota = 100 min; 0 usados; remaining = 100 min; coberta por completo
        Call call = buildCall(447, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));
        when(callRepository.sumQuotaMinutes(CIRCUIT_NUMBER, 3, 2026))
                .thenReturn(BigDecimal.ZERO);

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.PROCESSED);
        assertThat(call.getCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(new BigDecimal("7.5"));
    }

    // -------------------------------------------------------------------------
    // Fraction rounding
    // -------------------------------------------------------------------------

    @Test
    void applyCosting_roundsUpTo0Point5Minutes_for15Seconds() {
        plan.setPackageType(PackageType.NONE);
        // 15s → ceil(15/30)/2 = 0.5 min → 1*(0.09/2) = 0.045
        Call call = buildCall(15, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCost()).isEqualByComparingTo(new BigDecimal("0.045"));
    }

    @Test
    void applyCosting_roundsUpTo1Minute_for35Seconds() {
        plan.setPackageType(PackageType.NONE);
        // 35s → ceil(35/30)/2 = 1.0 min → 2*(0.09/2) = 0.09
        Call call = buildCall(35, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCost()).isEqualByComparingTo(new BigDecimal("0.09"));
    }

    // -------------------------------------------------------------------------
    // Short call — 3 seconds or less
    // -------------------------------------------------------------------------

    @Test
    void applyCosting_setsZeroCost_whenBillSecondsIsThree() {
        Call call = buildCall(3, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.PROCESSED);
        assertThat(call.getCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(callRepository);
    }

    @Test
    void applyCosting_setsZeroCost_whenBillSecondsIsZero() {
        Call call = buildCall(0, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.PROCESSED);
        assertThat(call.getCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(call.getMinutesFromQuota()).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(callRepository);
    }

    @Test
    void applyCosting_chargesNormally_whenBillSecondsIsFour() {
        plan.setPackageType(PackageType.NONE);
        // 4s → ceil(4/30)/2 = 0.5 min → 0.045
        Call call = buildCall(4, CallType.FIXED_LOCAL);
        when(endpointRepository.findById(CIRCUIT_NUMBER))
                .thenReturn(Optional.of(buildEndpoint(OUTBOUND_CONTEXT)));

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        assertThat(call.getCallStatus()).isEqualTo(CallStatus.PROCESSED);
        assertThat(call.getCost()).isGreaterThan(BigDecimal.ZERO);
    }

    // -------------------------------------------------------------------------
    // Idempotency
    // -------------------------------------------------------------------------

    @Test
    void applyCosting_skipsProcessing_whenCallStatusIsAlreadySet() {
        Call call = buildCall(60, CallType.FIXED_LOCAL);
        call.setCallStatus(CallStatus.PROCESSED);

        callCostingService.applyCosting(call, OUTBOUND_CONTEXT);

        verifyNoInteractions(endpointRepository, callRepository);
    }
}
