package com.dionialves.AsteraComm.report.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/cost-simulation")
    public ResponseEntity<AuditResultDTO> simulate(
            @RequestParam String circuitNumber,
            @RequestParam int    month,
            @RequestParam int    year) {
        return ResponseEntity.ok(auditService.simulate(circuitNumber, month, year));
    }
}
