package com.dionialves.AsteraComm.report.costpercircuit;

import java.math.BigDecimal;

public record CostPerCircuitSummaryDTO(
        int totalCircuits,
        long totalCalls,
        long totalMinutes,
        BigDecimal totalCost) {}
