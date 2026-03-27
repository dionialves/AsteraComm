package com.dionialves.AsteraComm.trunk;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TrunkRegistrationStatusRepository extends JpaRepository<TrunkRegistrationStatus, Long> {

    void deleteByTrunkName(String trunkName);
}
