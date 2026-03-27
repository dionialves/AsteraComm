package com.dionialves.AsteraComm.plan;

import com.dionialves.AsteraComm.plan.dto.PlanCreateDTO;
import com.dionialves.AsteraComm.plan.dto.PlanUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanViewController {

    private final PlanService planService;

    @GetMapping
    public String list() {
        return "pages/plans/list";
    }

    @GetMapping("/table")
    public String table(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Boolean active,
            Model model) {

        model.addAttribute("plans", fetchPage(page, size, sort, search, active));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("activeFilter", active);
        return "pages/plans/table :: table";
    }

    @GetMapping("/modal/new")
    public String modalNew(Model model) {
        model.addAttribute("plan", null);
        model.addAttribute("packageTypes", PackageType.values());
        return "pages/plans/modal :: modal";
    }

    @GetMapping("/{id}/modal")
    public String modalEdit(@PathVariable Long id, Model model) {
        Plan plan = planService.findById(id).orElse(null);
        model.addAttribute("plan", plan);
        model.addAttribute("packageTypes", PackageType.values());
        if (plan != null) {
            model.addAttribute("packageType", plan.getPackageType());
        }
        return "pages/plans/modal :: modal";
    }

    @GetMapping("/package-fields")
    public String packageFields(@RequestParam PackageType packageType, Model model) {
        model.addAttribute("packageType", packageType);
        model.addAttribute("plan", null);
        return "pages/plans/package-fields :: fields";
    }

    @GetMapping("/{id}/package-fields")
    public String packageFieldsEdit(@PathVariable Long id, @RequestParam PackageType packageType, Model model) {
        model.addAttribute("packageType", packageType);
        model.addAttribute("plan", planService.findById(id).orElse(null));
        return "pages/plans/package-fields :: fields";
    }

    @PostMapping
    public String create(
            @RequestParam String name,
            @RequestParam BigDecimal monthlyPrice,
            @RequestParam BigDecimal fixedLocal,
            @RequestParam BigDecimal fixedLongDistance,
            @RequestParam BigDecimal mobileLocal,
            @RequestParam BigDecimal mobileLongDistance,
            @RequestParam PackageType packageType,
            @RequestParam(required = false) Integer packageTotalMinutes,
            @RequestParam(required = false) Integer packageFixedLocal,
            @RequestParam(required = false) Integer packageFixedLongDistance,
            @RequestParam(required = false) Integer packageMobileLocal,
            @RequestParam(required = false) Integer packageMobileLongDistance,
            Model model) {
        try {
            Plan plan = planService.create(new PlanCreateDTO(name, monthlyPrice, fixedLocal, fixedLongDistance,
                    mobileLocal, mobileLongDistance, packageType, packageTotalMinutes,
                    packageFixedLocal, packageFixedLongDistance, packageMobileLocal, packageMobileLongDistance));
            model.addAttribute("plan", plan);
            model.addAttribute("packageTypes", PackageType.values());
            model.addAttribute("packageType", plan.getPackageType());
            model.addAttribute("toastMsg", "Plano criado com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
            model.addAttribute("plans", fetchPage(0, 20, "name,asc", "", null));
            model.addAttribute("sort", "name,asc");
            model.addAttribute("activeFilter", null);
        } catch (Exception e) {
            model.addAttribute("plan", null);
            model.addAttribute("packageTypes", PackageType.values());
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        return "pages/plans/modal :: modal";
    }

    @PutMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam BigDecimal monthlyPrice,
            @RequestParam BigDecimal fixedLocal,
            @RequestParam BigDecimal fixedLongDistance,
            @RequestParam BigDecimal mobileLocal,
            @RequestParam BigDecimal mobileLongDistance,
            @RequestParam PackageType packageType,
            @RequestParam(required = false) Integer packageTotalMinutes,
            @RequestParam(required = false) Integer packageFixedLocal,
            @RequestParam(required = false) Integer packageFixedLongDistance,
            @RequestParam(required = false) Integer packageMobileLocal,
            @RequestParam(required = false) Integer packageMobileLongDistance,
            @RequestParam(required = false) Boolean active,
            Model model) {
        try {
            planService.update(id, new PlanUpdateDTO(name, monthlyPrice, fixedLocal, fixedLongDistance,
                    mobileLocal, mobileLongDistance, packageType, packageTotalMinutes,
                    packageFixedLocal, packageFixedLongDistance, packageMobileLocal, packageMobileLongDistance,
                    Boolean.TRUE.equals(active)));
            model.addAttribute("toastMsg", "Plano atualizado com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
        } catch (Exception e) {
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        Plan plan = planService.findById(id).orElse(null);
        model.addAttribute("plan", plan);
        model.addAttribute("packageTypes", PackageType.values());
        if (plan != null) model.addAttribute("packageType", plan.getPackageType());
        return "pages/plans/modal :: modal";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, Model model) {
        try {
            planService.delete(id);
            model.addAttribute("toastMsg", "Plano removido com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
            model.addAttribute("tableUrl", "/plans/table");
            return "fragments/modal-close :: close";
        } catch (Exception e) {
            Plan plan = planService.findById(id).orElse(null);
            model.addAttribute("plan", plan);
            model.addAttribute("packageTypes", PackageType.values());
            if (plan != null) model.addAttribute("packageType", plan.getPackageType());
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
            return "pages/plans/modal :: modal";
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String tableFullResponse(Model model) {
        model.addAttribute("plans", fetchPage(0, 20, "name,asc", "", null));
        model.addAttribute("search", "");
        model.addAttribute("sort", "name,asc");
        model.addAttribute("activeFilter", null);
        return "pages/plans/table";
    }

    private Page<Plan> fetchPage(int page, int size, String sort, String search, Boolean active) {
        String[] parts = sort.split(",");
        String field = parts[0];
        Sort.Direction dir = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, field));
        return planService.getAll(search.isBlank() ? null : search, active, pageable);
    }
}
