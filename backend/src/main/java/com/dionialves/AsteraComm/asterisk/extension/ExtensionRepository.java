package com.dionialves.AsteraComm.asterisk.extension;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtensionRepository extends JpaRepository<Extension, Long> {
    void deleteByExten(String exten);
    void deleteByExtenAndContext(String exten, String context);
}
