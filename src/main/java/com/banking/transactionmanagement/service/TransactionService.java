package com.banking.transactionmanagement.service;

import com.banking.transactionmanagement.dto.TransactionCreateDTO;
import com.banking.transactionmanagement.dto.TransactionResponseDTO;
import com.banking.transactionmanagement.dto.TransactionUpdateDTO;
import com.banking.transactionmanagement.dto.PagedTransactionResponseDTO;
import com.banking.transactionmanagement.exception.DuplicateTransactionException;
import com.banking.transactionmanagement.exception.TransactionNotFoundException;
import com.banking.transactionmanagement.exception.ValidationException;
import com.banking.transactionmanagement.model.Transaction;
import com.banking.transactionmanagement.repository.TransactionRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Service layer for transaction management with business logic, validation,
 * caching, and transactional support.
 */
@Service
@Transactional
public class TransactionService {

  private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

  private final TransactionRepository transactionRepository;
  private final Validator validator;
  private final QueryPerformanceMonitoringService performanceMonitoringService;

  @Autowired
  public TransactionService(TransactionRepository transactionRepository, 
                           Validator validator,
                           QueryPerformanceMonitoringService performanceMonitoringService) {
    this.transactionRepository = transactionRepository;
    this.validator = validator;
    this.performanceMonitoringService = performanceMonitoringService;
  }

  /**
   * Creates a new transaction with validation and duplicate checking.
   * 
   * @param createDTO the transaction data to create
   * @return the created transaction response DTO
   * @throws ValidationException           if validation fails
   * @throws DuplicateTransactionException if duplicate transaction detected
   */
  @CacheEvict(value = { "transactions", "transactionsList" }, allEntries = true)
  public TransactionResponseDTO createTransaction(TransactionCreateDTO createDTO) {
    logger.info("Creating new transaction: {}", createDTO);

    // Validate input DTO
    validateTransactionCreateDTO(createDTO);

    // Check for duplicate transactions
    checkForDuplicateTransaction(createDTO);

    // Convert DTO to entity
    Transaction transaction = convertCreateDTOToEntity(createDTO);

    // Save transaction with performance monitoring
    Transaction savedTransaction = performanceMonitoringService.monitorQuery(
        "create_transaction", 
        () -> transactionRepository.save(transaction)
    );

    performanceMonitoringService.incrementCounter("transaction.created", "type", transaction.getType().toString());
    logger.info("Successfully created transaction with ID: {}", savedTransaction.getId());

    return convertEntityToResponseDTO(savedTransaction);
  }

  /**
   * Updates an existing transaction with partial update support.
   * 
   * @param id        the transaction ID to update
   * @param updateDTO the update data
   * @return the updated transaction response DTO
   * @throws TransactionNotFoundException if transaction not found
   * @throws ValidationException          if validation fails
   */
  @CacheEvict(value = { "transactions", "transactionsList" }, allEntries = true)
  public TransactionResponseDTO updateTransaction(Long id, TransactionUpdateDTO updateDTO) {
    logger.info("Updating transaction with ID: {} using data: {}", id, updateDTO);

    // Validate input DTO
    validateTransactionUpdateDTO(updateDTO);

    // Check if transaction exists with performance monitoring
    Transaction existingTransaction = performanceMonitoringService.monitorQuery(
        "find_transaction_by_id",
        () -> transactionRepository.findById(id)
            .orElseThrow(() -> new TransactionNotFoundException(id))
    );

    // Apply partial updates
    applyPartialUpdates(existingTransaction, updateDTO);

    // Save updated transaction with performance monitoring
    Transaction updatedTransaction = performanceMonitoringService.monitorQuery(
        "update_transaction",
        () -> transactionRepository.save(existingTransaction)
    );

    performanceMonitoringService.incrementCounter("transaction.updated", "type", updatedTransaction.getType().toString());
    logger.info("Successfully updated transaction with ID: {}", updatedTransaction.getId());

    return convertEntityToResponseDTO(updatedTransaction);
  }

