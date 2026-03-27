package com.dionialves.AsteraComm.customer;

import com.dionialves.AsteraComm.infra.fragment.SearchOptionDTO;
import com.dionialves.AsteraComm.infra.fragment.SearchOptionsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("customerId")
@RequiredArgsConstructor
public class CustomerSearchOptionsProvider implements SearchOptionsProvider {

    private final CustomerRepository customerRepository;

    @Override
    public List<SearchOptionDTO> search(String query) {
        return customerRepository.findAllSummary().stream()
                .filter(c -> query == null || query.isBlank()
                        || c.name().toLowerCase().contains(query.toLowerCase()))
                .limit(20)
                .map(c -> new SearchOptionDTO(String.valueOf(c.id()), c.name()))
                .collect(Collectors.toList());
    }
}
