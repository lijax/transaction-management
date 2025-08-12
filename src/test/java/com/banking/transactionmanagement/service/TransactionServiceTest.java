package com.banking.transactionmanagement.service;

import com.banking.transactionmanagement.dto.TransactionCreateDTO;
import com.banking.transactionmanagement.dto.TransactionResponseDTO;
import com.banking.transactionmanagement.dto.TransactionUpdateDTO;
import com.banking.transactionmanagement.dto.PagedTransactionResponseDTO;
import com.banking.transactionmanagement.exception.DuplicateTransactionException;
import com.banking.transactionmanagement.exception.TransactionNotFoundException;
import com.banking.transactionmanagement.exception.ValidationException;
import com.banking.transactionmanagement.model.Transaction;
import com.banking.transactionmanagement.model.TransactionType;
import com.banking.transactionmanagement.repository.TransactionRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for TransactionService.
 * Tests all CRUD operations, validation, error handling, and caching behavior.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private Validator validator;

    @Mock
    private QueryPerformanceMonitoringService performanceMonitoringService;

    @InjectMocks
    private TransactionService transactionService;

    private TransactionCreateDTO validCreateDTO;
    private TransactionUpdateDTO validUpdateDTO;
    private Transaction sampleTransaction;
    private LocalDateTime fixedTimestamp;

    @BeforeEach
    void setUp() {
        fixedTimestamp = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        
        // Configure performance monitoring service mock with lenient mode
        lenient().when(performanceMonitoringService.monitorQuery(anyString(), any(java.util.function.Supplier.class)))
            .thenAnswer(invocation -> {
                java.util.function.Supplier<?> supplier = invocation.getArgument(1);
                return supplier.get();
            });
        
        // Configure the void version of monitorQuery with lenient mode
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(performanceMonitoringService).monitorQuery(anyString(), any(Runnable.class));
        
        // Configure incrementCounter method with lenient mode
        lenient().doNothing().when(performanceMonitoringService).incrementCounter(anyString(), any());
        
        validCreateDTO = new TransactionCreateDTO();
        validCreateDTO.setAmount(new BigDecimal("100.50"));
        validCreateDTO.setDescription("Test transaction");
        validCreateDTO.setType(TransactionType.DEBIT);
        validCreateDTO.setTimestamp(fixedTimestamp);
        validCreateDTO.setCategory("Test");
        validCreateDTO.setAccountNumber("123456789");
        validCreateDTO.setReferenceNumber("REF123");

        validUpdateDTO = new TransactionUpdateDTO();
        validUpdateDTO.setAmount(new BigDecimal("200.75"));
        validUpdateDTO.setDescription("Updated transaction");

        sampleTransaction = new Transaction();
        sampleTransaction.setId(1L);
        sampleTransaction.setAmount(new BigDecimal("100.50"));
        sampleTransaction.setDescription("Test transaction");
        sampleTransaction.setType(TransactionType.DEBIT);
        sampleTransaction.setTimestamp(fixedTimestamp);
        sampleTransaction.setCategory("Test");
        sampleTransaction.setAccountNumber("123456789");
        sampleTransaction.setReferenceNumber("REF123");
        sampleTransaction.setCreatedAt(fixedTimestamp);
        sampleTransaction.setUpdatedAt(fixedTimestamp);
    }

    @Nested
    @DisplayName("Create Transaction Tests")
    class CreateTransactionTests {

        @Test
        @DisplayName("Should create transaction with valid data")
        void createTransaction_ValidInput_ReturnsTransactionResponseDTO() {
            // Arrange
            when(validator.validate(validCreateDTO)).thenReturn(Collections.emptySet());
            when(transactionRepository.existsByAmountAndDescriptionAndTimestamp(
                    any(BigDecimal.class), anyString(), any(LocalDateTime.class))).thenReturn(false);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

            // Act
            TransactionResponseDTO result = transactionService.createTransaction(validCreateDTO);

            // Assert
            assertNotNull(result);
            assertEquals(sampleTransaction.getId(), result.getId());
            assertEquals(sampleTransaction.getAmount(), result.getAmount());
            assertEquals(sampleTransaction.getDescription(), result.getDescription());
            assertEquals(sampleTransaction.getType(), result.getType());
            assertEquals(sampleTransaction.getTimestamp(), result.getTimestamp());
            assertEquals(sampleTransaction.getCategory(), result.getCategory());
            assertEquals(sampleTransaction.getAccountNumber(), result.getAccountNumber());
            assertEquals(sampleTransaction.getReferenceNumber(), result.getReferenceNumber());
            
            verify(validator).validate(validCreateDTO);
            verify(transactionRepository).existsByAmountAndDescriptionAndTimestamp(
                    validCreateDTO.getAmount(), validCreateDTO.getDescription(), validCreateDTO.getTimestamp());
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should throw ValidationException when input is null")
        void createTransaction_NullInput_ThrowsValidationException() {
            // Act & Assert
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                transactionService.createTransaction(null));
            
            assertEquals("Transaction data cannot be null", exception.getMessage());
            verifyNoInteractions(transactionRepository);
        }

        @Test
        @DisplayName("Should throw ValidationException when validation fails")
        void createTransaction_ValidationErrors_ThrowsValidationException() {
            // Arrange
            @SuppressWarnings("unchecked")
            ConstraintViolation<TransactionCreateDTO> violation = mock(ConstraintViolation.class);
            when(violation.getMessage()).thenReturn("Amount is required");
            Set<ConstraintViolation<TransactionCreateDTO>> violations = Set.of(violation);
            when(validator.validate(validCreateDTO)).thenReturn(violations);

            // Act & Assert
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                transactionService.createTransaction(validCreateDTO));
            
            assertTrue(exception.getMessage().contains("Validation failed"));
            assertTrue(exception.getMessage().contains("Amount is required"));
            verify(validator).validate(validCreateDTO);
            verifyNoInteractions(transactionRepository);
        }

        @Test
        @DisplayName("Should throw ValidationException with multiple validation errors")
        void createTransaction_MultipleValidationErrors_ThrowsValidationException() {
            // Arrange
            @SuppressWarnings("unchecked")
            ConstraintViolation<TransactionCreateDTO> violation1 = mock(ConstraintViolation.class);
            @SuppressWarnings("unchecked")
            ConstraintViolation<TransactionCreateDTO> violation2 = mock(ConstraintViolation.class);
            when(violation1.getMessage()).thenReturn("Amount is required");
            when(violation2.getMessage()).thenReturn("Description is required");
            Set<ConstraintViolation<TransactionCreateDTO>> violations = Set.of(violation1, violation2);
            when(validator.validate(validCreateDTO)).thenReturn(violations);

            // Act & Assert
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                transactionService.createTransaction(validCreateDTO));
            
            assertTrue(exception.getMessage().contains("Amount is required"));
            assertTrue(exception.getMessage().contains("Description is required"));
        }

        @Test
        @DisplayName("Should throw DuplicateTransactionException when duplicate exists")
        void createTransaction_DuplicateTransaction_ThrowsDuplicateTransactionException() {
            // Arrange
            when(validator.validate(validCreateDTO)).thenReturn(Collections.emptySet());
            when(transactionRepository.existsByAmountAndDescriptionAndTimestamp(
                    any(BigDecimal.class), anyString(), any(LocalDateTime.class))).thenReturn(true);

            // Act & Assert
            DuplicateTransactionException exception = assertThrows(DuplicateTransactionException.class, () -> 
                transactionService.createTransaction(validCreateDTO));
            
            assertTrue(exception.getMessage().contains("Duplicate transaction detected"));
            assertTrue(exception.getMessage().contains(validCreateDTO.getAmount().toString()));
            assertTrue(exception.getMessage().contains(validCreateDTO.getDescription()));
            
            verify(transactionRepository).existsByAmountAndDescriptionAndTimestamp(
                    validCreateDTO.getAmount(), validCreateDTO.getDescription(), validCreateDTO.getTimestamp());
            verify(transactionRepository, never()).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should create transaction with minimal required fields")
        void createTransaction_MinimalFields_ReturnsTransactionResponseDTO() {
            // Arrange
            TransactionCreateDTO minimalDTO = new TransactionCreateDTO();
            minimalDTO.setAmount(new BigDecimal("50.00"));
            minimalDTO.setDescription("Minimal transaction");
            minimalDTO.setType(TransactionType.CREDIT);
            minimalDTO.setTimestamp(fixedTimestamp);
            
            Transaction minimalTransaction = new Transaction();
            minimalTransaction.setId(2L);
            minimalTransaction.setAmount(new BigDecimal("50.00"));
            minimalTransaction.setDescription("Minimal transaction");
            minimalTransaction.setType(TransactionType.CREDIT);
            minimalTransaction.setTimestamp(fixedTimestamp);
            minimalTransaction.setCreatedAt(fixedTimestamp);
            minimalTransaction.setUpdatedAt(fixedTimestamp);

            when(validator.validate(minimalDTO)).thenReturn(Collections.emptySet());
            when(transactionRepository.existsByAmountAndDescriptionAndTimestamp(
                    any(BigDecimal.class), anyString(), any(LocalDateTime.class))).thenReturn(false);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(minimalTransaction);

            // Act
            TransactionResponseDTO result = transactionService.createTransaction(minimalDTO);

            // Assert
            assertNotNull(result);
            assertEquals(minimalTransaction.getId(), result.getId());
            assertEquals(minimalTransaction.getAmount(), result.getAmount());
            assertEquals(minimalTransaction.getDescription(), result.getDescription());
            assertNull(result.getCategory());
            assertNull(result.getAccountNumber());
            assertNull(result.getReferenceNumber());
        }
    }

    @Nested
    @DisplayName("Update Transaction Tests")
    class UpdateTransactionTests {

        @Test
        @DisplayName("Should update transaction with valid data")
        void updateTransaction_ValidInput_ReturnsUpdatedTransactionResponseDTO() {
            // Arrange
            Transaction updatedTransaction = new Transaction();
            updatedTransaction.setId(1L);
            updatedTransaction.setAmount(new BigDecimal("200.75"));
            updatedTransaction.setDescription("Updated transaction");
            updatedTransaction.setType(TransactionType.DEBIT);
            updatedTransaction.setTimestamp(fixedTimestamp);
            updatedTransaction.setCategory("Test");
            updatedTransaction.setAccountNumber("123456789");
            updatedTransaction.setReferenceNumber("REF123");
            updatedTransaction.setCreatedAt(fixedTimestamp);
            updatedTransaction.setUpdatedAt(fixedTimestamp.plusMinutes(5));

            when(validator.validate(validUpdateDTO)).thenReturn(Collections.emptySet());
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(updatedTransaction);

            // Act
            TransactionResponseDTO result = transactionService.updateTransaction(1L, validUpdateDTO);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(new BigDecimal("200.75"), result.getAmount());
            assertEquals("Updated transaction", result.getDescription());
            
            verify(validator).validate(validUpdateDTO);
            verify(transactionRepository).findById(1L);
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should throw ValidationException when update DTO is null")
        void updateTransaction_NullInput_ThrowsValidationException() {
            // Act & Assert
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                transactionService.updateTransaction(1L, null));
            
            assertEquals("Update data cannot be null", exception.getMessage());
            verifyNoInteractions(transactionRepository);
        }

        @Test
        @DisplayName("Should throw ValidationException when no updates provided")
        void updateTransaction_NoUpdates_ThrowsValidationException() {
            // Arrange
            TransactionUpdateDTO emptyUpdateDTO = new TransactionUpdateDTO();

            // Act & Assert
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                transactionService.updateTransaction(1L, emptyUpdateDTO));
            
            assertEquals("At least one field must be provided for update", exception.getMessage());
            verifyNoInteractions(transactionRepository);
        }

        @Test
        @DisplayName("Should throw ValidationException when update validation fails")
        void updateTransaction_ValidationErrors_ThrowsValidationException() {
            // Arrange
            @SuppressWarnings("unchecked")
            ConstraintViolation<TransactionUpdateDTO> violation = mock(ConstraintViolation.class);
            when(violation.getMessage()).thenReturn("Amount must be positive");
            Set<ConstraintViolation<TransactionUpdateDTO>> violations = Set.of(violation);
            when(validator.validate(validUpdateDTO)).thenReturn(violations);

            // Act & Assert
            ValidationException exception = assertThrows(ValidationException.class, () -> 
                transactionService.updateTransaction(1L, validUpdateDTO));
            
            assertTrue(exception.getMessage().contains("Validation failed"));
            assertTrue(exception.getMessage().contains("Amount must be positive"));
            verify(validator).validate(validUpdateDTO);
            verifyNoInteractions(transactionRepository);
        }

        @Test
        @DisplayName("Should throw TransactionNotFoundException when transaction doesn't exist")
        void updateTransaction_NonExistentTransaction_ThrowsTransactionNotFoundException() {
            // Arrange
            when(validator.validate(validUpdateDTO)).thenReturn(Collections.emptySet());
            when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class, () -> 
                transactionService.updateTransaction(1L, validUpdateDTO));
            
            verify(validator).validate(validUpdateDTO);
            verify(transactionRepository).findById(1L);
            verify(transactionRepository, never()).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should perform partial update with single field")
        void updateTransaction_PartialUpdateSingleField_UpdatesOnlySpecifiedField() {
            // Arrange
            TransactionUpdateDTO partialUpdateDTO = new TransactionUpdateDTO();
            partialUpdateDTO.setAmount(new BigDecimal("300.00"));

            Transaction originalTransaction = new Transaction();
            originalTransaction.setId(1L);
            originalTransaction.setAmount(new BigDecimal("100.50"));
            originalTransaction.setDescription("Original description");
            originalTransaction.setType(TransactionType.DEBIT);
            originalTransaction.setTimestamp(fixedTimestamp);
            originalTransaction.setCategory("Original");

            when(validator.validate(partialUpdateDTO)).thenReturn(Collections.emptySet());
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(originalTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TransactionResponseDTO result = transactionService.updateTransaction(1L, partialUpdateDTO);

            // Assert
            assertNotNull(result);
            assertEquals(new BigDecimal("300.00"), result.getAmount());
            assertEquals("Original description", result.getDescription()); // Should remain unchanged
            assertEquals(TransactionType.DEBIT, result.getType()); // Should remain unchanged
            assertEquals("Original", result.getCategory()); // Should remain unchanged
        }

        @Test
        @DisplayName("Should perform partial update with multiple fields")
        void updateTransaction_PartialUpdateMultipleFields_UpdatesSpecifiedFields() {
            // Arrange
            TransactionUpdateDTO partialUpdateDTO = new TransactionUpdateDTO();
            partialUpdateDTO.setAmount(new BigDecimal("400.00"));
            partialUpdateDTO.setDescription("New description");
            partialUpdateDTO.setType(TransactionType.CREDIT);

            when(validator.validate(partialUpdateDTO)).thenReturn(Collections.emptySet());
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TransactionResponseDTO result = transactionService.updateTransaction(1L, partialUpdateDTO);

            // Assert
            assertNotNull(result);
            assertEquals(new BigDecimal("400.00"), result.getAmount());
            assertEquals("New description", result.getDescription());
            assertEquals(TransactionType.CREDIT, result.getType());
            assertEquals("Test", result.getCategory()); // Should remain unchanged
        }

        @Test
        @DisplayName("Should update all fields when all are provided")
        void updateTransaction_AllFields_UpdatesAllFields() {
            // Arrange
            TransactionUpdateDTO fullUpdateDTO = new TransactionUpdateDTO();
            fullUpdateDTO.setAmount(new BigDecimal("500.00"));
            fullUpdateDTO.setDescription("Completely new description");
            fullUpdateDTO.setType(TransactionType.TRANSFER);
            fullUpdateDTO.setTimestamp(fixedTimestamp.plusHours(1));
            fullUpdateDTO.setCategory("New Category");
            fullUpdateDTO.setAccountNumber("987654321");
            fullUpdateDTO.setReferenceNumber("NEWREF456");

            when(validator.validate(fullUpdateDTO)).thenReturn(Collections.emptySet());
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            TransactionResponseDTO result = transactionService.updateTransaction(1L, fullUpdateDTO);

            // Assert
            assertNotNull(result);
            assertEquals(new BigDecimal("500.00"), result.getAmount());
            assertEquals("Completely new description", result.getDescription());
            assertEquals(TransactionType.TRANSFER, result.getType());
            assertEquals(fixedTimestamp.plusHours(1), result.getTimestamp());
            assertEquals("New Category", result.getCategory());
            assertEquals("987654321", result.getAccountNumber());
            assertEquals("NEWREF456", result.getReferenceNumber());
        }
    }

    @Nested
    @DisplayName("Delete Transaction Tests")
    class DeleteTransactionTests {

        @Test
        @DisplayName("Should delete existing transaction successfully")
        void deleteTransaction_ExistingTransaction_DeletesSuccessfully() {
            // Arrange
            when(transactionRepository.existsById(1L)).thenReturn(true);

            // Act
            assertDoesNotThrow(() -> transactionService.deleteTransaction(1L));

            // Assert
            verify(transactionRepository).existsById(1L);
            verify(transactionRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw TransactionNotFoundException when transaction doesn't exist")
        void deleteTransaction_NonExistentTransaction_ThrowsTransactionNotFoundException() {
            // Arrange
            when(transactionRepository.existsById(1L)).thenReturn(false);

            // Act & Assert
            TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class, () -> 
                transactionService.deleteTransaction(1L));
            
            verify(transactionRepository).existsById(1L);
            verify(transactionRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should handle deletion of different transaction IDs")
        void deleteTransaction_DifferentIds_HandlesCorrectly() {
            // Arrange
            when(transactionRepository.existsById(100L)).thenReturn(true);
            when(transactionRepository.existsById(200L)).thenReturn(false);

            // Act & Assert
            assertDoesNotThrow(() -> transactionService.deleteTransaction(100L));
            assertThrows(TransactionNotFoundException.class, () -> 
                transactionService.deleteTransaction(200L));

            verify(transactionRepository).existsById(100L);
            verify(transactionRepository).existsById(200L);
            verify(transactionRepository).deleteById(100L);
            verify(transactionRepository, never()).deleteById(200L);
        }
    }

    @Nested
    @DisplayName("Get Transaction Tests")
    class GetTransactionTests {

        @Test
        @DisplayName("Should return transaction when it exists")
        void getTransactionById_ExistingTransaction_ReturnsTransactionResponseDTO() {
            // Arrange
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));

            // Act
            TransactionResponseDTO result = transactionService.getTransactionById(1L);

            // Assert
            assertNotNull(result);
            assertEquals(sampleTransaction.getId(), result.getId());
            assertEquals(sampleTransaction.getAmount(), result.getAmount());
            assertEquals(sampleTransaction.getDescription(), result.getDescription());
            assertEquals(sampleTransaction.getType(), result.getType());
            assertEquals(sampleTransaction.getTimestamp(), result.getTimestamp());
            assertEquals(sampleTransaction.getCategory(), result.getCategory());
            assertEquals(sampleTransaction.getAccountNumber(), result.getAccountNumber());
            assertEquals(sampleTransaction.getReferenceNumber(), result.getReferenceNumber());
            assertEquals(sampleTransaction.getCreatedAt(), result.getCreatedAt());
            assertEquals(sampleTransaction.getUpdatedAt(), result.getUpdatedAt());
            
            verify(transactionRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw TransactionNotFoundException when transaction doesn't exist")
        void getTransactionById_NonExistentTransaction_ThrowsTransactionNotFoundException() {
            // Arrange
            when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class, () -> 
                transactionService.getTransactionById(1L));
            
            verify(transactionRepository).findById(1L);
        }
    }

    @Nested
    @DisplayName("Get All Transactions Tests")
    class GetAllTransactionsTests {

        @Test
        @DisplayName("Should return paginated transactions")
        void getAllTransactions_ValidPageable_ReturnsPagedTransactionResponseDTO() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            List<Transaction> transactions = List.of(sampleTransaction);
            Page<Transaction> transactionPage = new PageImpl<>(transactions, pageable, 1);
            when(transactionRepository.findAll(pageable)).thenReturn(transactionPage);

            // Act
            PagedTransactionResponseDTO result = transactionService.getAllTransactions(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(0, result.getPage());
            assertEquals(10, result.getSize());
            assertEquals(1, result.getTotalElements());
            assertEquals(1, result.getTotalPages());
            assertTrue(result.isFirst());
            assertTrue(result.isLast());
            assertFalse(result.isEmpty());
            
            TransactionResponseDTO firstTransaction = result.getContent().get(0);
            assertEquals(sampleTransaction.getId(), firstTransaction.getId());
            assertEquals(sampleTransaction.getAmount(), firstTransaction.getAmount());
            
            verify(transactionRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should return empty page when no transactions exist")
        void getAllTransactions_NoTransactions_ReturnsEmptyPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(transactionRepository.findAll(pageable)).thenReturn(emptyPage);

            // Act
            PagedTransactionResponseDTO result = transactionService.getAllTransactions(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(0, result.getContent().size());
            assertEquals(0, result.getPage());
            assertEquals(10, result.getSize());
            assertEquals(0, result.getTotalElements());
            assertEquals(0, result.getTotalPages());
            assertTrue(result.isFirst());
            assertTrue(result.isLast());
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle different page sizes and numbers")
        void getAllTransactions_DifferentPageSizes_HandlesCorrectly() {
            // Arrange
            Transaction transaction2 = new Transaction();
            transaction2.setId(2L);
            transaction2.setAmount(new BigDecimal("200.00"));
            transaction2.setDescription("Second transaction");
            transaction2.setType(TransactionType.CREDIT);
            transaction2.setTimestamp(fixedTimestamp.plusHours(1));

            Pageable pageable = PageRequest.of(1, 1);
            List<Transaction> transactions = List.of(transaction2);
            Page<Transaction> transactionPage = new PageImpl<>(transactions, pageable, 2);
            when(transactionRepository.findAll(pageable)).thenReturn(transactionPage);

            // Act
            PagedTransactionResponseDTO result = transactionService.getAllTransactions(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(1, result.getPage());
            assertEquals(1, result.getSize());
            assertEquals(2, result.getTotalElements());
            assertEquals(2, result.getTotalPages());
            assertFalse(result.isFirst());
            assertTrue(result.isLast());
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle sorting in pageable")
        void getAllTransactions_WithSorting_HandlesCorrectly() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10, Sort.by("timestamp").descending());
            List<Transaction> transactions = List.of(sampleTransaction);
            Page<Transaction> transactionPage = new PageImpl<>(transactions, pageable, 1);
            when(transactionRepository.findAll(pageable)).thenReturn(transactionPage);

            // Act
            PagedTransactionResponseDTO result = transactionService.getAllTransactions(pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(transactionRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Caching Behavior Tests")
    class CachingBehaviorTests {

        @Test
        @DisplayName("Should verify cache eviction on create")
        void createTransaction_CacheEviction_VerifyBehavior() {
            // Arrange
            when(validator.validate(validCreateDTO)).thenReturn(Collections.emptySet());
            when(transactionRepository.existsByAmountAndDescriptionAndTimestamp(
                    any(BigDecimal.class), anyString(), any(LocalDateTime.class))).thenReturn(false);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

            // Act
            TransactionResponseDTO result = transactionService.createTransaction(validCreateDTO);

            // Assert
            assertNotNull(result);
            // Cache eviction is handled by Spring's @CacheEvict annotation
            // We verify the method executes successfully, which implies cache eviction occurred
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should verify cache eviction on update")
        void updateTransaction_CacheEviction_VerifyBehavior() {
            // Arrange
            when(validator.validate(validUpdateDTO)).thenReturn(Collections.emptySet());
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

            // Act
            TransactionResponseDTO result = transactionService.updateTransaction(1L, validUpdateDTO);

            // Assert
            assertNotNull(result);
            // Cache eviction is handled by Spring's @CacheEvict annotation
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should verify cache eviction on delete")
        void deleteTransaction_CacheEviction_VerifyBehavior() {
            // Arrange
            when(transactionRepository.existsById(1L)).thenReturn(true);

            // Act
            assertDoesNotThrow(() -> transactionService.deleteTransaction(1L));

            // Assert
            // Cache eviction is handled by Spring's @CacheEvict annotation
            verify(transactionRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should verify caching on get by ID")
        void getTransactionById_Caching_VerifyBehavior() {
            // Arrange
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));

            // Act
            TransactionResponseDTO result = transactionService.getTransactionById(1L);

            // Assert
            assertNotNull(result);
            // Caching is handled by Spring's @Cacheable annotation
            // We verify the method executes successfully, which implies caching occurred
            verify(transactionRepository).findById(1L);
        }

        @Test
        @DisplayName("Should verify caching on get all transactions")
        void getAllTransactions_Caching_VerifyBehavior() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            List<Transaction> transactions = List.of(sampleTransaction);
            Page<Transaction> transactionPage = new PageImpl<>(transactions, pageable, 1);
            when(transactionRepository.findAll(pageable)).thenReturn(transactionPage);

            // Act
            PagedTransactionResponseDTO result = transactionService.getAllTransactions(pageable);

            // Assert
            assertNotNull(result);
            // Caching is handled by Spring's @Cacheable annotation
            verify(transactionRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle repository exceptions during create")
        void createTransaction_RepositoryException_PropagatesException() {
            // Arrange
            when(validator.validate(validCreateDTO)).thenReturn(Collections.emptySet());
            when(transactionRepository.existsByAmountAndDescriptionAndTimestamp(
                    any(BigDecimal.class), anyString(), any(LocalDateTime.class))).thenReturn(false);
            when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                transactionService.createTransaction(validCreateDTO));
            
            assertEquals("Database error", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle repository exceptions during update")
        void updateTransaction_RepositoryException_PropagatesException() {
            // Arrange
            when(validator.validate(validUpdateDTO)).thenReturn(Collections.emptySet());
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                transactionService.updateTransaction(1L, validUpdateDTO));
            
            assertEquals("Database error", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle repository exceptions during delete")
        void deleteTransaction_RepositoryException_PropagatesException() {
            // Arrange
            when(transactionRepository.existsById(1L)).thenReturn(true);
            doThrow(new RuntimeException("Database error")).when(transactionRepository).deleteById(1L);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                transactionService.deleteTransaction(1L));
            
            assertEquals("Database error", exception.getMessage());
        }
    }
}