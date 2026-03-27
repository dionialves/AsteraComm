package com.dionialves.AsteraComm.plan.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPackageValidator implements ConstraintValidator<ValidPackage, PackageValidatable> {

    @Override
    public boolean isValid(PackageValidatable dto, ConstraintValidatorContext ctx) {
        if (dto == null || dto.packageType() == null) {
            return true;
        }

        ctx.disableDefaultConstraintViolation();

        return switch (dto.packageType()) {
            case NONE -> validateNone(dto, ctx);
            case UNIFIED -> validateUnified(dto, ctx);
            case PER_CATEGORY -> validatePerCategory(dto, ctx);
        };
    }

    private boolean validateNone(PackageValidatable dto, ConstraintValidatorContext ctx) {
        if (dto.packageTotalMinutes() != null || dto.packageFixedLocal() != null
                || dto.packageFixedLongDistance() != null || dto.packageMobileLocal() != null
                || dto.packageMobileLongDistance() != null) {
            ctx.buildConstraintViolationWithTemplate(
                    "Campos de pacote devem ser nulos quando packageType é NONE")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean validateUnified(PackageValidatable dto, ConstraintValidatorContext ctx) {
        boolean valid = true;

        if (dto.packageTotalMinutes() == null) {
            ctx.buildConstraintViolationWithTemplate(
                    "packageTotalMinutes é obrigatório para UNIFIED")
                    .addPropertyNode("packageTotalMinutes").addConstraintViolation();
            valid = false;
        }

        if (dto.packageFixedLocal() != null || dto.packageFixedLongDistance() != null
                || dto.packageMobileLocal() != null || dto.packageMobileLongDistance() != null) {
            ctx.buildConstraintViolationWithTemplate(
                    "Campos por categoria pertencem ao tipo PER_CATEGORY, não UNIFIED")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }

    private boolean validatePerCategory(PackageValidatable dto, ConstraintValidatorContext ctx) {
        boolean valid = true;

        if (dto.packageTotalMinutes() != null) {
            ctx.buildConstraintViolationWithTemplate(
                    "packageTotalMinutes pertence ao tipo UNIFIED, não PER_CATEGORY")
                    .addPropertyNode("packageTotalMinutes").addConstraintViolation();
            valid = false;
        }

        if (dto.packageFixedLocal() == null) {
            ctx.buildConstraintViolationWithTemplate(
                    "packageFixedLocal é obrigatório para PER_CATEGORY")
                    .addPropertyNode("packageFixedLocal").addConstraintViolation();
            valid = false;
        }

        if (dto.packageFixedLongDistance() == null) {
            ctx.buildConstraintViolationWithTemplate(
                    "packageFixedLongDistance é obrigatório para PER_CATEGORY")
                    .addPropertyNode("packageFixedLongDistance").addConstraintViolation();
            valid = false;
        }

        if (dto.packageMobileLocal() == null) {
            ctx.buildConstraintViolationWithTemplate(
                    "packageMobileLocal é obrigatório para PER_CATEGORY")
                    .addPropertyNode("packageMobileLocal").addConstraintViolation();
            valid = false;
        }

        if (dto.packageMobileLongDistance() == null) {
            ctx.buildConstraintViolationWithTemplate(
                    "packageMobileLongDistance é obrigatório para PER_CATEGORY")
                    .addPropertyNode("packageMobileLongDistance").addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
