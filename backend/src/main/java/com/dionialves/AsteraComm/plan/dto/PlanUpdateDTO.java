package com.dionialves.AsteraComm.plan.dto;

import com.dionialves.AsteraComm.plan.PackageType;
import com.dionialves.AsteraComm.plan.validation.PackageValidatable;
import com.dionialves.AsteraComm.plan.validation.ValidPackage;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

@ValidPackage
public record PlanUpdateDTO(
        String name,
        @DecimalMin("0") BigDecimal monthlyPrice,
        @DecimalMin("0") BigDecimal fixedLocal,
        @DecimalMin("0") BigDecimal fixedLongDistance,
        @DecimalMin("0") BigDecimal mobileLocal,
        @DecimalMin("0") BigDecimal mobileLongDistance,
        PackageType packageType,
        Integer packageTotalMinutes,
        Integer packageFixedLocal,
        Integer packageFixedLongDistance,
        Integer packageMobileLocal,
        Integer packageMobileLongDistance,
        Boolean active
) implements PackageValidatable {}
