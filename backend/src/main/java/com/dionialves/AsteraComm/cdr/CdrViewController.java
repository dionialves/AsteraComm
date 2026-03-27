package com.dionialves.AsteraComm.cdr;

import com.dionialves.AsteraComm.call.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/cdrs")
@RequiredArgsConstructor
public class CdrViewController {

    private final CallService callService;

    @GetMapping
    public String list() {
        return "pages/cdrs/list";
    }

    @GetMapping("/table")
    public String table(
            @RequestParam(required = false) String src,
            @RequestParam(required = false) String dst,
            @RequestParam(required = false) String circuitNumber,
            @RequestParam(required = false) String disposition,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "callDate,desc") String sort,
            Model model) {

        String[] parts = sort.split(",");
        String field = parts[0];
        String dir   = parts.length > 1 ? parts[1] : "desc";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, field));
        LocalDateTime fromDt = from != null ? from.atStartOfDay()      : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59)    : null;

        model.addAttribute("calls",         callService.getAll(src, dst, disposition, fromDt, toDt, circuitNumber, pageable));
        model.addAttribute("sort",          field + "," + dir);
        model.addAttribute("disposition",   disposition   != null ? disposition   : "");
        model.addAttribute("circuitNumber", circuitNumber != null ? circuitNumber : "");

        return "pages/cdrs/table :: table";
    }

    @GetMapping("/{id}/modal")
    public String modal(@PathVariable Long id, Model model) {
        callService.findById(id).ifPresent(c -> model.addAttribute("call", c));
        return "pages/cdrs/modal :: modal";
    }
}
