package com.dionialves.AsteraComm.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateDTO(
        @NotBlank @Size(min = 5, max = 100) String name,
        @NotBlank @Size(min = 5, max = 50) String username,
        @NotBlank @Size(min = 8, max = 100) String password) {
}
