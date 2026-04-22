package com.dionialves.AsteraComm.report;

import com.dionialves.AsteraComm.report.audit.AuditResultDTO;
import com.dionialves.AsteraComm.report.audit.AuditService;
import com.dionialves.AsteraComm.report.costpercircuit.CostPerCircuitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportViewController {

    private final CostPerCircuitService costPerCircuitService;
    private final AuditService          auditService;

    @GetMapping
    public String index() {
        return "pages/reports/index";
    }

    // ── Cost per Circuit ───────────────────────────────────────────────────────

    @GetMapping("/cost-per-circuit")
    public String costPerCircuit(Model model) {
        LocalDate now = LocalDate.now();
        model.addAttribute("currentMonth", now.getMonthValue());
        model.addAttribute("currentYear",  now.getYear());
        return "pages/reports/cost-per-circuit";
    }

    @GetMapping("/cost-per-circuit/table")
    public String costPerCircuitTable(
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "false") boolean onlyWithCost,
            Model model) {

        if (month == 0 || year == 0) {
            LocalDate now = LocalDate.now();
            month = now.getMonthValue();
            year  = now.getYear();
        }
        model.addAttribute("report",        costPerCircuitService.getCostPerCircuit(month, year, onlyWithCost));
        model.addAttribute("month",         month);
        model.addAttribute("year",          year);
        model.addAttribute("onlyWithCost",  onlyWithCost);
        return "pages/reports/cost-per-circuit-table :: table";
    }

    @GetMapping("/cost-per-circuit/pdf")
    @ResponseBody
    public ResponseEntity<byte[]> costPerCircuitPdf(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(defaultValue = "false") boolean onlyWithCost) {

        byte[] pdf = costPerCircuitService.generateCostPerCircuitPdf(month, year, onlyWithCost);
        return pdfResponse(pdf, "custo-por-circuito-" + month + "-" + year + ".pdf");
    }

    // ── Audit ──────────────────────────────────────────────────────────────────

    @GetMapping("/audit")
    public String audit(Model model) {
        LocalDate now = LocalDate.now();
        model.addAttribute("currentMonth", now.getMonthValue());
        model.addAttribute("currentYear",  now.getYear());
        return "pages/reports/audit";
    }

    @GetMapping("/audit/simulation")
    public String auditSimulation(
            @RequestParam(required = false) String circuitNumber,
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "false") boolean onlyOutgoing,
            Model model) {

        model.addAttribute("month",         month);
        model.addAttribute("year",          year);
        model.addAttribute("circuitNumber", circuitNumber);
        model.addAttribute("onlyOutgoing",  onlyOutgoing);

        if (circuitNumber == null || circuitNumber.isBlank()) {
            model.addAttribute("errorMsg", "Selecione um circuito para simular.");
            return "pages/reports/audit-table :: table";
        }
        if (month == 0 || year == 0) {
            model.addAttribute("errorMsg", "Selecione o mês e o ano.");
            return "pages/reports/audit-table :: table";
        }
        try {
            AuditResultDTO result = auditService.simulate(circuitNumber, month, year, onlyOutgoing);
            model.addAttribute("result", result);
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
        }
        return "pages/reports/audit-table :: table";
    }

    @GetMapping("/audit/pdf")
    @ResponseBody
    public ResponseEntity<byte[]> auditPdf(
            @RequestParam String circuitNumber,
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(defaultValue = "false") boolean onlyRelevant,
            @RequestParam(defaultValue = "false") boolean onlyOutgoing) {

        byte[] pdf = auditService.generatePdf(circuitNumber, month, year, onlyRelevant, onlyOutgoing);
        return pdfResponse(pdf, "auditoria-" + circuitNumber + "-" + month + "-" + year + ".pdf");
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
