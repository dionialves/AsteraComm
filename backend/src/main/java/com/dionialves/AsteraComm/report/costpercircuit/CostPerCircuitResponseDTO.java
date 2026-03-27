package com.dionialves.AsteraComm.report.costpercircuit;

import java.util.List;

public record CostPerCircuitResponseDTO(
        CostPerCircuitSummaryDTO summary,
        List<CallCostReportDTO> data) {}
