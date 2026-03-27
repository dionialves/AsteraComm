package com.dionialves.AsteraComm.trunk;

import com.dionialves.AsteraComm.trunk.dto.TrunkCreateDTO;
import com.dionialves.AsteraComm.trunk.dto.TrunkSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/trunks")
public class TrunkController {

    private final TrunkService trunkService;

    @GetMapping
    public Page<TrunkProjection> findAll(
            @RequestParam(required = false, defaultValue = "") String search,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return trunkService.getAll(search, pageable);
    }

    @GetMapping("/summary")
    public ResponseEntity<?> findAllSummary() {
        return ResponseEntity.ok(trunkService.findAllSummary());
    }

    @GetMapping("/{name}")
    public ResponseEntity<Trunk> findByName(@PathVariable String name) {
        return trunkService.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Trunk> create(@RequestBody TrunkCreateDTO dto) {
        Trunk trunk = trunkService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(trunk);
    }

    @PutMapping("/{name}")
    public ResponseEntity<Trunk> update(@PathVariable String name, @RequestBody TrunkCreateDTO dto) {
        Trunk trunk = trunkService.update(name, dto);
        return ResponseEntity.ok(trunk);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        trunkService.delete(name);
        return ResponseEntity.noContent().build();
    }
}
