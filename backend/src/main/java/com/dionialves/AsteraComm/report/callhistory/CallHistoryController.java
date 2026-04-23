package com.dionialves.AsteraComm.report.callhistory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reports/call-history")
public class CallHistoryController {

    private final CallHistoryService service;

    @GetMapping
    public String index(Model model) {
        LocalDate now = LocalDate.now();
        model.addAttribute("currentMonth", now.getMonthValue());
        model.addAttribute("currentYear",  now.getYear());
        return "pages/reports/call-history";
    }

    @GetMapping("/table")
    public String table(@RequestParam String circuitNumber,
                        @RequestParam int month,
                        @RequestParam int year,
                        Model model) {
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        if (circuitNumber == null || circuitNumber.isBlank()) {
            model.addAttribute("errorMsg", "Selecione um circuito.");
        } else {
            model.addAttribute("result", service.getHistory(circuitNumber, month, year));
        }
        return "pages/reports/call-history-table :: table";
    }
}