package com.dionialves.AsteraComm.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateDTO(
        @NotBlank @Size(min = 8, max = 100) String password) {
}
