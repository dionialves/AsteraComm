package com.dionialves.AsteraComm.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDTO(
        CircuitStats circuits,
        TrunkStats trunks,
        CallStats calls,
        BillingStats billing,
        List<DailyCallStat> dailyCalls,
        List<MonthlyBillingStat> monthlyBilling,
        List<CircuitConsumption> nearLimitCircuits,
        List<TopCircuit> topCircuits,
        CircuitOverageStats circuitOverage,
        long orphanCalls) {

    public record CircuitStats(long total, long online, long offline) {
    }

    public record TrunkStats(long total, long registered, long unregistered) {
    }

    public record CallStats(long total, long totalMinutes, long answered, long noAnswer, long busy) {
    }

    public record BillingStats(
            BigDecimal excedents,
            BigDecimal previousMonthCost) {
    }

    public record DailyCallStat(String date, long answered, long noAnswer, long busy, long failed) {
    }

    public record MonthlyBillingStat(String month, BigDecimal excedents) {
    }

    public record CircuitConsumption(
            String circuit,
            String customerName,
            String planName,
            long usedMinutes,
            long limitMinutes,
            double percent) {
    }

    public record TopCircuit(String customerName, String circuit, long usedMinutes, long limitMinutes) {
    }

    public record CircuitOverageStats(long exceeded, long withinLimit) {
    }
}
