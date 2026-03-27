package com.dionialves.AsteraComm.asterisk.aors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AorRepository extends JpaRepository<Aors, String> {
}
