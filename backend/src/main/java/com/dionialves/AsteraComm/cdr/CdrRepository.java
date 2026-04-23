package com.dionialves.AsteraComm.cdr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CdrRepository extends JpaRepository<CdrRecord, Long>,
        JpaSpecificationExecutor<CdrRecord> {

    Optional<CdrRecord> findFirstByUniqueId(String uniqueId);

    @Query("SELECT c FROM CdrRecord c WHERE c.uniqueId NOT IN (SELECT ca.uniqueId FROM Call ca)")
    List<CdrRecord> findUnprocessed();
}
