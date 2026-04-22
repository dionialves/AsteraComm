package com.dionialves.AsteraComm.call;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reports/orphan-calls")
public class OrphanCallReportController {

    private final OrphanCallReportService reportService;

    @GetMapping
    public String index(Model model) {
        LocalDate now = LocalDate.now();
        model.addAttribute("currentMonth", now.getMonthValue());
        model.addAttribute("currentYear", now.getYear());
        return "pages/reports/orphan-calls";
    }

    @GetMapping("/table")
    public String table(@RequestParam int month, @RequestParam int year, Model model) {
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("orphans", reportService.findOrphanCalls(month, year));
        return "pages/reports/orphan-calls-table :: table";
    }

    @GetMapping("/count")
    public ResponseEntity<Long> count() {
        return ResponseEntity.ok(reportService.countOrphanCallsCurrentMonth());
    }
}