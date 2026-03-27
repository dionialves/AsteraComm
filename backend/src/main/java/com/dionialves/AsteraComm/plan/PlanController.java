package com.dionialves.AsteraComm.plan;

import com.dionialves.AsteraComm.plan.dto.PlanCreateDTO;
import com.dionialves.AsteraComm.plan.dto.PlanUpdateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    @GetMapping("/summary")
    public ResponseEntity<?> findAllSummary() {
        return ResponseEntity.ok(planService.findAllSummary());
    }

    @GetMapping
    public Page<Plan> findAll(
            @RequestParam(defaultValue = "") String search,
            @RequestParam Optional<Boolean> active,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return planService.getAll(search, active.orElse(null), pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Plan> findById(@PathVariable Long id) {
        return planService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Plan> create(@Valid @RequestBody PlanCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Plan> update(@PathVariable Long id, @Valid @RequestBody PlanUpdateDTO dto) {
        return ResponseEntity.ok(planService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
