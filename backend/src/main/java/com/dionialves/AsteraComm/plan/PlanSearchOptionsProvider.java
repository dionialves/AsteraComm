package com.dionialves.AsteraComm.plan;

import com.dionialves.AsteraComm.infra.fragment.SearchOptionDTO;
import com.dionialves.AsteraComm.infra.fragment.SearchOptionsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("planId")
@RequiredArgsConstructor
public class PlanSearchOptionsProvider implements SearchOptionsProvider {

    private final PlanRepository planRepository;

    @Override
    public List<SearchOptionDTO> search(String query) {
        return planRepository.findAllSummary().stream()
                .filter(p -> query == null || query.isBlank()
                        || p.name().toLowerCase().contains(query.toLowerCase()))
                .limit(20)
                .map(p -> new SearchOptionDTO(String.valueOf(p.id()), p.name()))
                .collect(Collectors.toList());
    }
}
