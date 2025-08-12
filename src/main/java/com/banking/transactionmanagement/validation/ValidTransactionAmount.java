package com.banking.transactionmanagement.validation;

import com.banking.transactionmanagement.model.TransactionType;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for transaction amounts based on transaction type.
 * Validates that amounts are appropriate for the transaction type.
 */
@Documented
@Constraint(validatedBy = TransactionAmountValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTransactionAmount {
    
    String message() default "Transaction amount is not valid for the specified transaction type";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}