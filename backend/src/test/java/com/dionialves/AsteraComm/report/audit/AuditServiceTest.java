package com.dionialves.AsteraComm.report.audit;

import com.dionialves.AsteraComm.call.Call;
import com.dionialves.AsteraComm.call.CallDirection;
import com.dionialves.AsteraComm.call.CallRepository;
import com.dionialves.AsteraComm.call.CallStatus;
import com.dionialves.AsteraComm.call.CallType;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.exception.NotFoundException;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private CallRepository callRepository;

    @Mock
    private CircuitRepository circuitRepository;

    @InjectMocks
    private AuditService auditService;

    private static final String CIRCUIT_NUMBER  = "1001";
    private static final int    MONTH           = 3;
    private static final int    YEAR            = 2026;
    private static final BigDecimal FIXED_LOCAL_RATE = new BigDecimal("0.0900");

    private Circuit circuit;
    private Plan    plan;

    @BeforeEach
    void setUp() {
        plan = new Plan();
        plan.setId(1L);
        plan.setName("Plano Básico");
        plan.setFixedLocal(FIXED_LOCAL_RATE);
        plan.setFixedLongDistance(new BigDecimal("0.2100"));
        plan.setMobileLocal(new BigDecimal("0.4500"));
        plan.setMobileLongDistance(new BigDecimal("0.5500"));
        plan.setPackageType(PackageType.NONE);

        circuit = new Circuit();
        circuit.setNumber(CIRCUIT_NUMBER);
        circuit.setPlan(plan);
    }

    private Call buildCall(String uniqueId, LocalDateTime callDate, int billSeconds, CallType callType) {
        return buildCall(uniqueId, callDate, billSeconds, callType, CallDirection.OUTBOUND);
    }

    private Call buildCall(String uniqueId, LocalDateTime callDate, int billSeconds, CallType callType, CallDirection direction) {
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
    // Circuito / Plano inválido
    // -------------------------------------------------------------------------

    @Test
    void simulate_throwsNotFoundException_whenCircuitNotFound() {
        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(CIRCUIT_NUMBER);
    }

    @Test
    void simulate_throwsNotFoundException_whenCircuitHasNoPlan() {
        circuit.setPlan(null);
        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));

        assertThatThrownBy(() -> auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR))
                .isInstanceOf(NotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Pacote NONE — cobrança direta sem desconto
    // -------------------------------------------------------------------------

    @Test
    void simulate_chargesAllCalls_whenPlanHasNoPackage() {
        // 2 ligações de 60s → cada uma: ceil(60/30)/2 = 1.0 min → 2*(0.09/2) = 0.09
        Call c1 = buildCall("uid1", LocalDateTime.of(2026, 3, 1, 9, 0, 0), 60, CallType.FIXED_LOCAL);
        Call c2 = buildCall("uid2", LocalDateTime.of(2026, 3, 2, 10, 0, 0), 60, CallType.FIXED_LOCAL);

        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(List.of(c1, c2));

        AuditResultDTO result = auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines().get(0).cost()).isEqualByComparingTo(new BigDecimal("0.09"));
        assertThat(result.lines().get(1).cost()).isEqualByComparingTo(new BigDecimal("0.09"));
        assertThat(result.summary().quotaMinutesUsed()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.summary().totalCost()).isEqualByComparingTo(new BigDecimal("0.18"));
    }

    // -------------------------------------------------------------------------
    // Pacote UNIFIED — suficiente para cobrir todo o mês
    // -------------------------------------------------------------------------

    @Test
    void simulate_zerosAllCosts_whenUnifiedPackageCoversTotalMonth() {
        plan.setPackageType(PackageType.UNIFIED);
        plan.setPackageTotalMinutes(10); // 10 min; 2 ligações de 60s = 2.0 min total → cobertas

        Call c1 = buildCall("uid1", LocalDateTime.of(2026, 3, 1, 9, 0, 0), 60, CallType.FIXED_LOCAL);
        Call c2 = buildCall("uid2", LocalDateTime.of(2026, 3, 2, 10, 0, 0), 60, CallType.FIXED_LOCAL);

        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(List.of(c1, c2));

        AuditResultDTO result = auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.lines()).allSatisfy(line -> {
            assertThat(line.cost()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(line.quotaUsedThisCall()).isPositive();
        });
        assertThat(result.summary().quotaMinutesUsed()).isEqualByComparingTo(new BigDecimal("2.0"));
        assertThat(result.summary().totalCost()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // -------------------------------------------------------------------------
    // Pacote UNIFIED — esgotado antes do fim do mês
    // -------------------------------------------------------------------------

    @Test
    void simulate_chargesCallsAfterQuotaExhausted_whenUnifiedPackageRunsOut() {
        plan.setPackageType(PackageType.UNIFIED);
        plan.setPackageTotalMinutes(2); // 2 min de pacote

        // 3 ligações de 60s (1.0 min cada): 2 primeiras cobertas, 3ª cobrada
        Call c1 = buildCall("uid1", LocalDateTime.of(2026, 3, 1, 9, 0, 0),  60, CallType.FIXED_LOCAL);
        Call c2 = buildCall("uid2", LocalDateTime.of(2026, 3, 2, 10, 0, 0), 60, CallType.FIXED_LOCAL);
        Call c3 = buildCall("uid3", LocalDateTime.of(2026, 3, 3, 11, 0, 0), 60, CallType.FIXED_LOCAL);

        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(List.of(c1, c2, c3));

        AuditResultDTO result = auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR);

        AuditCallLineDTO line1 = result.lines().get(0);
        AuditCallLineDTO line2 = result.lines().get(1);
        AuditCallLineDTO line3 = result.lines().get(2);

        assertThat(line1.cost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(line1.quotaUsedThisCall()).isEqualByComparingTo(new BigDecimal("1.0"));
        assertThat(line1.quotaAccumulated()).isEqualByComparingTo(new BigDecimal("1.0"));

        assertThat(line2.cost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(line2.quotaUsedThisCall()).isEqualByComparingTo(new BigDecimal("1.0"));
        assertThat(line2.quotaAccumulated()).isEqualByComparingTo(new BigDecimal("2.0"));

        // Pacote esgotado: 3ª ligação cobrada normalmente, contador congela em 2.0
        assertThat(line3.cost()).isEqualByComparingTo(new BigDecimal("0.09"));
        assertThat(line3.quotaUsedThisCall()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(line3.quotaAccumulated()).isEqualByComparingTo(new BigDecimal("2.0"));

        assertThat(result.summary().quotaMinutesUsed()).isEqualByComparingTo(new BigDecimal("2.0"));
        assertThat(result.summary().excessMinutes()).isEqualByComparingTo(new BigDecimal("1.0"));
        assertThat(result.summary().totalCost()).isEqualByComparingTo(new BigDecimal("0.09"));
    }

    // -------------------------------------------------------------------------
    // Ligação parcialmente coberta pelo pacote
    // -------------------------------------------------------------------------

    @Test
    void simulate_chargesOnlyExcess_whenCallPartiallyExceedsUnifiedQuota() {
        plan.setPackageType(PackageType.UNIFIED);
        plan.setPackageTotalMinutes(2); // 2 min de pacote

        // 1 ligação de 180s (3.0 min): 2.0 min cobertos pelo pacote, 60s excedentes cobrados
        // billableSeconds = 180 - (2.0 * 60) = 60s → ceil(60/30)/2 = 1.0 min → 0.09
        Call c1 = buildCall("uid1", LocalDateTime.of(2026, 3, 1, 9, 0, 0), 180, CallType.FIXED_LOCAL);

        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(List.of(c1));

        AuditResultDTO result = auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR);

        AuditCallLineDTO line = result.lines().get(0);
        assertThat(line.quotaUsedThisCall()).isEqualByComparingTo(new BigDecimal("2.0"));
        assertThat(line.quotaAccumulated()).isEqualByComparingTo(new BigDecimal("2.0"));
        assertThat(line.cost()).isEqualByComparingTo(new BigDecimal("0.09"));

        assertThat(result.summary().quotaMinutesUsed()).isEqualByComparingTo(new BigDecimal("2.0"));
        assertThat(result.summary().excessMinutes()).isEqualByComparingTo(new BigDecimal("1.0"));
    }

    @Test
    void simulate_consumes1Point5Minutes_for61SecondCallWithinQuota() {
        // ligação de 61s: ceil(61/30)/2 = 1.5 min debitados do plano
        plan.setPackageType(PackageType.UNIFIED);
        plan.setPackageTotalMinutes(10); // 10 min disponíveis

        Call c1 = buildCall("uid1", LocalDateTime.of(2026, 3, 1, 9, 0, 0), 61, CallType.FIXED_LOCAL);

        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(List.of(c1));

        AuditResultDTO result = auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.lines().get(0).quotaUsedThisCall()).isEqualByComparingTo(new BigDecimal("1.5"));
        assertThat(result.lines().get(0).cost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.summary().quotaMinutesUsed()).isEqualByComparingTo(new BigDecimal("1.5"));
    }

    // -------------------------------------------------------------------------
    // Pacote PER_CATEGORY — quotas independentes por tipo
    // -------------------------------------------------------------------------

    @Test
    void simulate_usesPerCategoryQuota_independently_perCallType() {
        plan.setPackageType(PackageType.PER_CATEGORY);
        plan.setPackageFixedLocal(1);  // 1 min para FIXED_LOCAL
        plan.setPackageMobileLocal(1); // 1 min para MOBILE_LOCAL

        // 1 call fixo local + 1 call móvel local: cada uma com seu próprio pacote → custo 0
        Call c1 = buildCall("uid1", LocalDateTime.of(2026, 3, 1, 9, 0, 0),  60, CallType.FIXED_LOCAL);
        Call c2 = buildCall("uid2", LocalDateTime.of(2026, 3, 1, 10, 0, 0), 60, CallType.MOBILE_LOCAL);

        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(List.of(c1, c2));

        AuditResultDTO result = auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.lines().get(0).cost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.lines().get(1).cost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.summary().totalCost()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void simulate_chargesAtCorrectRate_afterPerCategoryQuotaExhausted() {
        plan.setPackageType(PackageType.PER_CATEGORY);
        plan.setPackageFixedLocal(1); // apenas 1 min para FIXED_LOCAL

        // 2 ligações fixo local de 60s: 1ª coberta, 2ª cobrada à tarifa FIXED_LOCAL
        Call c1 = buildCall("uid1", LocalDateTime.of(2026, 3, 1, 9, 0, 0),  60, CallType.FIXED_LOCAL);
        Call c2 = buildCall("uid2", LocalDateTime.of(2026, 3, 2, 10, 0, 0), 60, CallType.FIXED_LOCAL);

        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(List.of(c1, c2));

        AuditResultDTO result = auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.lines().get(0).cost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.lines().get(1).cost()).isEqualByComparingTo(new BigDecimal("0.09"));
    }

    // -------------------------------------------------------------------------
    // Ligação curta (≤ 3s) — não cobrada, não consome pacote
    // -------------------------------------------------------------------------

    @Test
    void simulate_skipsShortCalls_withZeroCostAndNoQuotaUsage() {
        plan.setPackageType(PackageType.UNIFIED);
        plan.setPackageTotalMinutes(10);

        Call c1 = buildCall("uid1", LocalDateTime.of(2026, 3, 1, 9, 0, 0), 3, CallType.FIXED_LOCAL);

        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(List.of(c1));

        AuditResultDTO result = auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.lines().get(0).cost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.lines().get(0).quotaUsedThisCall()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.summary().quotaMinutesUsed()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // -------------------------------------------------------------------------
    // Resumo consolidado
    // -------------------------------------------------------------------------

    @Test
    void simulate_returnsCorrectSummary_forMixedCalls() {
        plan.setPackageType(PackageType.UNIFIED);
        plan.setPackageTotalMinutes(1); // 1 min de pacote

        // c1: 60s = 1.0 min → coberta (1.0 min do pacote)
        // c2: 60s = 1.0 min → pacote esgotado, cobrada (1.0 min em excesso)
        Call c1 = buildCall("uid1", LocalDateTime.of(2026, 3, 1, 9, 0, 0),  60, CallType.FIXED_LOCAL);
        Call c2 = buildCall("uid2", LocalDateTime.of(2026, 3, 2, 10, 0, 0), 60, CallType.FIXED_LOCAL);

        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(List.of(c1, c2));

        AuditResultDTO result = auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.summary().totalCalls()).isEqualTo(2);
        assertThat(result.summary().totalMinutes()).isEqualByComparingTo(new BigDecimal("2.0"));
        assertThat(result.summary().quotaMinutesUsed()).isEqualByComparingTo(new BigDecimal("1.0"));
        assertThat(result.summary().excessMinutes()).isEqualByComparingTo(new BigDecimal("1.0"));
        assertThat(result.summary().totalCost()).isEqualByComparingTo(new BigDecimal("0.09"));
    }

    // -------------------------------------------------------------------------
    // Metadados do resultado
    // -------------------------------------------------------------------------

    @Test
    void simulate_returnsCorrectMetadata() {
        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(List.of());

        AuditResultDTO result = auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.circuitNumber()).isEqualTo(CIRCUIT_NUMBER);
        assertThat(result.planName()).isEqualTo("Plano Básico");
        assertThat(result.month()).isEqualTo(MONTH);
        assertThat(result.year()).isEqualTo(YEAR);
    }

    // -------------------------------------------------------------------------
    // Direction filtering
    // -------------------------------------------------------------------------

    @Test
    void simulate_returnsOnlyOutbound_whenOnlyOutgoingTrue() {
        plan.setPackageType(PackageType.NONE);

        Call c1 = buildCall("uid1", LocalDateTime.of(2026, 3, 1, 9, 0, 0), 60, CallType.FIXED_LOCAL, CallDirection.OUTBOUND);
        Call c2 = buildCall("uid2", LocalDateTime.of(2026, 3, 2, 10, 0, 0), 60, CallType.FIXED_LOCAL, CallDirection.INBOUND);
        Call c3 = buildCall("uid3", LocalDateTime.of(2026, 3, 3, 11, 0, 0), 60, CallType.FIXED_LOCAL, CallDirection.OUTBOUND);

        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(List.of(c1, c2, c3));

        AuditResultDTO result = auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR, true);

        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines()).allMatch(l -> l.direction() == CallDirection.OUTBOUND);
    }

    @Test
    void simulate_returnsAll_whenOnlyOutgoingFalse() {
        plan.setPackageType(PackageType.NONE);

        Call c1 = buildCall("uid1", LocalDateTime.of(2026, 3, 1, 9, 0, 0), 60, CallType.FIXED_LOCAL, CallDirection.OUTBOUND);
        Call c2 = buildCall("uid2", LocalDateTime.of(2026, 3, 2, 10, 0, 0), 60, CallType.FIXED_LOCAL, CallDirection.INBOUND);

        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(List.of(c1, c2));

        AuditResultDTO result = auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR, false);

        assertThat(result.lines()).hasSize(2);
    }

    @Test
    void simulate_inboundCallsAppearInResult() {
        plan.setPackageType(PackageType.NONE);

        Call inbound = buildCall("uid1", LocalDateTime.of(2026, 3, 1, 9, 0, 0), 60, CallType.FIXED_LOCAL, CallDirection.INBOUND);

        when(circuitRepository.findByNumber(CIRCUIT_NUMBER)).thenReturn(Optional.of(circuit));
        when(callRepository.findByCircuitNumberAndPeriod(CIRCUIT_NUMBER, MONTH, YEAR))
                .thenReturn(List.of(inbound));

        AuditResultDTO result = auditService.simulate(CIRCUIT_NUMBER, MONTH, YEAR);

        assertThat(result.lines()).hasSize(1);
        assertThat(result.lines().get(0).direction()).isEqualTo(CallDirection.INBOUND);
    }
}
