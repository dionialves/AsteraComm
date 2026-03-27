package com.dionialves.AsteraComm.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomerCreateDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 5, max = 100, message = "Nome deve ter entre 5 e 100 caracteres")
    String name;

    Boolean enabled;

}
