package com.dionialves.AsteraComm.trunk;

import com.dionialves.AsteraComm.trunk.dto.TrunkSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrunkRepository extends JpaRepository<Trunk, Long> {

    @Query("SELECT new com.dionialves.AsteraComm.trunk.dto.TrunkSummaryDTO(t.name) FROM Trunk t ORDER BY t.name")
    List<TrunkSummaryDTO> findAllSummary();

    Optional<Trunk> findByName(String name);

    boolean existsByName(String name);

    @Query(value = """
            SELECT COUNT(DISTINCT t.name)
            FROM asteracomm_trunks t
            JOIN (
                SELECT DISTINCT ON (trunk_name) trunk_name, registered
                FROM asteracomm_trunk_registration_status
                ORDER BY trunk_name, id DESC
            ) s ON s.trunk_name = t.name
            WHERE s.registered = true
            """, nativeQuery = true)
    long countRegistered();

    @Query(value = """
            WITH last_status AS (
                SELECT DISTINCT ON (trunk_name) *
                FROM asteracomm_trunk_registration_status
                ORDER BY trunk_name, id DESC
            )
            SELECT
                t.id AS id,
                t.name AS name,
                t.host AS host,
                t.username AS username,
                CASE WHEN s.registered IS NOT NULL AND s.registered THEN true ELSE false END AS registered
            FROM asteracomm_trunks t
            LEFT JOIN last_status s ON s.trunk_name = t.name
            WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.host) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.username) LIKE LOWER(CONCAT('%', :search, '%'))
            """, countQuery = """
            SELECT COUNT(*)
            FROM asteracomm_trunks t
            WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.host) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(t.username) LIKE LOWER(CONCAT('%', :search, '%'))
            """, nativeQuery = true)
    Page<TrunkProjection> findAllTrunks(@Param("search") String search, Pageable pageable);
}
