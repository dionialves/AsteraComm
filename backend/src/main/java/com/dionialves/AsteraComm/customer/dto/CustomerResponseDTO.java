package com.dionialves.AsteraComm.customer.dto;

import java.time.LocalDateTime;

public record CustomerResponseDTO(
        Long id,
        String name,
        boolean enabled,
        int circuitCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
