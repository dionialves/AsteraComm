package com.dionialves.AsteraComm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateDTO(
        @NotBlank @Size(min = 2, max = 100) String name) {
}
