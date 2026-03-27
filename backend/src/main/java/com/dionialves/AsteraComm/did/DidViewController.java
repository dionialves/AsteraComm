package com.dionialves.AsteraComm.did;

import com.dionialves.AsteraComm.did.dto.DIDCreateDTO;
import com.dionialves.AsteraComm.did.dto.DIDResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/dids")
@RequiredArgsConstructor
public class DidViewController {

    private final DIDService didService;

    @GetMapping
    public String list() {
        return "pages/dids/list";
    }

    @GetMapping("/table")
    public String table(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String status,
            Model model) {

        model.addAttribute("dids", fetchPage(page, size, sort, search, status));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("statusFilter", status);
        return "pages/dids/table :: table";
    }

    @GetMapping("/modal/new")
    public String modalNew(Model model) {
        model.addAttribute("did", null);
        return "pages/dids/modal :: modal";
    }

    @GetMapping("/{id}/modal")
    public String modalEdit(@PathVariable Long id, Model model) {
        model.addAttribute("did", didService.findById(id).orElse(null));
        return "pages/dids/modal :: modal";
    }

    @PostMapping
    public String create(
            @RequestParam String number,
            @RequestParam(defaultValue = "") String circuitNumber,
            Model model) {
        try {
            DID did = didService.create(new DIDCreateDTO(number.replaceAll("\\D", "")));
            if (!circuitNumber.isBlank()) {
                did = didService.linkToCircuit(did.getId(), circuitNumber);
            }
            model.addAttribute("did", did);
            model.addAttribute("toastMsg", "DID criado com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
            model.addAttribute("dids", fetchPage(0, 20, "id,desc", "", ""));
            model.addAttribute("sort", "id,desc");
            model.addAttribute("statusFilter", "");
        } catch (Exception e) {
            model.addAttribute("did", null);
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        return "pages/dids/modal :: modal";
    }

    @PutMapping("/{id}/link")
    public String link(
            @PathVariable Long id,
            @RequestParam String circuitNumber,
            Model model) {
        try {
            didService.linkToCircuit(id, circuitNumber);
            model.addAttribute("toastMsg", "DID vinculado ao circuito.");
            model.addAttribute("toastType", "success");
        } catch (Exception e) {
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        model.addAttribute("clearModal", true);
        return tableFullResponse(model);
    }

    @PutMapping("/{id}/unlink")
    public String unlink(@PathVariable Long id, Model model) {
        try {
            didService.unlinkFromCircuit(id);
            model.addAttribute("toastMsg", "DID desvinculado.");
            model.addAttribute("toastType", "success");
        } catch (Exception e) {
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        model.addAttribute("clearModal", true);
        return tableFullResponse(model);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, Model model) {
        try {
            didService.delete(id);
            model.addAttribute("toastMsg", "DID removido com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
            model.addAttribute("tableUrl", "/dids/table");
            return "fragments/modal-close :: close";
        } catch (Exception e) {
            model.addAttribute("did", didService.findById(id).orElse(null));
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
            return "pages/dids/modal :: modal";
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String tableFullResponse(Model model) {
        model.addAttribute("dids", fetchPage(0, 20, "id,desc", "", ""));
        model.addAttribute("search", "");
        model.addAttribute("sort", "id,desc");
        model.addAttribute("statusFilter", "");
        return "pages/dids/table";
    }

    private Page<DIDResponseDTO> fetchPage(int page, int size, String sort, String search, String status) {
        String[] parts = sort.split(",");
        String field = parts[0];
        Sort.Direction dir = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, field));
        return didService.getAll(search.isBlank() ? null : search,
                status.isBlank() ? null : status, pageable);
    }
}
