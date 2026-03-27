package com.dionialves.AsteraComm.customer;

import com.dionialves.AsteraComm.customer.dto.CustomerSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT new com.dionialves.AsteraComm.customer.dto.CustomerSummaryDTO(c.id, c.name) FROM Customer c WHERE c.enabled = true ORDER BY c.name")
    List<CustomerSummaryDTO> findAllSummary();

    Page<Customer> findByNameContainingIgnoreCase(String search, Pageable pageable);

    Page<Customer> findByEnabled(boolean enabled, Pageable pageable);

    Page<Customer> findByEnabledAndNameContainingIgnoreCase(boolean enabled, String search, Pageable pageable);
}
