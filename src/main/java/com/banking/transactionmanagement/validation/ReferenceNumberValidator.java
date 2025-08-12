package com.banking.transactionmanagement.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for reference numbers.
 * Ensures reference numbers are alphanumeric and 6-50 characters long.
 */
public class ReferenceNumberValidator implements ConstraintValidator<ValidReferenceNumber, String> {

    @Override
    public void initialize(ValidReferenceNumber constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String referenceNumber, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull if required
        if (referenceNumber == null || referenceNumber.trim().isEmpty()) {
            return true;
        }
        
        // Check if it's 6-50 characters long and contains only alphanumeric characters
        return referenceNumber.matches("^[a-zA-Z0-9]{6,50}$");
    }
}