package com.dionialves.AsteraComm.plan.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPackageValidator.class)
@Documented
public @interface ValidPackage {
    String message() default "Configuração de pacote inválida para o tipo informado";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
