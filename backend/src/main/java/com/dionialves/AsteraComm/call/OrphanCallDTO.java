package com.dionialves.AsteraComm.call;

import java.time.LocalDateTime;

public record OrphanCallDTO(
        Long callId,
        String uniqueId,
        LocalDateTime callDate,
        String reasonOrDst,
        String circuitCode,
        boolean resolvable
) {}