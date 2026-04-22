package com.dionialves.AsteraComm.dashboard;

import com.dionialves.AsteraComm.call.CallRepository;
import com.dionialves.AsteraComm.call.OrphanCallReportService;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.trunk.TrunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private CircuitRepository circuitRepository;

    @Mock
    private TrunkRepository trunkRepository;

    @Mock
    private CallRepository callRepository;

    @Mock
    private OrphanCallReportService orphanCallReportService;

    @InjectMocks
    private DashboardService dashboardService;

    private final LocalDate now = LocalDate.now();
    private final int currentMonth = now.getMonthValue();
    private final int currentYear = now.getYear();
    private final int prevMonth = now.minusMonths(1).getMonthValue();
    private final int prevYear = now.minusMonths(1).getYear();

    // -------------------------------------------------------------------------
    // Painel de Circuitos
    // -------------------------------------------------------------------------

    @Test
    void getDashboard_circuitStats_totalAndOnlineAreCorrect() {
        stubCircuits(5L, 3L);
        stubTrunks(2L, 1L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.circuits().total()).isEqualTo(5);
        assertThat(result.circuits().online()).isEqualTo(3);
        assertThat(result.circuits().offline()).isEqualTo(2);
    }

    @Test
    void getDashboard_circuitStats_allOfflineWhenNoneOnline() {
        stubCircuits(4L, 0L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.circuits().online()).isEqualTo(0);
        assertThat(result.circuits().offline()).isEqualTo(4);
    }

    @Test
    void getDashboard_circuitStats_allOnlineWhenNoneOffline() {
        stubCircuits(3L, 3L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.circuits().online()).isEqualTo(3);
        assertThat(result.circuits().offline()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Painel de Troncos
    // -------------------------------------------------------------------------

    @Test
    void getDashboard_trunkStats_totalAndRegisteredAreCorrect() {
        stubCircuits(0L, 0L);
        stubTrunks(4L, 2L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.trunks().total()).isEqualTo(4);
        assertThat(result.trunks().registered()).isEqualTo(2);
        assertThat(result.trunks().unregistered()).isEqualTo(2);
    }

    @Test
    void getDashboard_trunkStats_allUnregisteredWhenNoneRegistered() {
        stubCircuits(0L, 0L);
        stubTrunks(3L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.trunks().registered()).isEqualTo(0);
        assertThat(result.trunks().unregistered()).isEqualTo(3);
    }

    // -------------------------------------------------------------------------
    // Painel de Ligações
    // -------------------------------------------------------------------------

    @Test
    void getDashboard_callStats_totalAndMinutesAreCorrect() {
        stubCircuits(0L, 0L);
        stubTrunks(0L, 0L);
        // 120 calls, 7200 bill-seconds (= 120 minutes)
        stubCalls(120L, 7200L, 90L, 20L, 10L, BigDecimal.ZERO, BigDecimal.ZERO);

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.calls().total()).isEqualTo(120);
        assertThat(result.calls().totalMinutes()).isEqualTo(120);
    }

    @Test
    void getDashboard_callStats_minutesRoundedDown() {
        stubCircuits(0L, 0L);
        stubTrunks(0L, 0L);
        // 90 bill-seconds = 1 full minute (floor)
        stubCalls(1L, 90L, 1L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.calls().totalMinutes()).isEqualTo(1);
    }

    @Test
    void getDashboard_callStats_dispositionBreakdown() {
        stubCircuits(0L, 0L);
        stubTrunks(0L, 0L);
        stubCalls(50L, 3000L, 40L, 7L, 3L, BigDecimal.ZERO, BigDecimal.ZERO);

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.calls().answered()).isEqualTo(40);
        assertThat(result.calls().noAnswer()).isEqualTo(7);
        assertThat(result.calls().busy()).isEqualTo(3);
    }

    @Test
    void getDashboard_callStats_allZerosWhenNoCallsInPeriod() {
        stubCircuits(0L, 0L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.calls().total()).isEqualTo(0);
        assertThat(result.calls().totalMinutes()).isEqualTo(0);
        assertThat(result.calls().answered()).isEqualTo(0);
        assertThat(result.calls().noAnswer()).isEqualTo(0);
        assertThat(result.calls().busy()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Painel de Faturamento
    // -------------------------------------------------------------------------

    @Test
    void getDashboard_billingStats_excedentsAndPreviousMonthAreCorrect() {
        stubCircuits(0L, 0L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L,
                new BigDecimal("450.00"),
                new BigDecimal("380.00"));

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.billing().excedents())
                .isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(result.billing().previousMonthCost())
                .isEqualByComparingTo(new BigDecimal("380.00"));
    }

    @Test
    void getDashboard_billingStats_zeroWhenNoCostsInPeriod() {
        stubCircuits(0L, 0L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.billing().excedents())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.billing().previousMonthCost())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getDashboard_billingStats_queriesCorrectMonthsForCurrentAndPrevious() {
        stubCircuits(0L, 0L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);

        dashboardService.getDashboard();

        verify(callRepository).sumCostByPeriod(currentMonth, currentYear);
        verify(callRepository).sumCostByPeriod(prevMonth, prevYear);
    }

    // -------------------------------------------------------------------------
    // Cobertura do período correto nas queries de ligações
    // -------------------------------------------------------------------------

    @Test
    void getDashboard_callStats_queriesCurrentMonthAndYear() {
        stubCircuits(0L, 0L);
        stubTrunks(0L, 0L);
        stubCalls(10L, 600L, 8L, 1L, 1L, BigDecimal.ZERO, BigDecimal.ZERO);

        dashboardService.getDashboard();

        verify(callRepository).countByPeriod(currentMonth, currentYear);
        verify(callRepository).sumBillSecondsByPeriod(currentMonth, currentYear);
        verify(callRepository).countByDispositionAndPeriod("ANSWERED", currentMonth, currentYear);
        verify(callRepository).countByDispositionAndPeriod("NO ANSWER", currentMonth, currentYear);
        verify(callRepository).countByDispositionAndPeriod("BUSY", currentMonth, currentYear);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Near-limit circuits — exclusão de excedidos e ordenação
    // -------------------------------------------------------------------------

    @Test
    void getDashboard_nearLimitCircuits_excludesExceededCircuits() {
        stubCircuits(2L, 2L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(callRepository.findCircuitConsumption(currentMonth, currentYear)).thenReturn(List.of(
                consumptionRow("1001", "Cliente A", "Plano X", 110L, 100L), // 110% — excedido
                consumptionRow("1002", "Cliente B", "Plano Y", 90L, 100L) // 90% — próximo do limite
        ));

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.nearLimitCircuits()).hasSize(1);
        assertThat(result.nearLimitCircuits().get(0).circuit()).isEqualTo("1002");
    }

    @Test
    void getDashboard_nearLimitCircuits_sortedByPercentDescending() {
        stubCircuits(3L, 3L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(callRepository.findCircuitConsumption(currentMonth, currentYear)).thenReturn(List.of(
                consumptionRow("1001", "A", "P", 50L, 100L), // 50%
                consumptionRow("1002", "B", "P", 80L, 100L), // 80%
                consumptionRow("1003", "C", "P", 70L, 100L) // 70%
        ));

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.nearLimitCircuits())
                .extracting(DashboardDTO.CircuitConsumption::circuit)
                .containsExactly("1002", "1003", "1001");
    }

    // -------------------------------------------------------------------------
    // Circuit overage stats (gráfico donut)
    // -------------------------------------------------------------------------

    @Test
    void getDashboard_circuitOverage_countsExceededAndWithinLimit() {
        stubCircuits(3L, 3L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(callRepository.findCircuitConsumption(currentMonth, currentYear)).thenReturn(List.of(
                consumptionRow("1001", "A", "P", 110L, 100L), // excedido
                consumptionRow("1002", "B", "P", 80L, 100L), // dentro do limite
                consumptionRow("1003", "C", "P", 50L, 100L) // dentro do limite
        ));

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.circuitOverage().exceeded()).isEqualTo(1);
        assertThat(result.circuitOverage().withinLimit()).isEqualTo(2);
    }

    @Test
    void getDashboard_circuitOverage_allWithinLimitWhenNoneExceeded() {
        stubCircuits(2L, 2L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(callRepository.findCircuitConsumption(currentMonth, currentYear)).thenReturn(List.of(
                consumptionRow("1001", "A", "P", 60L, 100L),
                consumptionRow("1002", "B", "P", 85L, 100L)));

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.circuitOverage().exceeded()).isEqualTo(0);
        assertThat(result.circuitOverage().withinLimit()).isEqualTo(2);
    }

    @Test
    void getDashboard_circuitOverage_zeroWhenNoCircuitsWithPackage() {
        stubCircuits(0L, 0L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(callRepository.findCircuitConsumption(currentMonth, currentYear)).thenReturn(List.of());

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.circuitOverage().exceeded()).isEqualTo(0);
        assertThat(result.circuitOverage().withinLimit()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // PER_CATEGORY circuits — gráfico de consumo próximo ao limite
    // -------------------------------------------------------------------------

    @Test
    void getDashboard_nearLimitCircuits_includesPerCategoryCircuit() {
        stubCircuits(1L, 1L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(callRepository.findPerCategoryCircuitConsumption(currentMonth, currentYear)).thenReturn(List.<Object[]>of(
                perCategoryRow("2001", "Cliente C", "Plano Z",
                        100, null, null, null,
                        80L, 0L, 0L, 0L) // fixo local 80% — próximo do limite
        ));

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.nearLimitCircuits()).hasSize(1);
        assertThat(result.nearLimitCircuits().get(0).circuit()).isEqualTo("2001");
        assertThat(result.nearLimitCircuits().get(0).percent()).isEqualTo(80.0);
    }

    @Test
    void getDashboard_nearLimitCircuits_perCategoryShowsMostCriticalCategory() {
        stubCircuits(1L, 1L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(callRepository.findPerCategoryCircuitConsumption(currentMonth, currentYear)).thenReturn(List.<Object[]>of(
                perCategoryRow("2001", "Cliente C", "Plano Z",
                        100, 100, 100, 100,
                        50L, 90L, 30L, 60L) // fixo LD = 90% é a mais crítica
        ));

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.nearLimitCircuits()).hasSize(1);
        DashboardDTO.CircuitConsumption c = result.nearLimitCircuits().get(0);
        assertThat(c.percent()).isEqualTo(90.0);
        assertThat(c.planName()).contains("Fixo LD");
        assertThat(c.usedMinutes()).isEqualTo(90L);
        assertThat(c.limitMinutes()).isEqualTo(100L);
    }

    @Test
    void getDashboard_circuitOverage_countsPerCategoryExceededCircuit() {
        stubCircuits(1L, 1L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(callRepository.findPerCategoryCircuitConsumption(currentMonth, currentYear)).thenReturn(List.<Object[]>of(
                perCategoryRow("2001", "Cliente C", "Plano Z",
                        100, null, null, null,
                        110L, 0L, 0L, 0L) // 110% — excedido
        ));

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.circuitOverage().exceeded()).isEqualTo(1);
        assertThat(result.circuitOverage().withinLimit()).isEqualTo(0);
    }

    @Test
    void getDashboard_nearLimitCircuits_perCategoryPlanNameIncludesCategoryLabel() {
        stubCircuits(1L, 1L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(callRepository.findPerCategoryCircuitConsumption(currentMonth, currentYear)).thenReturn(List.<Object[]>of(
                perCategoryRow("2001", "Cliente C", "Plano Z",
                        null, null, 100, null, // apenas móvel local tem limite
                        0L, 0L, 70L, 0L)
        ));

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.nearLimitCircuits().get(0).planName()).isEqualTo("Plano Z — Móvel Local");
    }

    // -------------------------------------------------------------------------
    // Top 10 circuitos por consumo — PER_CATEGORY
    // -------------------------------------------------------------------------

    @Test
    void getDashboard_topCircuits_includesPerCategoryCircuit() {
        stubCircuits(1L, 1L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(callRepository.findPerCategoryCircuitConsumption(currentMonth, currentYear)).thenReturn(List.<Object[]>of(
                perCategoryRow("2001", "Cliente C", "Plano Z",
                        100, 100, 100, 100,
                        30L, 20L, 10L, 5L) // total usado = 65
        ));

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.topCircuits()).hasSize(1);
        assertThat(result.topCircuits().get(0).circuit()).isEqualTo("2001");
        assertThat(result.topCircuits().get(0).usedMinutes()).isEqualTo(65L);
        assertThat(result.topCircuits().get(0).limitMinutes()).isEqualTo(400L);
    }

    @Test
    void getDashboard_topCircuits_sortedByUsedMinutesDescMixed() {
        stubCircuits(2L, 2L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(callRepository.findCircuitConsumption(currentMonth, currentYear)).thenReturn(List.<Object[]>of(
                consumptionRow("1001", "Cliente A", "Plano X", 80L, 100L)  // UNIFIED 80 min
        ));
        when(callRepository.findPerCategoryCircuitConsumption(currentMonth, currentYear)).thenReturn(List.<Object[]>of(
                perCategoryRow("2001", "Cliente C", "Plano Z",
                        200, null, null, null,
                        90L, 0L, 0L, 0L) // PER_CATEGORY 90 min — deve aparecer primeiro
        ));

        DashboardDTO result = dashboardService.getDashboard();

        assertThat(result.topCircuits()).hasSize(2);
        assertThat(result.topCircuits().get(0).circuit()).isEqualTo("2001");
        assertThat(result.topCircuits().get(1).circuit()).isEqualTo("1001");
    }

    @Test
    void getDashboard_topCircuits_perCategoryAggregatesAllCategories() {
        stubCircuits(1L, 1L);
        stubTrunks(0L, 0L);
        stubCalls(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(callRepository.findPerCategoryCircuitConsumption(currentMonth, currentYear)).thenReturn(List.<Object[]>of(
                perCategoryRow("2001", "Cliente C", "Plano Z",
                        50, 60, null, 80,          // limites: 50+60+80 = 190 (null ignorado)
                        10L, 20L, 5L, 15L)          // usados: 10+20+5+15 = 50
        ));

        DashboardDTO result = dashboardService.getDashboard();

        DashboardDTO.TopCircuit top = result.topCircuits().get(0);
        assertThat(top.usedMinutes()).isEqualTo(50L);
        assertThat(top.limitMinutes()).isEqualTo(190L);
    }

    private Object[] consumptionRow(String circuit, String customer, String plan, long used, long limit) {
        return new Object[] { circuit, customer, plan, used, limit };
    }

    private Object[] perCategoryRow(String circuit, String customer, String plan,
            Integer limitFixedLocal, Integer limitFixedLd, Integer limitMobileLocal, Integer limitMobileLd,
            long usedFixedLocal, long usedFixedLd, long usedMobileLocal, long usedMobileLd) {
        return new Object[] { circuit, customer, plan,
                limitFixedLocal, limitFixedLd, limitMobileLocal, limitMobileLd,
                usedFixedLocal, usedFixedLd, usedMobileLocal, usedMobileLd };
    }

    private void stubCircuits(long total, long online) {
        when(circuitRepository.count()).thenReturn(total);
        when(circuitRepository.countOnline()).thenReturn(online);
    }

    private void stubTrunks(long total, long registered) {
        when(trunkRepository.count()).thenReturn(total);
        when(trunkRepository.countRegistered()).thenReturn(registered);
    }

    private void stubCalls(long total, long billSeconds, long answered, long noAnswer, long busy,
            BigDecimal currentCost, BigDecimal prevCost) {
        when(callRepository.countByPeriod(currentMonth, currentYear)).thenReturn(total);
        when(callRepository.sumBillSecondsByPeriod(currentMonth, currentYear)).thenReturn(billSeconds);
        when(callRepository.countByDispositionAndPeriod("ANSWERED", currentMonth, currentYear)).thenReturn(answered);
        when(callRepository.countByDispositionAndPeriod("NO ANSWER", currentMonth, currentYear)).thenReturn(noAnswer);
        when(callRepository.countByDispositionAndPeriod("BUSY", currentMonth, currentYear)).thenReturn(busy);
        when(callRepository.sumCostByPeriod(currentMonth, currentYear)).thenReturn(currentCost);
        when(callRepository.sumCostByPeriod(prevMonth, prevYear)).thenReturn(prevCost);
    }
}
