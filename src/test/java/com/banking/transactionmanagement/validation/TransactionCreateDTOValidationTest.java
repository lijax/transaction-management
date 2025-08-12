package com.banking.transactionmanagement.validation;

import com.banking.transactionmanagement.dto.TransactionCreateDTO;
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
 * Comprehensive validation tests for TransactionCreateDTO.
 */
class TransactionCreateDTOValidationTest extends ValidationTestBase {

    @Test
    void validTransactionCreateDTO_shouldPassValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(violations.isEmpty(), "Valid DTO should not have validation errors");
    }

    @Test
    void nullAmount_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setAmount(null);
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Amount is required"));
    }

    @Test
    void zeroAmount_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setAmount(BigDecimal.ZERO);
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Amount must be greater than 0.01"));
    }

    @Test
    void negativeAmount_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setAmount(new BigDecimal("-10.00"));
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Amount must be greater than 0.01"));
    }

    @Test
    void amountWithTooManyDecimalPlaces_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setAmount(new BigDecimal("100.123"));
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Amount must have at most 10 integer digits and 2 decimal places"));
    }

    @Test
    void amountWithTooManyIntegerDigits_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setAmount(new BigDecimal("12345678901.00")); // 11 integer digits
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Amount must have at most 10 integer digits and 2 decimal places"));
    }

    @Test
    void nullDescription_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setDescription(null);
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "description", "Description is required"));
    }

    @Test
    void emptyDescription_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setDescription("");
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "description", "Description is required"));
    }

    @Test
    void blankDescription_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setDescription("   ");
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "description", "Description is required"));
    }

    @Test
    void descriptionTooLong_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setDescription("a".repeat(256)); // 256 characters
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "description", "Description must not exceed 255 characters"));
    }

    @Test
    void nullTransactionType_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setType(null);
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "type", "Transaction type is required"));
    }

    @Test
    void nullTimestamp_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setTimestamp(null);
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "timestamp", "Timestamp is required"));
    }

    @Test
    void futureTimestamp_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setTimestamp(LocalDateTime.now().plusDays(1));
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "timestamp", "Transaction timestamp cannot be in the future"));
    }

    @Test
    void timestampTooOld_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setTimestamp(LocalDateTime.now().minusDays(31));
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "timestamp", "Transaction timestamp cannot be older than 30 days"));
    }

    @Test
    void categoryTooLong_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setCategory("a".repeat(101)); // 101 characters
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "category", "Category must not exceed 100 characters"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Food & Drinks", "Shopping@Mall", "Gas#Station", "Rent$Payment"})
    void categoryWithInvalidCharacters_shouldFailValidation(String invalidCategory) {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setCategory(invalidCategory);
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "category", "Category can only contain letters, numbers, spaces, hyphens, and underscores"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Food", "Shopping Mall", "Gas-Station", "Rent_Payment", "Category123"})
    void categoryWithValidCharacters_shouldPassValidation(String validCategory) {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setCategory(validCategory);
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertFalse(hasFieldError(violations, "category"));
    }

    @Test
    void accountNumberTooLong_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setAccountNumber("a".repeat(51)); // 51 characters
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "account", "Account number must not exceed 50 characters") ||
                  hasFieldError(violations, "accountNumber", "Account number must not exceed 50 characters"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "123456789012345678901"}) // 7 digits and 21 digits
    void invalidAccountNumberLength_shouldFailValidation(String invalidAccountNumber) {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setAccountNumber(invalidAccountNumber);
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "accountNumber", "Account number must be 8-20 digits and contain only numbers"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345abc", "abc12345"})
    void accountNumberWithInvalidCharacters_shouldFailValidation(String invalidAccountNumber) {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setAccountNumber(invalidAccountNumber);
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "accountNumber", "Account number must be 8-20 digits and contain only numbers"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "123456789012345", "12345678901234567890", "1234-5678", "1234 5678"}) // 8, 15, and 20 digits, plus formatted numbers
    void validAccountNumbers_shouldPassValidation(String validAccountNumber) {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setAccountNumber(validAccountNumber);
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertFalse(hasFieldError(violations, "accountNumber"));
    }

    @Test
    void referenceNumberTooLong_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setReferenceNumber("a".repeat(101)); // 101 characters
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "reference", "Reference number must not exceed 100 characters") ||
                  hasFieldError(violations, "referenceNumber", "Reference number must not exceed 100 characters"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "123456789012345678901234567890123456789012345678901"}) // 5 and 51 characters
    void invalidReferenceNumberLength_shouldFailValidation(String invalidReferenceNumber) {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setReferenceNumber(invalidReferenceNumber);
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "referenceNumber", "Reference number must be alphanumeric and 6-50 characters long"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"REF-123", "REF@123", "REF 123"})
    void referenceNumberWithInvalidCharacters_shouldFailValidation(String invalidReferenceNumber) {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setReferenceNumber(invalidReferenceNumber);
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "referenceNumber", "Reference number must be alphanumeric and 6-50 characters long"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"REF123", "ABC123DEF", "1234567890ABCDEF"})
    void validReferenceNumbers_shouldPassValidation(String validReferenceNumber) {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setReferenceNumber(validReferenceNumber);
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertFalse(hasFieldError(violations, "referenceNumber"));
    }

    @Test
    void withdrawalAmountExceedsLimit_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setType(TransactionType.WITHDRAWAL);
        dto.setAmount(new BigDecimal("10000.01"));
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Withdrawal amount cannot exceed 10000.00"));
    }

    @Test
    void transferAmountExceedsLimit_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setType(TransactionType.TRANSFER);
        dto.setAmount(new BigDecimal("50000.01"));
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Transfer amount cannot exceed 50000.00"));
    }

    @Test
    void depositAmountBelowMinimum_shouldFailValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setType(TransactionType.DEPOSIT);
        dto.setAmount(new BigDecimal("0.50"));
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertTrue(hasFieldError(violations, "amount", "Deposit amount must be at least 1.00"));
    }

    @Test
    void validWithdrawalAmount_shouldPassValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setType(TransactionType.WITHDRAWAL);
        dto.setAmount(new BigDecimal("5000.00"));
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertFalse(hasFieldError(violations, "amount"));
    }

    @Test
    void validTransferAmount_shouldPassValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setType(TransactionType.TRANSFER);
        dto.setAmount(new BigDecimal("25000.00"));
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertFalse(hasFieldError(violations, "amount"));
    }

    @Test
    void validDepositAmount_shouldPassValidation() {
        TransactionCreateDTO dto = createValidTransactionCreateDTO();
        dto.setType(TransactionType.DEPOSIT);
        dto.setAmount(new BigDecimal("100.00"));
        
        Set<ConstraintViolation<TransactionCreateDTO>> violations = validate(dto);
        
        assertFalse(hasFieldError(violations, "amount"));
    }

    private TransactionCreateDTO createValidTransactionCreateDTO() {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setAmount(new BigDecimal("100.50"));
        dto.setDescription("Test transaction");
        dto.setType(TransactionType.DEBIT);
        dto.setTimestamp(LocalDateTime.now().minusHours(1));
        dto.setCategory("Shopping");
        dto.setAccountNumber("1234567890");
        dto.setReferenceNumber("REF123456");
        return dto;
    }
}