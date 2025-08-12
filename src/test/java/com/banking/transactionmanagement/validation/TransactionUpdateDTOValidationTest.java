package com.banking.transactionmanagement.validation;

import com.banking.transactionmanagement.dto.TransactionUpdateDTO;
import com.banking.transactionmanagement.model.TransactionType;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive validation tests for TransactionUpdateDTO.
 */
class TransactionUpdateDTOValidationTest extends ValidationTestBase {

    @Test
    void emptyTransactionUpdateDTO_shouldPassValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(violations.isEmpty(), "Empty DTO should not have validation errors for partial updates");
    }

    @Test
    void validTransactionUpdateDTO_shouldPassValidation() {
        TransactionUpdateDTO dto = createValidTransactionUpdateDTO();
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(violations.isEmpty(), "Valid DTO should not have validation errors");
    }

    @Test
    void zeroAmount_shouldFailValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setAmount(BigDecimal.ZERO);
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Amount must be greater than 0.01"));
    }

    @Test
    void negativeAmount_shouldFailValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setAmount(new BigDecimal("-10.00"));
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Amount must be greater than 0.01"));
    }

    @Test
    void amountWithTooManyDecimalPlaces_shouldFailValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setAmount(new BigDecimal("100.123"));
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Amount must have at most 10 integer digits and 2 decimal places"));
    }

    @Test
    void validAmount_shouldPassValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setAmount(new BigDecimal("100.50"));
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertFalse(hasFieldError(violations, "amount"));
    }

    @Test
    void emptyDescription_shouldFailValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setDescription("");
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "description", "Description must be between 1 and 255 characters"));
    }

    @Test
    void descriptionTooLong_shouldFailValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setDescription("a".repeat(256)); // 256 characters
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "description", "Description must be between 1 and 255 characters"));
    }

    @Test
    void validDescription_shouldPassValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setDescription("Updated transaction description");
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertFalse(hasFieldError(violations, "description"));
    }

    @Test
    void futureTimestamp_shouldFailValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setTimestamp(LocalDateTime.now().plusDays(1));
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "timestamp", "Transaction timestamp cannot be in the future"));
    }

    @Test
    void timestampTooOld_shouldFailValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setTimestamp(LocalDateTime.now().minusDays(31));
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "timestamp", "Transaction timestamp cannot be older than 30 days"));
    }

    @Test
    void validTimestamp_shouldPassValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setTimestamp(LocalDateTime.now().minusHours(1));
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertFalse(hasFieldError(violations, "timestamp"));
    }

    @Test
    void categoryTooLong_shouldFailValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setCategory("a".repeat(101)); // 101 characters
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "category", "Category must not exceed 100 characters"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Food & Drinks", "Shopping@Mall", "Gas#Station"})
    void categoryWithInvalidCharacters_shouldFailValidation(String invalidCategory) {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setCategory(invalidCategory);
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "category", "Category can only contain letters, numbers, spaces, hyphens, and underscores"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Food", "Shopping Mall", "Gas-Station", "Rent_Payment"})
    void validCategory_shouldPassValidation(String validCategory) {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setCategory(validCategory);
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertFalse(hasFieldError(violations, "category"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "123456789012345678901"}) // 7 digits and 21 digits
    void invalidAccountNumberLength_shouldFailValidation(String invalidAccountNumber) {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setAccountNumber(invalidAccountNumber);
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "accountNumber", "Account number must be 8-20 digits and contain only numbers"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "123456789012345", "12345678901234567890"})
    void validAccountNumber_shouldPassValidation(String validAccountNumber) {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setAccountNumber(validAccountNumber);
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertFalse(hasFieldError(violations, "accountNumber"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "123456789012345678901234567890123456789012345678901"}) // 5 and 51 characters
    void invalidReferenceNumberLength_shouldFailValidation(String invalidReferenceNumber) {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setReferenceNumber(invalidReferenceNumber);
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "referenceNumber", "Reference number must be alphanumeric and 6-50 characters long"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"REF123", "ABC123DEF", "1234567890ABCDEF"})
    void validReferenceNumber_shouldPassValidation(String validReferenceNumber) {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setReferenceNumber(validReferenceNumber);
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertFalse(hasFieldError(violations, "referenceNumber"));
    }

    @Test
    void withdrawalAmountExceedsLimit_shouldFailValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setType(TransactionType.WITHDRAWAL);
        dto.setAmount(new BigDecimal("10000.01"));
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Withdrawal amount cannot exceed 10000.00"));
    }

    @Test
    void transferAmountExceedsLimit_shouldFailValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setType(TransactionType.TRANSFER);
        dto.setAmount(new BigDecimal("50000.01"));
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Transfer amount cannot exceed 50000.00"));
    }

    @Test
    void depositAmountBelowMinimum_shouldFailValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setType(TransactionType.DEPOSIT);
        dto.setAmount(new BigDecimal("0.50"));
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Deposit amount must be at least 1.00"));
    }

    @Test
    void partialUpdateWithOnlyAmount_shouldSkipCrossFieldValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setAmount(new BigDecimal("15000.00")); // This would fail for WITHDRAWAL but type is null
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertFalse(hasFieldError(violations, "amount"));
    }

    @Test
    void partialUpdateWithOnlyType_shouldSkipCrossFieldValidation() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setType(TransactionType.WITHDRAWAL); // This would fail with large amounts but amount is null
        
        Set<ConstraintViolation<TransactionUpdateDTO>> violations = validate(dto);
        
        assertTrue(violations.isEmpty());
    }

    @Test
    void hasUpdates_shouldReturnTrueWhenFieldsAreSet() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        assertFalse(dto.hasUpdates());
        
        dto.setAmount(new BigDecimal("100.00"));
        assertTrue(dto.hasUpdates());
    }

    @Test
    void hasUpdates_shouldReturnFalseWhenNoFieldsAreSet() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        assertFalse(dto.hasUpdates());
    }

    private TransactionUpdateDTO createValidTransactionUpdateDTO() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setAmount(new BigDecimal("150.75"));
        dto.setDescription("Updated transaction");
        dto.setType(TransactionType.CREDIT);
        dto.setTimestamp(LocalDateTime.now().minusHours(2));
        dto.setCategory("Food");
        dto.setAccountNumber("9876543210");
        dto.setReferenceNumber("UPD123456");
        return dto;
    }
}