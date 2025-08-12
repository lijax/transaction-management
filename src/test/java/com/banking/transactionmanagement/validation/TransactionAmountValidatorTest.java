package com.banking.transactionmanagement.validation;

import com.banking.transactionmanagement.dto.TransactionCreateDTO;
import com.banking.transactionmanagement.dto.TransactionUpdateDTO;
import com.banking.transactionmanagement.model.TransactionType;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionAmountValidator.
 */
class TransactionAmountValidatorTest {

    private TransactionAmountValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new TransactionAmountValidator();
        validator.initialize(null);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode("amount")).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void nullObject_shouldReturnTrue() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void createDTOWithNullAmount_shouldReturnTrue() {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType(TransactionType.DEBIT);
        
        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void createDTOWithNullType_shouldReturnTrue() {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setAmount(new BigDecimal("100.00"));
        
        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void updateDTOWithNullAmount_shouldReturnTrue() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setType(TransactionType.WITHDRAWAL);
        
        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void updateDTOWithNullType_shouldReturnTrue() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setAmount(new BigDecimal("15000.00"));
        
        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void debitTransactionWithValidAmount_shouldReturnTrue() {
        TransactionCreateDTO dto = createTransactionCreateDTO(TransactionType.DEBIT, "5000.00");
        
        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void creditTransactionWithValidAmount_shouldReturnTrue() {
        TransactionCreateDTO dto = createTransactionCreateDTO(TransactionType.CREDIT, "15000.00");
        
        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void withdrawalWithValidAmount_shouldReturnTrue() {
        TransactionCreateDTO dto = createTransactionCreateDTO(TransactionType.WITHDRAWAL, "5000.00");
        
        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void withdrawalWithMaxAmount_shouldReturnTrue() {
        TransactionCreateDTO dto = createTransactionCreateDTO(TransactionType.WITHDRAWAL, "10000.00");
        
        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void withdrawalExceedingLimit_shouldReturnFalse() {
        TransactionCreateDTO dto = createTransactionCreateDTO(TransactionType.WITHDRAWAL, "10000.01");
        
        assertFalse(validator.isValid(dto, context));
        
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Withdrawal amount cannot exceed 10000.00");
        verify(violationBuilder).addPropertyNode("amount");
        verify(nodeBuilder).addConstraintViolation();
    }

    @Test
    void transferWithValidAmount_shouldReturnTrue() {
        TransactionCreateDTO dto = createTransactionCreateDTO(TransactionType.TRANSFER, "25000.00");
        
        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void transferWithMaxAmount_shouldReturnTrue() {
        TransactionCreateDTO dto = createTransactionCreateDTO(TransactionType.TRANSFER, "50000.00");
        
        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void transferExceedingLimit_shouldReturnFalse() {
        TransactionCreateDTO dto = createTransactionCreateDTO(TransactionType.TRANSFER, "50000.01");
        
        assertFalse(validator.isValid(dto, context));
        
        verify(context).buildConstraintViolationWithTemplate("Transfer amount cannot exceed 50000.00");
    }

    @Test
    void depositWithValidAmount_shouldReturnTrue() {
        TransactionCreateDTO dto = createTransactionCreateDTO(TransactionType.DEPOSIT, "100.00");
        
        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void depositWithMinAmount_shouldReturnTrue() {
        TransactionCreateDTO dto = createTransactionCreateDTO(TransactionType.DEPOSIT, "1.00");
        
        assertTrue(validator.isValid(dto, context));
    }

    @Test
    void depositBelowMinimum_shouldReturnFalse() {
        TransactionCreateDTO dto = createTransactionCreateDTO(TransactionType.DEPOSIT, "0.99");
        
        assertFalse(validator.isValid(dto, context));
        
        verify(context).buildConstraintViolationWithTemplate("Deposit amount must be at least 1.00");
    }

    @Test
    void updateDTOWithWithdrawalExceedingLimit_shouldReturnFalse() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setType(TransactionType.WITHDRAWAL);
        dto.setAmount(new BigDecimal("15000.00"));
        
        assertFalse(validator.isValid(dto, context));
        
        verify(context).buildConstraintViolationWithTemplate("Withdrawal amount cannot exceed 10000.00");
    }

    @Test
    void updateDTOWithTransferExceedingLimit_shouldReturnFalse() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setType(TransactionType.TRANSFER);
        dto.setAmount(new BigDecimal("60000.00"));
        
        assertFalse(validator.isValid(dto, context));
        
        verify(context).buildConstraintViolationWithTemplate("Transfer amount cannot exceed 50000.00");
    }

    @Test
    void updateDTOWithDepositBelowMinimum_shouldReturnFalse() {
        TransactionUpdateDTO dto = new TransactionUpdateDTO();
        dto.setType(TransactionType.DEPOSIT);
        dto.setAmount(new BigDecimal("0.50"));
        
        assertFalse(validator.isValid(dto, context));
        
        verify(context).buildConstraintViolationWithTemplate("Deposit amount must be at least 1.00");
    }

    private TransactionCreateDTO createTransactionCreateDTO(TransactionType type, String amount) {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setAmount(new BigDecimal(amount));
        dto.setDescription("Test transaction");
        dto.setType(type);
        dto.setTimestamp(LocalDateTime.now());
        return dto;
    }
}