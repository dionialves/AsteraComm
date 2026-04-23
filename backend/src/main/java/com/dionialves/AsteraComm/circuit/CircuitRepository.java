package com.dionialves.AsteraComm.circuit;

import com.dionialves.AsteraComm.circuit.dto.CircuitSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CircuitRepository extends JpaRepository<Circuit, Long> {

    @Query("SELECT new com.dionialves.AsteraComm.circuit.dto.CircuitSummaryDTO(c.number, cu.name) FROM Circuit c JOIN c.customer cu WHERE c.active = true ORDER BY c.number")
    List<CircuitSummaryDTO> findAllSummary();

    Optional<Circuit> findByNumber(String number);

    List<Circuit> findByNumberIn(List<String> numbers);

    @Query("SELECT c FROM Circuit c JOIN FETCH c.customer JOIN FETCH c.plan WHERE c.number = :number")
    Optional<Circuit> findByNumberForModal(@Param("number") String number);

    @Query(value = """
            SELECT MAX(c.number)
                FROM asteracomm_circuits c
                WHERE c.number ~ '^[0-9]+$'
                  AND CAST(c.number AS BIGINT) BETWEEN 100000 AND 999999
            """, nativeQuery = true)
    Optional<String> findMaxCode();

    boolean existsByCustomerId(Long customerId);

    long countByCustomerId(Long customerId);

    @Query(value = """
            WITH last_status AS (
                SELECT DISTINCT ON (endpoint) *
                FROM asteracomm_endpoint_status
                WHERE online = true
                ORDER BY endpoint, id DESC
            )
            SELECT
                c.id            AS id,
                c.number        AS number,
                c.password      AS password,
                c.trunk_name    AS trunkName,
                cu.name         AS customerName,
                p.name          AS planName,
                s.ip            AS ip,
                s.rtt           AS rtt,
                CASE WHEN s.id IS NOT NULL THEN true ELSE false END AS online,
                c.active        AS active
            FROM asteracomm_circuits c
            JOIN asteracomm_customers cu ON cu.id = c.customer_id
            LEFT JOIN asteracomm_plans p ON p.id = c.plan_id
            LEFT JOIN last_status s ON s.endpoint = c.number
            WHERE c.customer_id = :customerId
            ORDER BY c.id DESC
            """, nativeQuery = true)
    List<CircuitProjection> findByCustomerIdProjected(@Param("customerId") Long customerId);

    @Query(value = "SELECT COUNT(*) FROM asteracomm_circuits", nativeQuery = true)
    long countAll();

    @Query(value = "SELECT COUNT(*) FROM asteracomm_circuits WHERE active = true", nativeQuery = true)
    long countActive();

    @Query(value = "SELECT COUNT(*) FROM asteracomm_circuits WHERE active = false", nativeQuery = true)
    long countInactive();

    @Query(value = """
            SELECT COUNT(DISTINCT c.number)
            FROM asteracomm_circuits c
            JOIN (
                SELECT DISTINCT ON (endpoint) endpoint
                FROM asteracomm_endpoint_status
                WHERE online = true
                ORDER BY endpoint, id DESC
            ) s ON s.endpoint = c.number
            """, nativeQuery = true)
    long countOnline();

    @Query(value = """
            WITH last_status AS (
                SELECT DISTINCT ON (endpoint) *
                FROM asteracomm_endpoint_status
                WHERE online = true
                ORDER BY endpoint, id DESC
            )
            SELECT
                c.id            AS id,
                c.number        AS number,
                c.password      AS password,
                c.trunk_name    AS trunkName,
                cu.name         AS customerName,
                p.name          AS planName,
                s.ip            AS ip,
                s.rtt           AS rtt,
                CASE WHEN s.id IS NOT NULL THEN true ELSE false END AS online,
                c.active        AS active
            FROM asteracomm_circuits c
            JOIN asteracomm_customers cu ON cu.id = c.customer_id
            LEFT JOIN asteracomm_plans p ON p.id = c.plan_id
            LEFT JOIN last_status s ON s.endpoint = c.number
            WHERE (LOWER(c.number) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(cu.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.ip) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (CAST(:online AS boolean) IS NULL OR (s.id IS NOT NULL) = CAST(:online AS boolean))
            AND (CAST(:active AS boolean) IS NULL OR c.active = CAST(:active AS boolean))
            """, countQuery = """
            WITH last_status AS (
                SELECT DISTINCT ON (endpoint) *
                FROM asteracomm_endpoint_status
                WHERE online = true
                ORDER BY endpoint, id DESC
            )
            SELECT COUNT(*)
            FROM asteracomm_circuits c
            JOIN asteracomm_customers cu ON cu.id = c.customer_id
            LEFT JOIN asteracomm_plans p ON p.id = c.plan_id
            LEFT JOIN last_status s ON s.endpoint = c.number
            WHERE (LOWER(c.number) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(cu.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.ip) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (CAST(:online AS boolean) IS NULL OR (s.id IS NOT NULL) = CAST(:online AS boolean))
            AND (CAST(:active AS boolean) IS NULL OR c.active = CAST(:active AS boolean))
            """, nativeQuery = true)
    Page<CircuitProjection> findAllCircuits(@Param("search") String search,
            @Param("online") Boolean online,
            @Param("active") Boolean active,
            Pageable pageable);
}
