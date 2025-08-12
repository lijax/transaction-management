package com.banking.transactionmanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;

/**
 * Validator for transaction timestamps.
 * Ensures timestamps are not in the future and not older than 30 days.
 */
public class TransactionTimestampValidator implements ConstraintValidator<ValidTransactionTimestamp, LocalDateTime> {

    private static final int MAX_DAYS_IN_PAST = 30;

    @Override
    public void initialize(ValidTransactionTimestamp constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(LocalDateTime timestamp, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull if required
        if (timestamp == null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(MAX_DAYS_IN_PAST);

        // Check if timestamp is not in the future
        if (timestamp.isAfter(now)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Transaction timestamp cannot be in the future")
                   .addConstraintViolation();
            return false;
        }

        // Check if timestamp is not older than 30 days
        if (timestamp.isBefore(thirtyDaysAgo)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Transaction timestamp cannot be older than " + MAX_DAYS_IN_PAST + " days")
                   .addConstraintViolation();
            return false;
        }

        return true;
    }
}