package com.dionialves.AsteraComm.did;

import com.dionialves.AsteraComm.circuit.Circuit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DIDRepository extends JpaRepository<DID, Long> {

    Optional<DID> findByNumber(String number);

    boolean existsByNumber(String number);

    boolean existsByCircuit(Circuit circuit);

    Page<DID> findByNumberContaining(String search, Pageable pageable);

    List<DID> findByCircuitIsNull();

    Page<DID> findByCircuitIsNull(Pageable pageable);

    Page<DID> findByCircuitIsNotNull(Pageable pageable);

    Page<DID> findByCircuitIsNullAndNumberContaining(String number, Pageable pageable);

    Page<DID> findByCircuitIsNotNullAndNumberContaining(String number, Pageable pageable);

    List<DID> findByCircuit_Number(String circuitNumber);

}
