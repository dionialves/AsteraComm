package com.dionialves.AsteraComm.report.callhistory;

import com.dionialves.AsteraComm.call.CallDirection;
import com.dionialves.AsteraComm.call.CallType;

import java.time.LocalDateTime;

public record CallHistoryLineDTO(
        String        uniqueId,
        LocalDateTime callDate,
        String        callerNumber,
        String        dst,
        String        dispositionLabel,
        CallType      callType,
        CallDirection direction,
        String        directionLabel,
        int           billSeconds,
        Integer       durationSeconds
) {}