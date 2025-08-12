package com.banking.transactionmanagement.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;

import java.util.Set;

/**
 * Base class for validation tests providing common validation utilities.
 */
public abstract class ValidationTestBase {

    protected Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Validates an object and returns the constraint violations.
     */
    protected <T> Set<ConstraintViolation<T>> validate(T object) {
        return validator.validate(object);
    }

    /**
     * Checks if a specific field has a validation error with the expected message.
     */
    protected <T> boolean hasFieldError(Set<ConstraintViolation<T>> violations, String fieldName, String expectedMessage) {
        return violations.stream()
                .anyMatch(violation -> 
                    violation.getPropertyPath().toString().equals(fieldName) &&
                    violation.getMessage().equals(expectedMessage));
    }

    /**
     * Checks if a specific field has any validation error.
     */
    protected <T> boolean hasFieldError(Set<ConstraintViolation<T>> violations, String fieldName) {
        return violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(fieldName));
    }

    /**
     * Gets the first violation message for a specific field.
     */
    protected <T> String getFieldErrorMessage(Set<ConstraintViolation<T>> violations, String fieldName) {
        return violations.stream()
                .filter(violation -> violation.getPropertyPath().toString().equals(fieldName))
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(null);
    }
}