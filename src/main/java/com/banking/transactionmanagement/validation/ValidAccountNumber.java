package com.banking.transactionmanagement.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for account numbers.
 * Validates that account numbers follow banking standards.
 */
@Documented
@Constraint(validatedBy = AccountNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAccountNumber {
    
    String message() default "Account number must be 8-20 digits and contain only numbers";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}