package com.dionialves.AsteraComm.call;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

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
    public String table(@RequestParam int month, @RequestParam int year,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "50") int size,
                        Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrphanCallReportDTO> orphansPage = reportService.findOrphanCalls(month, year, pageable);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        model.addAttribute("orphans", orphansPage);
        model.addAttribute("resolvableCount", reportService.countResolvable(month, year));
        return "pages/reports/orphan-calls-table :: table";
    }

    @PostMapping("/link")
    public String linkOrphanCalls(@RequestParam int month, @RequestParam int year, Model model) {
        int linked = reportService.linkOrphanCalls(month, year);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        Pageable pageable = PageRequest.of(0, 50);
        model.addAttribute("orphans", reportService.findOrphanCalls(month, year, pageable));
        model.addAttribute("resolvableCount", reportService.countResolvable(month, year));
        model.addAttribute("linkResult", linked);
        return "pages/reports/orphan-calls-table :: table";
    }
}