  /**
   * Deletes a transaction by ID with existence validation.
   * 
   * @param id the transaction ID to delete
   * @throws TransactionNotFoundException if transaction not found
   */
  @CacheEvict(value = { "transactions", "transactionsList" }, allEntries = true)
  public void deleteTransaction(Long id) {
    logger.info("Deleting transaction with ID: {}", id);

    // Check if transaction exists with performance monitoring
    boolean exists = performanceMonitoringService.monitorQuery(
        "check_transaction_exists",
        () -> transactionRepository.existsById(id)
    );
    
    if (!exists) {
      throw new TransactionNotFoundException(id);
    }

    // Delete transaction with performance monitoring
    performanceMonitoringService.monitorQuery(
        "delete_transaction",
        () -> transactionRepository.deleteById(id)
    );

    performanceMonitoringService.incrementCounter("transaction.deleted");
    logger.info("Successfully deleted transaction with ID: {}", id);
  }

  /**
   * Retrieves a transaction by ID with caching.
   * 
   * @param id the transaction ID
   * @return the transaction response DTO
   * @throws TransactionNotFoundException if transaction not found
   */
  @Cacheable(value = "transactions", key = "#id")
  @Transactional(readOnly = true)
  public TransactionResponseDTO getTransactionById(Long id) {
    logger.debug("Retrieving transaction with ID: {}", id);

    Transaction transaction = performanceMonitoringService.monitorQuery(
        "get_transaction_by_id",
        () -> transactionRepository.findById(id)
            .orElseThrow(() -> new TransactionNotFoundException(id))
    );

    return convertEntityToResponseDTO(transaction);
  }

