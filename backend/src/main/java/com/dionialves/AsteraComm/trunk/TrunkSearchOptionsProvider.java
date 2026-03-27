package com.dionialves.AsteraComm.trunk;

import com.dionialves.AsteraComm.infra.fragment.SearchOptionDTO;
import com.dionialves.AsteraComm.infra.fragment.SearchOptionsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("trunkName")
@RequiredArgsConstructor
public class TrunkSearchOptionsProvider implements SearchOptionsProvider {

    private final TrunkRepository trunkRepository;

    @Override
    public List<SearchOptionDTO> search(String query) {
        return trunkRepository.findAllSummary().stream()
                .filter(t -> query == null || query.isBlank()
                        || t.name().toLowerCase().contains(query.toLowerCase()))
                .limit(20)
                .map(t -> new SearchOptionDTO(t.name(), t.name()))
                .collect(Collectors.toList());
    }
}
