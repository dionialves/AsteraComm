package com.dionialves.AsteraComm.dashboard;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardViewController {

    private static final String[] MONTHS_PT = {
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    private final DashboardService dashboardService;

    public DashboardViewController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String index(Model model) {
        LocalDate now = LocalDate.now();
        model.addAttribute("dashboard", dashboardService.getDashboard());
        model.addAttribute("subtitle",
            "Painel de monitoramento — " + MONTHS_PT[now.getMonthValue() - 1] + "/" + now.getYear());
        return "pages/dashboard/index";
    }
}
