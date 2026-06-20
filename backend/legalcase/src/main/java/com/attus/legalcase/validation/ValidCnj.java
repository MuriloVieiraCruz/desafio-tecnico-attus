package com.attus.legalcase.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CnjValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCnj {
    String message() default "invalid CNJ number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
