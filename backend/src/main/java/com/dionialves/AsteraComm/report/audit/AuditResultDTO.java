package com.dionialves.AsteraComm.report.audit;

import java.util.List;

public record AuditResultDTO(
        String               circuitNumber,
        String               planName,
        int                  month,
        int                  year,
        List<AuditCallLineDTO> lines,
        AuditSummaryDTO      summary
) {}
