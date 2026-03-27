package com.dionialves.AsteraComm.asterisk.endpoint;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EndpointStatusRepository extends JpaRepository<EndpointStatus, Long> {
    Optional<EndpointStatus> findTopByEndpointOrderByCheckedAtDesc(Endpoint endpoint);
    void deleteByEndpoint(Endpoint endpoint);
}
