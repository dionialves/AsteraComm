package com.dionialves.AsteraComm.report.costpercircuit;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reports")
public class CostPerCircuitController {

    private final CostPerCircuitService costPerCircuitService;

    @GetMapping("/call-cost")
    public List<CallCostReportDTO> getCallCostReport(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(defaultValue = "false") boolean onlyWithCost) {
        return costPerCircuitService.getReport(month, year, onlyWithCost);
    }

    @GetMapping("/cost-per-circuit")
    public CostPerCircuitResponseDTO getCostPerCircuit(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(defaultValue = "false") boolean onlyWithCost) {
        return costPerCircuitService.getCostPerCircuit(month, year, onlyWithCost);
    }

    @GetMapping("/cost-per-circuit/pdf")
    public ResponseEntity<byte[]> getCostPerCircuitPdf(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(defaultValue = "false") boolean onlyWithCost) {
        byte[] pdf = costPerCircuitService.generateCostPerCircuitPdf(month, year, onlyWithCost);
        String filename = "relatorio-custo-" + month + "-" + year + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
