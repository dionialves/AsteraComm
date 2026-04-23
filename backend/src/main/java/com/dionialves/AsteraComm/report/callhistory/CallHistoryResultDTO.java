package com.dionialves.AsteraComm.report.callhistory;

import java.math.BigDecimal;
import java.util.List;

public record CallHistoryResultDTO(
        String                  circuitNumber,
        int                     month,
        int                     year,
        int                     totalCalls,
        BigDecimal              totalMinutes,
        List<CallHistoryLineDTO> lines
) {}