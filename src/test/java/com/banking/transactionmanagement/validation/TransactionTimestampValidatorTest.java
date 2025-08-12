package com.banking.transactionmanagement.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionTimestampValidator.
 */
class TransactionTimestampValidatorTest {

    private TransactionTimestampValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new TransactionTimestampValidator();
        validator.initialize(null);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void nullTimestamp_shouldReturnTrue() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void currentTimestamp_shouldReturnTrue() {
        LocalDateTime now = LocalDateTime.now();
        assertTrue(validator.isValid(now, context));
    }

    @Test
    void pastTimestampWithinLimit_shouldReturnTrue() {
        LocalDateTime timestamp = LocalDateTime.now().minusDays(15);
        assertTrue(validator.isValid(timestamp, context));
    }

    @Test
    void timestampAtExactLimit_shouldReturnTrue() {
        LocalDateTime timestamp = LocalDateTime.now().minusDays(30).plusMinutes(1);
        assertTrue(validator.isValid(timestamp, context));
    }

    @Test
    void futureTimestamp_shouldReturnFalse() {
        LocalDateTime futureTimestamp = LocalDateTime.now().plusMinutes(1);
        
        assertFalse(validator.isValid(futureTimestamp, context));
        
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Transaction timestamp cannot be in the future");
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void timestampTooOld_shouldReturnFalse() {
        LocalDateTime oldTimestamp = LocalDateTime.now().minusDays(31);
        
        assertFalse(validator.isValid(oldTimestamp, context));
        
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Transaction timestamp cannot be older than 30 days");
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    void timestampOneSecondInFuture_shouldReturnFalse() {
        LocalDateTime futureTimestamp = LocalDateTime.now().plusSeconds(1);
        
        assertFalse(validator.isValid(futureTimestamp, context));
        
        verify(context).buildConstraintViolationWithTemplate("Transaction timestamp cannot be in the future");
    }

    @Test
    void timestampOneSecondTooOld_shouldReturnFalse() {
        LocalDateTime oldTimestamp = LocalDateTime.now().minusDays(30).minusSeconds(1);
        
        assertFalse(validator.isValid(oldTimestamp, context));
        
        verify(context).buildConstraintViolationWithTemplate("Transaction timestamp cannot be older than 30 days");
    }
}