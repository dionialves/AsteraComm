package com.dionialves.AsteraComm.asterisk.endpoint;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EndpointFactory {

    private final EndpointRepository endpointRepository;

    @Autowired
    public EndpointFactory(EndpointRepository endpointRepository) {
        this.endpointRepository = endpointRepository;
    }

    public Endpoint getById(String id) {
        return endpointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Endpoint not found: " + id));
    }

    public List<Endpoint> getAll() {
        return endpointRepository.findAll();
    }
}
