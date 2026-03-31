package com.dionialves.AsteraComm.report.audit;

import com.dionialves.AsteraComm.call.CallType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AuditCallLineDTO(
        String        uniqueId,
        LocalDateTime callDate,
        String        dst,
        CallType      callType,
        int           billSeconds,
        BigDecimal    ratePerMinute,
        BigDecimal    quotaUsedThisCall,
        BigDecimal    quotaAccumulated,
        BigDecimal    cost
) {}
