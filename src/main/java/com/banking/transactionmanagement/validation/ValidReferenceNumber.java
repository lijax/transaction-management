package com.banking.transactionmanagement.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for reference numbers.
 * Validates that reference numbers follow banking standards.
 */
@Documented
@Constraint(validatedBy = ReferenceNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidReferenceNumber {
    
    String message() default "Reference number must be alphanumeric and 6-50 characters long";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}