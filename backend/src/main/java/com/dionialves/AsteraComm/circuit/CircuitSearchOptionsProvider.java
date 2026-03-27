package com.dionialves.AsteraComm.circuit;

import com.dionialves.AsteraComm.infra.fragment.SearchOptionDTO;
import com.dionialves.AsteraComm.infra.fragment.SearchOptionsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("circuitNumber")
@RequiredArgsConstructor
public class CircuitSearchOptionsProvider implements SearchOptionsProvider {

    private final CircuitRepository circuitRepository;

    @Override
    public List<SearchOptionDTO> search(String query) {
        return circuitRepository.findAllSummary().stream()
                .filter(c -> query == null || query.isBlank()
                        || c.number().toLowerCase().contains(query.toLowerCase())
                        || (c.customerName() != null && c.customerName().toLowerCase().contains(query.toLowerCase())))
                .limit(20)
                .map(c -> new SearchOptionDTO(c.number(),
                        c.number() + (c.customerName() != null ? " — " + c.customerName() : "")))
                .collect(Collectors.toList());

    }
}
