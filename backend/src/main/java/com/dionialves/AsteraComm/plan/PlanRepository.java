package com.dionialves.AsteraComm.plan;

import com.dionialves.AsteraComm.plan.dto.PlanSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    @Query("SELECT new com.dionialves.AsteraComm.plan.dto.PlanSummaryDTO(p.id, p.name) FROM Plan p WHERE p.active = true ORDER BY p.name")
    List<PlanSummaryDTO> findAllSummary();

    boolean existsByName(String name);

    Page<Plan> findByNameContainingIgnoreCase(String search, Pageable pageable);

    Page<Plan> findByActive(boolean active, Pageable pageable);

    Page<Plan> findByActiveAndNameContainingIgnoreCase(boolean active, String search, Pageable pageable);

    // Returns false until client entity is implemented
    default boolean isLinkedToClient(Long id) {
        return false;
    }
}
