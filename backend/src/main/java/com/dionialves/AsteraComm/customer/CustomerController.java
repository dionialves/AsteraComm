package com.dionialves.AsteraComm.customer;

import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.customer.dto.CustomerCreateDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CircuitRepository circuitRepository;

    @GetMapping("/summary")
    public ResponseEntity<?> findAllSummary() {
        return ResponseEntity.ok(customerService.findAllSummary());
    }

    @GetMapping
    public Page<?> findAll(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Boolean enabled = "ACTIVE".equals(status) ? Boolean.TRUE : "INACTIVE".equals(status) ? Boolean.FALSE : null;
        return customerService.getAll(search, enabled, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        return customerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CustomerCreateDTO dto) {
        Customer customer = customerService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(customer);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CustomerCreateDTO dto) {
        Customer customer = customerService.update(id, dto);
        return ResponseEntity.ok(customer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<?> disable(@PathVariable Long id) {
        Customer customer = customerService.disable(id);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/{id}/circuits")
    public ResponseEntity<?> findCircuits(@PathVariable Long id) {
        return ResponseEntity.ok(circuitRepository.findByCustomerIdProjected(id));
    }
}
