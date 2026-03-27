package com.dionialves.AsteraComm.call;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/calls")
public class CallController {

    private final CallService callService;

    @GetMapping
    public Page<Call> findAll(
            @RequestParam(required = false) String src,
            @RequestParam(required = false) String dst,
            @RequestParam(required = false) String disposition,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String circuitNumber,
            @PageableDefault(size = 20, sort = "callDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return callService.getAll(src, dst, disposition, from, to, circuitNumber, pageable);
    }

    @GetMapping("/{uniqueid:.+}")
    public ResponseEntity<Call> findByUniqueId(@PathVariable String uniqueid) {
        return callService.findByUniqueId(uniqueid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
