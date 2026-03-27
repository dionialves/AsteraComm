package com.dionialves.AsteraComm.user.dto;

import jakarta.validation.constraints.Size;

public record UserUpdateDTO(
        @Size(min = 5, max = 100) String name,
        Boolean enabled) {
}
