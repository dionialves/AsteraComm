package com.dionialves.AsteraComm.call;

import java.util.List;

public record ReconciliationResultDTO(
        int linked,
        int skipped,
        List<String> skippedDetails
) {}