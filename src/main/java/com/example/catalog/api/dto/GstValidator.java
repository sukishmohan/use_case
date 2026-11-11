package com.example.catalog.api.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

public class GstValidator implements ConstraintValidator<AllowedGst, Integer> {
    private static final Set<Integer> RATES = Set.of(0, 5, 12, 18, 28);
    @Override public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value == null || RATES.contains(value);
    }
}
