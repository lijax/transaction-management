package com.banking.transactionmanagement.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for transaction timestamps.
 * Validates that timestamps are not in the future and not too far in the past.
 */
@Documented
@Constraint(validatedBy = TransactionTimestampValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTransactionTimestamp {
    
    String message() default "Transaction timestamp must not be in the future and not older than 30 days";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}