package com.example.catalog.api.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PackUnitValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedPackUnit {
    String message() default "unit must be one of G, KG, ML, L, PCS";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
