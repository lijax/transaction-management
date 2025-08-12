package com.banking.transactionmanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for account numbers.
 * Ensures account numbers are 8-20 digits long and contain only numbers.
 */
public class AccountNumberValidator implements ConstraintValidator<ValidAccountNumber, String> {

    @Override
    public void initialize(ValidAccountNumber constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String accountNumber, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull if required
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return true;
        }
        
        // Remove any spaces or dashes for validation
        String cleanAccountNumber = accountNumber.replaceAll("[\\s-]", "");
        
        // Check if it's 8-20 digits long and contains only numbers
        return cleanAccountNumber.matches("^\\d{8,20}$");
    }
}