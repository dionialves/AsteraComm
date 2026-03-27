package com.dionialves.AsteraComm.plan.dto;

import com.dionialves.AsteraComm.plan.PackageType;
import com.dionialves.AsteraComm.plan.validation.PackageValidatable;
import com.dionialves.AsteraComm.plan.validation.ValidPackage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@ValidPackage
public record PlanCreateDTO(
        @NotBlank String name,
        @NotNull @DecimalMin("0") BigDecimal monthlyPrice,
        @NotNull @DecimalMin("0") BigDecimal fixedLocal,
        @NotNull @DecimalMin("0") BigDecimal fixedLongDistance,
        @NotNull @DecimalMin("0") BigDecimal mobileLocal,
        @NotNull @DecimalMin("0") BigDecimal mobileLongDistance,
        @NotNull PackageType packageType,
        Integer packageTotalMinutes,
        Integer packageFixedLocal,
        Integer packageFixedLongDistance,
        Integer packageMobileLocal,
        Integer packageMobileLongDistance
) implements PackageValidatable {}
