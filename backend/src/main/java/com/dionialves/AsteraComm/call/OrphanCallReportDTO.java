package com.dionialves.AsteraComm.call;

import java.time.LocalDateTime;

public record OrphanCallReportDTO(
        Long          callId,
        String        uniqueId,
        LocalDateTime callDate,
        String        dst,
        String        channel,
        String        dstChannel,
        String        circuitCode,
        boolean       resolvable
) {}