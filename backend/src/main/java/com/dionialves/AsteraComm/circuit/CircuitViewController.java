package com.dionialves.AsteraComm.circuit;

import com.dionialves.AsteraComm.call.CallRepository;
import com.dionialves.AsteraComm.circuit.dto.CircuitCreateDTO;
import com.dionialves.AsteraComm.did.DIDService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/circuits")
@RequiredArgsConstructor
public class CircuitViewController {

    private final CircuitService circuitService;
    private final CircuitRepository circuitRepository;
    private final DIDService didService;
    private final CallRepository callRepository;

    // ── Páginas ───────────────────────────────────────────────────────────────

    @GetMapping
    public String list() {
        return "pages/circuits/list";
    }

    @GetMapping("/table")
    public String table(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "number,asc") String sort,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String filter,
            Model model) {

        model.addAttribute("circuits", fetchPage(page, size, sort, search, filter));
        model.addAttribute("sort", sort);
        model.addAttribute("filter", filter);
        return "pages/circuits/table :: table";
    }

    // ── Modal ─────────────────────────────────────────────────────────────────

    @GetMapping("/modal/new")
    public String modalNew(Model model) {
        model.addAttribute("circuit", null);
        return "pages/circuits/modal :: modal";
    }

    @GetMapping("/{number}/modal")
    public String modalEdit(@PathVariable String number, Model model) {
        Circuit circuit = circuitRepository.findByNumberForModal(number).orElse(null);
        model.addAttribute("circuit", circuit);
        return "pages/circuits/modal :: modal";
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    @GetMapping("/{number}/tab/detalhes")
    public String tabDetalhes(@PathVariable String number, Model model) {
        Circuit circuit = circuitRepository.findByNumberForModal(number).orElse(null);
        model.addAttribute("circuit", circuit);
        return "pages/circuits/tab-detalhes :: detalhes";
    }

    @GetMapping("/{number}/tab/dids")
    public String tabDids(@PathVariable String number, Model model) {
        model.addAttribute("linkedDids", didService.getByCircuit(number));
        model.addAttribute("freeDids", didService.getFree());
        model.addAttribute("circuitNumber", number);
        return "pages/circuits/tab-dids :: dids";
    }

    @GetMapping("/{number}/tab/historico")
    public String tabHistorico(@PathVariable String number, Model model) {
        model.addAttribute("calls", callRepository.findRecentByCircuitNumber(number));
        return "pages/circuits/tab-history :: history";
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    public String create(
            @RequestParam String password,
            @RequestParam String trunkName,
            @RequestParam Long customerId,
            @RequestParam Long planId,
            @RequestParam(value = "active", required = false) Boolean active,
            Model model) {
        try {
            Circuit circuit = circuitService.create(
                    new CircuitCreateDTO(password, trunkName, customerId, planId, active));
            Circuit loaded = circuitRepository.findByNumberForModal(circuit.getNumber()).orElse(circuit);
            model.addAttribute("circuit", loaded);
            model.addAttribute("toastMsg", "Circuito " + circuit.getNumber() + " criado com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
        } catch (Exception e) {
            model.addAttribute("circuit", null);
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        return "pages/circuits/modal :: modal";
    }

    @PutMapping("/{number}")
    public String update(
            @PathVariable String number,
            @RequestParam String password,
            @RequestParam String trunkName,
            @RequestParam Long customerId,
            @RequestParam Long planId,
            @RequestParam(value = "active", required = false) Boolean active,
            Model model) {
        try {
            circuitService.update(number, new CircuitCreateDTO(password, trunkName, customerId, planId, active));
            model.addAttribute("toastMsg", "Circuito " + number + " atualizado com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
        } catch (Exception e) {
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        model.addAttribute("circuit", circuitRepository.findByNumberForModal(number).orElse(null));
        return "pages/circuits/modal :: modal";
    }

    @DeleteMapping("/{number}")
    public String delete(@PathVariable String number, Model model) {
        try {
            circuitService.delete(number);
            model.addAttribute("toastMsg", "Circuito " + number + " removido com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
            model.addAttribute("tableUrl", "/circuits/table");
            return "fragments/modal-close :: close";
        } catch (Exception e) {
            model.addAttribute("circuit", circuitRepository.findByNumberForModal(number).orElse(null));
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
            return "pages/circuits/modal :: modal";
        }
    }

    // ── DID link/unlink ───────────────────────────────────────────────────────

    @PostMapping("/{number}/dids/{didId}")
    public String linkDid(
            @PathVariable String number,
            @PathVariable Long didId,
            Model model) {
        try {
            didService.linkToCircuit(didId, number);
            model.addAttribute("toastMsg", "DID vinculado ao circuito.");
            model.addAttribute("toastType", "success");
        } catch (Exception e) {
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        model.addAttribute("linkedDids", didService.getByCircuit(number));
        model.addAttribute("freeDids", didService.getFree());
        model.addAttribute("circuitNumber", number);
        return "pages/circuits/tab-dids :: dids";
    }

    @DeleteMapping("/{number}/dids/{didId}")
    public String unlinkDid(
            @PathVariable String number,
            @PathVariable Long didId,
            Model model) {
        try {
            didService.unlinkFromCircuit(didId);
            model.addAttribute("toastMsg", "DID desvinculado do circuito.");
            model.addAttribute("toastType", "success");
        } catch (Exception e) {
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        model.addAttribute("linkedDids", didService.getByCircuit(number));
        model.addAttribute("freeDids", didService.getFree());
        model.addAttribute("circuitNumber", number);
        return "pages/circuits/tab-dids :: dids";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String tableFullResponse(Model model) {
        model.addAttribute("circuits", fetchPage(0, 20, "number,asc", "", ""));
        model.addAttribute("sort", "number,asc");
        model.addAttribute("filter", "");
        return "pages/circuits/table";
    }

    private Page<CircuitProjection> fetchPage(
            int page, int size, String sort, String search, String filter) {
        String[] parts = sort.split(",");
        String field = parts[0];
        Sort.Direction dir = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, field));
        // filter: '' → tudo | 'online' → online=true | 'offline' → online=false |
        // 'inactive' → active=false
        Boolean onlineParam = "online".equals(filter) ? Boolean.TRUE
                : "offline".equals(filter) ? Boolean.FALSE : null;
        Boolean activeParam = "inactive".equals(filter) ? Boolean.FALSE : null;
        return circuitService.getAll(
                search.isBlank() ? null : search,
                onlineParam,
                activeParam,
                pageable);
    }
}
