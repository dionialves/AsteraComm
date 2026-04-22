package com.dionialves.AsteraComm.call;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@Controller
@RequestMapping("/admin/reconcile-calls")
public class CallCircuitReconciliationController {

    private final CallCircuitReconciliationService reconciliationService;

    @GetMapping
    public String show(Model model) {
        model.addAttribute("orphans", reconciliationService.findOrphanCalls());
        return "pages/admin/reconcile-calls";
    }

    @PostMapping
    public String run(Model model) {
        ReconciliationResultDTO result = reconciliationService.reconcile();
        model.addAttribute("result", result);
        model.addAttribute("orphans", reconciliationService.findOrphanCalls());
        return "pages/admin/reconcile-calls";
    }
}