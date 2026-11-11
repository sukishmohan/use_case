package com.example.catalog.api.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

public class PackUnitValidator implements ConstraintValidator<AllowedPackUnit, String> {
    private static final Set<String> UNITS = Set.of("G", "KG", "ML", "L", "PCS");
    @Override public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || UNITS.contains(value);
    }
}
