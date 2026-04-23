package com.dionialves.AsteraComm.report.callhistory;

import com.dionialves.AsteraComm.call.CallDirection;
import com.dionialves.AsteraComm.call.CallType;

import java.time.LocalDateTime;

public record CallHistoryLineDTO(
        String        uniqueId,
        LocalDateTime callDate,
        String        callerNumber,
        String        dst,
        String        disposition,
        CallType      callType,
        CallDirection direction,
        int           billSeconds,
        Integer       durationSeconds
) {}