  /**
   * Retrieves all transactions with pagination and caching.
   * 
   * @param pageable pagination parameters
   * @return paged transaction response DTO
   */
  @Cacheable(value = "transactionsList", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
  @Transactional(readOnly = true)
  public PagedTransactionResponseDTO getAllTransactions(Pageable pageable) {
    logger.debug("Retrieving transactions with pagination: {}", pageable);

    Page<Transaction> transactionPage = performanceMonitoringService.monitorQuery(
        "get_all_transactions_paginated",
        () -> transactionRepository.findAll(pageable)
    );

    return convertPageToPagedResponseDTO(transactionPage);
  }

  /**
   * Validates the transaction create DTO.
   * 
   * @param createDTO the DTO to validate
   * @throws ValidationException if validation fails
   */
  private void validateTransactionCreateDTO(TransactionCreateDTO createDTO) {
    if (createDTO == null) {
      throw new ValidationException("Transaction data cannot be null");
    }

    Set<ConstraintViolation<TransactionCreateDTO>> violations = validator.validate(createDTO);
    if (!violations.isEmpty()) {
      StringBuilder errorMessage = new StringBuilder("Validation failed: ");
      for (ConstraintViolation<TransactionCreateDTO> violation : violations) {
        errorMessage.append(violation.getMessage()).append("; ");
      }
      throw new ValidationException(errorMessage.toString());
    }
  }

  /**
   * Validates the transaction update DTO.
   * 
   * @param updateDTO the DTO to validate
   * @throws ValidationException if validation fails
   */
  private void validateTransactionUpdateDTO(TransactionUpdateDTO updateDTO) {
    if (updateDTO == null) {
      throw new ValidationException("Update data cannot be null");
    }

    if (!updateDTO.hasUpdates()) {
      throw new ValidationException("At least one field must be provided for update");
    }

    Set<ConstraintViolation<TransactionUpdateDTO>> violations = validator.validate(updateDTO);
    if (!violations.isEmpty()) {
      StringBuilder errorMessage = new StringBuilder("Validation failed: ");
      for (ConstraintViolation<TransactionUpdateDTO> violation : violations) {
        errorMessage.append(violation.getMessage()).append("; ");
      }
      throw new ValidationException(errorMessage.toString());
    }
  }

  /**
   * Checks for duplicate transactions based on amount, description, and
   * timestamp.
   * 
   * @param createDTO the transaction data to check
   * @throws DuplicateTransactionException if duplicate found
   */
  private void checkForDuplicateTransaction(TransactionCreateDTO createDTO) {
    boolean isDuplicate = performanceMonitoringService.monitorQuery(
        "check_duplicate_transaction",
        () -> transactionRepository.existsByAmountAndDescriptionAndTimestamp(
            createDTO.getAmount(),
            createDTO.getDescription(),
            createDTO.getTimestamp())
    );

    if (isDuplicate) {
      throw new DuplicateTransactionException(
          "Duplicate transaction detected with amount: " + createDTO.getAmount() +
              ", description: " + createDTO.getDescription() +
              ", timestamp: " + createDTO.getTimestamp());
    }
  }

  /**
   * Converts TransactionCreateDTO to Transaction entity.
   * 
   * @param createDTO the DTO to convert
   * @return the converted entity
   */
  private Transaction convertCreateDTOToEntity(TransactionCreateDTO createDTO) {
    Transaction transaction = new Transaction();
    transaction.setAmount(createDTO.getAmount());
    transaction.setDescription(createDTO.getDescription());
    transaction.setType(createDTO.getType());
    transaction.setTimestamp(createDTO.getTimestamp());
    transaction.setCategory(createDTO.getCategory());
    transaction.setAccountNumber(createDTO.getAccountNumber());
    transaction.setReferenceNumber(createDTO.getReferenceNumber());
    return transaction;
  }

  /**
   * Applies partial updates from UpdateDTO to existing transaction entity.
   * 
   * @param existingTransaction the transaction to update
   * @param updateDTO           the update data
   */
  private void applyPartialUpdates(Transaction existingTransaction, TransactionUpdateDTO updateDTO) {
    if (updateDTO.getAmount() != null) {
      existingTransaction.setAmount(updateDTO.getAmount());
    }
    if (updateDTO.getDescription() != null) {
      existingTransaction.setDescription(updateDTO.getDescription());
    }
    if (updateDTO.getType() != null) {
      existingTransaction.setType(updateDTO.getType());
    }
    if (updateDTO.getTimestamp() != null) {
      existingTransaction.setTimestamp(updateDTO.getTimestamp());
    }
    if (updateDTO.getCategory() != null) {
      existingTransaction.setCategory(updateDTO.getCategory());
    }
    if (updateDTO.getAccountNumber() != null) {
      existingTransaction.setAccountNumber(updateDTO.getAccountNumber());
    }
    if (updateDTO.getReferenceNumber() != null) {
      existingTransaction.setReferenceNumber(updateDTO.getReferenceNumber());
    }
  }

  /**
   * Converts Transaction entity to TransactionResponseDTO.
   * 
   * @param transaction the entity to convert
   * @return the converted response DTO
   */
  private TransactionResponseDTO convertEntityToResponseDTO(Transaction transaction) {
    return new TransactionResponseDTO(
        transaction.getId(),
        transaction.getAmount(),
        transaction.getDescription(),
        transaction.getType(),
        transaction.getTimestamp(),
        transaction.getCategory(),
        transaction.getAccountNumber(),
        transaction.getReferenceNumber(),
        transaction.getCreatedAt(),
        transaction.getUpdatedAt());
  }

  /**
   * Converts Page<Transaction> to PagedTransactionResponseDTO.
   * 
   * @param transactionPage the page to convert
   * @return the converted paged response DTO
   */
  private PagedTransactionResponseDTO convertPageToPagedResponseDTO(Page<Transaction> transactionPage) {
    PagedTransactionResponseDTO pagedResponse = new PagedTransactionResponseDTO();

    // Convert transactions to response DTOs
    pagedResponse.setContent(
        transactionPage.getContent().stream()
            .map(this::convertEntityToResponseDTO)
            .toList());

    // Set pagination metadata
    pagedResponse.setPage(transactionPage.getNumber());
    pagedResponse.setSize(transactionPage.getSize());
    pagedResponse.setTotalElements(transactionPage.getTotalElements());
    pagedResponse.setTotalPages(transactionPage.getTotalPages());
    pagedResponse.setFirst(transactionPage.isFirst());
    pagedResponse.setLast(transactionPage.isLast());
    pagedResponse.setEmpty(transactionPage.isEmpty());

    return pagedResponse;
  }
}