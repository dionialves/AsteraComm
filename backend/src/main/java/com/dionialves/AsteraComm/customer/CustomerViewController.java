package com.dionialves.AsteraComm.customer;

import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.customer.dto.CustomerCreateDTO;
import com.dionialves.AsteraComm.customer.dto.CustomerResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerViewController {

    private final CustomerService customerService;
    private final CircuitRepository circuitRepository;

    @GetMapping
    public String list() {
        return "pages/customers/list";
    }

    @GetMapping("/table")
    public String table(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String status,
            Model model) {

        model.addAttribute("customers", fetchPage(page, size, sort, search, status));
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("statusFilter", status);
        return "pages/customers/table :: table";
    }

    @GetMapping("/modal/new")
    public String modalNew(Model model) {
        model.addAttribute("customer", null);
        return "pages/customers/modal :: modal";
    }

    @GetMapping("/{id}/modal")
    public String modalEdit(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id).orElse(null);
        model.addAttribute("customer", customer);
        if (customer != null) {
            model.addAttribute("circuits", circuitRepository.findByCustomerIdProjected(id));
        }
        return "pages/customers/modal :: modal";
    }

    @PostMapping
    public String create(
            @RequestParam String name,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            Model model) {
        try {
            Customer customer = customerService.create(new CustomerCreateDTO(name, Boolean.TRUE.equals(enabled)));
            model.addAttribute("customer", customer);
            model.addAttribute("circuits", circuitRepository.findByCustomerIdProjected(customer.getId()));
            model.addAttribute("toastMsg", "Cliente criado com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
        } catch (Exception e) {
            model.addAttribute("customer", null);
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        return "pages/customers/modal :: modal";
    }

    @PutMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            Model model) {
        try {
            customerService.update(id, new CustomerCreateDTO(name, Boolean.TRUE.equals(enabled)));
            model.addAttribute("toastMsg", "Cliente atualizado com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
        } catch (Exception e) {
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
        }
        Customer customer = customerService.findById(id).orElse(null);
        model.addAttribute("customer", customer);
        if (customer != null) {
            model.addAttribute("circuits", circuitRepository.findByCustomerIdProjected(id));
        }
        return "pages/customers/modal :: modal";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, Model model) {
        try {
            customerService.delete(id);
            model.addAttribute("toastMsg", "Cliente removido com sucesso.");
            model.addAttribute("toastType", "success");
            model.addAttribute("refreshTable", true);
            model.addAttribute("tableUrl", "/customers/table");
            return "fragments/modal-close :: close";
        } catch (Exception e) {
            Customer customer = customerService.findById(id).orElse(null);
            model.addAttribute("customer", customer);
            if (customer != null) {
                model.addAttribute("circuits", circuitRepository.findByCustomerIdProjected(id));
            }
            model.addAttribute("toastMsg", e.getMessage());
            model.addAttribute("toastType", "error");
            return "pages/customers/modal :: modal";
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String tableFullResponse(Model model) {
        model.addAttribute("customers", fetchPage(0, 20, "id,desc", "", ""));
        model.addAttribute("search", "");
        model.addAttribute("sort", "id,desc");
        model.addAttribute("statusFilter", "");
        return "pages/customers/table";
    }

    private Page<CustomerResponseDTO> fetchPage(int page, int size, String sort, String search, String status) {
        String[] parts = sort.split(",");
        String field = parts[0];
        Sort.Direction dir = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, field));
        Boolean enabled = "ACTIVE".equals(status) ? Boolean.TRUE : "INACTIVE".equals(status) ? Boolean.FALSE : null;
        return customerService.getAll(search.isBlank() ? null : search, enabled, pageable);
    }
}
