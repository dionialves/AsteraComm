package com.dionialves.AsteraComm.trunk.dto;

import com.dionialves.AsteraComm.trunk.TrunkAuthType;

public record TrunkCreateDTO(
        String name,
        String host,
        String username,
        String password,
        String prefix,
        TrunkAuthType authType,
        String identifyMatch
) {}
