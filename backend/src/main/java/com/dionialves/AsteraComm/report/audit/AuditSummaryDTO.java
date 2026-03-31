package com.dionialves.AsteraComm.report.audit;

import java.math.BigDecimal;

public record AuditSummaryDTO(
        int        totalCalls,
        BigDecimal totalMinutes,
        BigDecimal quotaMinutesUsed,
        BigDecimal excessMinutes,
        BigDecimal totalCost
) {}
