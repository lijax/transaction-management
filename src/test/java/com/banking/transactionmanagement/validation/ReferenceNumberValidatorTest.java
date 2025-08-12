package com.banking.transactionmanagement.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReferenceNumberValidator.
 */
class ReferenceNumberValidatorTest {

    private ReferenceNumberValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ReferenceNumberValidator();
        validator.initialize(null);
    }

    @Test
    void nullReferenceNumber_shouldReturnTrue() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void emptyReferenceNumber_shouldReturnTrue() {
        assertTrue(validator.isValid("", null));
    }

    @Test
    void blankReferenceNumber_shouldReturnTrue() {
        assertTrue(validator.isValid("   ", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"REF123", "ABC123DEF", "1234567890ABCDEF", "123456", "12345678901234567890123456789012345678901234567890"}) // 6 and 50 characters
    void validReferenceNumbers_shouldReturnTrue(String referenceNumber) {
        assertTrue(validator.isValid(referenceNumber, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"REF12", "123456789012345678901234567890123456789012345678901"}) // 5 and 51 characters
    void referenceNumbersWithInvalidLength_shouldReturnFalse(String referenceNumber) {
        assertFalse(validator.isValid(referenceNumber, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"REF-123", "REF@123", "REF 123", "REF#123", "REF$123"})
    void referenceNumbersWithInvalidCharacters_shouldReturnFalse(String referenceNumber) {
        assertFalse(validator.isValid(referenceNumber, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC123", "123ABC", "ABCDEF", "123456"})
    void referenceNumbersWithOnlyAlphanumeric_shouldReturnTrue(String referenceNumber) {
        assertTrue(validator.isValid(referenceNumber, null));
    }
}