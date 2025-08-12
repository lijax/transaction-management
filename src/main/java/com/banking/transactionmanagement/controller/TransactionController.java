package com.banking.transactionmanagement.controller;

import com.banking.transactionmanagement.dto.TransactionCreateDTO;
import com.banking.transactionmanagement.dto.TransactionResponseDTO;
import com.banking.transactionmanagement.dto.TransactionUpdateDTO;
import com.banking.transactionmanagement.dto.PagedTransactionResponseDTO;
import com.banking.transactionmanagement.dto.ErrorResponseDTO;
import com.banking.transactionmanagement.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for transaction management operations.
 * Provides CRUD endpoints for financial transactions with proper validation,
 * error handling, and pagination support.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Validated
@Tag(name = "Transaction Management", description = "API endpoints for managing financial transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Creates a new transaction.
     * 
     * @param createDTO the transaction data to create
     * @return ResponseEntity with created transaction data and HTTP 201 status
     */
    @Operation(
        summary = "Create a new transaction",
        description = "Creates a new financial transaction with the provided details. All required fields must be provided and validated."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Transaction created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransactionResponseDTO.class),
                examples = @ExampleObject(
                    name = "Created Transaction",
                    value = "{\"id\": 1, \"amount\": 100.50, \"description\": \"Payment for services\", \"type\": \"DEBIT\", \"timestamp\": \"2024-01-15T10:30:00\", \"category\": \"Services\", \"accountNumber\": \"12345678\", \"referenceNumber\": \"TXN123456\", \"createdAt\": \"2024-01-15T10:30:00\", \"updatedAt\": \"2024-01-15T10:30:00\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid input data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDTO.class),
                examples = @ExampleObject(
                    name = "Validation Error",
                    value = "{\"timestamp\": \"2024-01-15T10:30:00\", \"status\": 400, \"error\": \"Bad Request\", \"message\": \"Validation failed\", \"path\": \"/api/v1/transactions\", \"details\": [{\"field\": \"amount\", \"message\": \"Amount is required\"}]}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Duplicate transaction",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            @Parameter(description = "Transaction data to create", required = true)
            @Valid @RequestBody TransactionCreateDTO createDTO) {
        
        logger.info("Received request to create transaction: {}", createDTO);
        
        TransactionResponseDTO createdTransaction = transactionService.createTransaction(createDTO);
        
        logger.info("Successfully created transaction with ID: {}", createdTransaction.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }

    /**
     * Retrieves all transactions with pagination support.
     * 
     * @param page the page number (default: 0)
     * @param size the page size (default: 20)
     * @param sort the sort criteria (default: timestamp,desc)
     * @return ResponseEntity with paginated transaction list and HTTP 200 status
     */
    @Operation(
        summary = "Get all transactions",
        description = "Retrieves a paginated list of all transactions. Supports sorting by various fields."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Transactions retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PagedTransactionResponseDTO.class),
                examples = @ExampleObject(
                    name = "Paginated Transactions",
                    value = "{\"content\": [{\"id\": 1, \"amount\": 100.50, \"description\": \"Payment\", \"type\": \"DEBIT\", \"timestamp\": \"2024-01-15T10:30:00\"}], \"page\": 0, \"size\": 20, \"totalElements\": 1, \"totalPages\": 1, \"first\": true, \"last\": true, \"empty\": false}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid pagination parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<PagedTransactionResponseDTO> getAllTransactions(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria in format: field,direction (e.g., timestamp,desc)", example = "timestamp,desc")
            @RequestParam(defaultValue = "timestamp,desc") String sort) {
        
        logger.debug("Received request to get transactions - page: {}, size: {}, sort: {}", 
                    page, size, sort);
        
        // Parse sort parameter
        Pageable pageable = createPageable(page, size, sort);
        
        PagedTransactionResponseDTO pagedResponse = transactionService.getAllTransactions(pageable);
        
        logger.debug("Successfully retrieved {} transactions", pagedResponse.getContent().size());
        
        return ResponseEntity.ok(pagedResponse);
    }

    /**
     * Retrieves a specific transaction by ID.
     * 
     * @param id the transaction ID
     * @return ResponseEntity with transaction data and HTTP 200 status
     */
    @Operation(
        summary = "Get transaction by ID",
        description = "Retrieves a specific transaction using its unique identifier."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Transaction found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransactionResponseDTO.class),
                examples = @ExampleObject(
                    name = "Transaction Details",
                    value = "{\"id\": 1, \"amount\": 100.50, \"description\": \"Payment for services\", \"type\": \"DEBIT\", \"timestamp\": \"2024-01-15T10:30:00\", \"category\": \"Services\", \"accountNumber\": \"12345678\", \"referenceNumber\": \"TXN123456\", \"createdAt\": \"2024-01-15T10:30:00\", \"updatedAt\": \"2024-01-15T10:30:00\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Transaction not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDTO.class),
                examples = @ExampleObject(
                    name = "Not Found Error",
                    value = "{\"timestamp\": \"2024-01-15T10:30:00\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Transaction not found with ID: 999\", \"path\": \"/api/v1/transactions/999\"}"
                )
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(
            @Parameter(description = "Transaction ID", required = true, example = "1")
            @PathVariable Long id) {
        
        logger.debug("Received request to get transaction with ID: {}", id);
        
        TransactionResponseDTO transaction = transactionService.getTransactionById(id);
        
        logger.debug("Successfully retrieved transaction with ID: {}", id);
        
        return ResponseEntity.ok(transaction);
    }

    /**
     * Updates an existing transaction.
     * 
     * @param id the transaction ID to update
     * @param updateDTO the update data
     * @return ResponseEntity with updated transaction data and HTTP 200 status
     */
    @Operation(
        summary = "Update transaction",
        description = "Updates an existing transaction with the provided data. Only provided fields will be updated."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Transaction updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransactionResponseDTO.class),
                examples = @ExampleObject(
                    name = "Updated Transaction",
                    value = "{\"id\": 1, \"amount\": 150.75, \"description\": \"Updated payment\", \"type\": \"DEBIT\", \"timestamp\": \"2024-01-15T10:30:00\", \"category\": \"Services\", \"accountNumber\": \"12345678\", \"referenceNumber\": \"TXN123456\", \"createdAt\": \"2024-01-15T10:30:00\", \"updatedAt\": \"2024-01-15T11:00:00\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid input data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Transaction not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDTO.class)
            )
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> updateTransaction(
            @Parameter(description = "Transaction ID to update", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Transaction update data", required = true)
            @Valid @RequestBody TransactionUpdateDTO updateDTO) {
        
        logger.info("Received request to update transaction with ID: {} using data: {}", id, updateDTO);
        
        TransactionResponseDTO updatedTransaction = transactionService.updateTransaction(id, updateDTO);
        
        logger.info("Successfully updated transaction with ID: {}", id);
        
        return ResponseEntity.ok(updatedTransaction);
    }

    /**
     * Deletes a transaction by ID.
     * 
     * @param id the transaction ID to delete
     * @return ResponseEntity with HTTP 204 No Content status
     */
    @Operation(
        summary = "Delete transaction",
        description = "Permanently deletes a transaction from the system using its unique identifier."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204", 
            description = "Transaction deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Transaction not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponseDTO.class),
                examples = @ExampleObject(
                    name = "Not Found Error",
                    value = "{\"timestamp\": \"2024-01-15T10:30:00\", \"status\": 404, \"error\": \"Not Found\", \"message\": \"Transaction not found with ID: 999\", \"path\": \"/api/v1/transactions/999\"}"
                )
            )
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @Parameter(description = "Transaction ID to delete", required = true, example = "1")
            @PathVariable Long id) {
        
        logger.info("Received request to delete transaction with ID: {}", id);
        
        transactionService.deleteTransaction(id);
        
        logger.info("Successfully deleted transaction with ID: {}", id);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a Pageable object from request parameters.
     * 
     * @param page the page number
     * @param size the page size
     * @param sort the sort criteria string
     * @return configured Pageable object
     */
    private Pageable createPageable(int page, int size, String sort) {
        // Validate page and size parameters
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 20; // Default size with maximum limit
        }

        // Parse sort parameter (format: "field,direction" or just "field")
        Sort sortObj = Sort.unsorted();
        if (sort != null && !sort.trim().isEmpty()) {
            String[] sortParts = sort.split(",");
            String field = sortParts[0].trim();
            
            // Default to descending for timestamp, ascending for others
            Sort.Direction direction = Sort.Direction.DESC;
            if (sortParts.length > 1) {
                String directionStr = sortParts[1].trim().toLowerCase();
                direction = "asc".equals(directionStr) ? Sort.Direction.ASC : Sort.Direction.DESC;
            } else if (!"timestamp".equals(field)) {
                direction = Sort.Direction.ASC;
            }
            
            sortObj = Sort.by(direction, field);
        } else {
            // Default sort by timestamp descending
            sortObj = Sort.by(Sort.Direction.DESC, "timestamp");
        }

        return PageRequest.of(page, size, sortObj);
    }
}