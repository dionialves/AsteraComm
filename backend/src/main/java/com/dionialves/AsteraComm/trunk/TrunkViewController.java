package com.dionialves.AsteraComm.trunk;

import com.dionialves.AsteraComm.trunk.dto.TrunkCreateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/trunks")
@RequiredArgsConstructor
public class TrunkViewController {

    private final TrunkService trunkService;

    @GetMapping
    public String list() {
        return "pages/trunks/list";
    }

    @GetMapping("/table")
    public String table(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(defaultValue = "") String search,
            Model model) {

        model.addAttribute("trunks", fetchPage(page, size, sort, search));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        return "pages/trunks/table :: table";
    }

    @GetMapping("/modal/new")
    public String modalNew(Model model) {
        model.addAttribute("trunk", null);
        return "pages/trunks/modal :: modal";
    }

    @GetMapping("/{name}/modal")
    public String modalEdit(@PathVariable String name, Model model) {
        model.addAttribute("trunk", trunkService.findByName(name).orElse(null));
        return "pages/trunks/modal :: modal";
    }

    @PostMapping
    public String create(
            @RequestParam String name,
            @RequestParam String host,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "") String prefix,
            @RequestParam(defaultValue = "CREDENTIAL") String authType,
            @RequestParam(required = false) String identifyMatch,
            Model model) {
        try {
            TrunkAuthType type = TrunkAuthType.valueOf(authType);
            Trunk trunk = trunkService.create(new TrunkCreateDTO(
                    name, host,
                    blankToNull(username), blankToNull(password),
                    blankToNull(prefix),
                    type, blankToNull(identifyMatch)));
            model.addAttribute("trunk", trunk);
            model.addAttribute("toastMsg", "Tronco criado com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
        } catch (Exception e) {
            model.addAttribute("trunk", null);
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        return "pages/trunks/modal :: modal";
    }

    @PutMapping("/{name}")
    public String update(
            @PathVariable String name,
            @RequestParam String host,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "") String password,
            @RequestParam(defaultValue = "") String prefix,
            @RequestParam(required = false) String identifyMatch,
            Model model) {
        try {
            Trunk existing = trunkService.findByName(name).orElse(null);
            TrunkAuthType type = existing != null ? existing.getAuthType() : TrunkAuthType.CREDENTIAL;
            trunkService.update(name, new TrunkCreateDTO(
                    name, host,
                    blankToNull(username), blankToNull(password),
                    blankToNull(prefix),
                    type, blankToNull(identifyMatch)));
            model.addAttribute("toastMsg", "Tronco atualizado com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
        } catch (Exception e) {
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        model.addAttribute("trunk", trunkService.findByName(name).orElse(null));
        return "pages/trunks/modal :: modal";
    }

    @DeleteMapping("/{name}")
    public String delete(@PathVariable String name, Model model) {
        try {
            trunkService.delete(name);
            model.addAttribute("toastMsg", "Tronco removido com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
            model.addAttribute("tableUrl", "/trunks/table");
            return "fragments/modal-close :: close";
        } catch (Exception e) {
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
            model.addAttribute("trunk", trunkService.findByName(name).orElse(null));
            return "pages/trunks/modal :: modal";
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Page<TrunkProjection> fetchPage(int page, int size, String sort, String search) {
        String[] parts = sort.split(",");
        String field = parts[0];
        Sort.Direction dir = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, field));
        return trunkService.getAll(search, pageable);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
