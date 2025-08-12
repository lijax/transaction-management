package com.banking.transactionmanagement.exception;

import com.banking.transactionmanagement.dto.ErrorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private WebRequest webRequest;

    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/transactions");
    }

    @Test
    void handleValidationException_ShouldReturnBadRequest() {
        // Given
        ValidationException exception = new ValidationException("Invalid transaction data");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler
            .handleValidationException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponseDTO errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Bad Request", errorResponse.getError());
        assertEquals("Invalid transaction data", errorResponse.getMessage());
        assertEquals("/api/v1/transactions", errorResponse.getPath());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void handleTransactionNotFoundException_ShouldReturnNotFound() {
        // Given
        TransactionNotFoundException exception = new TransactionNotFoundException(123L);

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler
            .handleTransactionNotFoundException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponseDTO errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(404, errorResponse.getStatus());
        assertEquals("Not Found", errorResponse.getError());
        assertEquals("Transaction not found with ID: 123", errorResponse.getMessage());
        assertEquals("/api/v1/transactions", errorResponse.getPath());
    }

    @Test
    void handleDuplicateTransactionException_ShouldReturnConflict() {
        // Given
        DuplicateTransactionException exception = new DuplicateTransactionException("Duplicate transaction detected");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler
            .handleDuplicateTransactionException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ErrorResponseDTO errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(409, errorResponse.getStatus());
        assertEquals("Conflict", errorResponse.getError());
        assertEquals("Duplicate transaction detected", errorResponse.getMessage());
        assertEquals("/api/v1/transactions", errorResponse.getPath());
    }

    @Test
    void handleMethodArgumentNotValidException_ShouldReturnBadRequestWithDetails() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        FieldError fieldError1 = new FieldError("transaction", "amount", "Amount must be positive");
        FieldError fieldError2 = new FieldError("transaction", "description", "Description cannot be blank");
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler
            .handleMethodArgumentNotValidException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponseDTO errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Bad Request", errorResponse.getError());
        assertEquals("Validation failed", errorResponse.getMessage());
        
        List<ErrorResponseDTO.ValidationErrorDetail> details = errorResponse.getDetails();
        assertNotNull(details);
        assertEquals(2, details.size());
        
        assertEquals("amount", details.get(0).getField());
        assertEquals("Amount must be positive", details.get(0).getMessage());
        assertEquals("description", details.get(1).getField());
        assertEquals("Description cannot be blank", details.get(1).getMessage());
    }

    @Test
    void handleConstraintViolationException_ShouldReturnBadRequestWithDetails() {
        // Given
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
        Path path1 = mock(Path.class);
        Path path2 = mock(Path.class);
        
        when(violation1.getPropertyPath()).thenReturn(path1);
        when(path1.toString()).thenReturn("amount");
        when(violation1.getMessage()).thenReturn("must be greater than 0");
        
        when(violation2.getPropertyPath()).thenReturn(path2);
        when(path2.toString()).thenReturn("id");
        when(violation2.getMessage()).thenReturn("must not be null");
        
        ConstraintViolationException exception = new ConstraintViolationException(
            "Validation failed", Set.of(violation1, violation2));

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler
            .handleConstraintViolationException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponseDTO errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Validation failed", errorResponse.getMessage());
        
        List<ErrorResponseDTO.ValidationErrorDetail> details = errorResponse.getDetails();
        assertNotNull(details);
        assertEquals(2, details.size());
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequest() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument provided");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler
            .handleIllegalArgumentException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponseDTO errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Bad Request", errorResponse.getError());
        assertEquals("Invalid argument provided", errorResponse.getMessage());
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler
            .handleGenericException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponseDTO errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(500, errorResponse.getStatus());
        assertEquals("Internal Server Error", errorResponse.getError());
        assertEquals("An unexpected error occurred. Please try again later.", errorResponse.getMessage());
    }
}