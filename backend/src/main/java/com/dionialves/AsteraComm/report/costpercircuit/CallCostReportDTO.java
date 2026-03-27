package com.dionialves.AsteraComm.report.costpercircuit;

import java.math.BigDecimal;

public record CallCostReportDTO(
        String customerName,
        String circuitName,
        long callCount,
        long totalMinutes,
        BigDecimal totalCost
) {}
