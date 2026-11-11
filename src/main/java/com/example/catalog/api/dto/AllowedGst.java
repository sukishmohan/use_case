package com.example.catalog.api.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = GstValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedGst {
    String message() default "gstPercent must be one of 0, 5, 12, 18, 28";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
