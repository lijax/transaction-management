package com.banking.transactionmanagement.dto;

import com.banking.transactionmanagement.model.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for transaction API responses.
 * Contains all transaction data formatted for client consumption.
 */
@Schema(description = "Transaction response data transfer object")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {

    @Schema(description = "Unique transaction identifier", example = "1")
    private Long id;

    @Schema(description = "Transaction amount", example = "100.50")
    private BigDecimal amount;

    @Schema(description = "Transaction description", example = "Payment for services")
    private String description;

    @Schema(description = "Type of transaction", example = "DEBIT")
    private TransactionType type;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Transaction timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Transaction category", example = "Services")
    private String category;

    @Schema(description = "Account number (8-20 digits only)", example = "1234567890123456")
    private String accountNumber;

    @Schema(description = "Reference number (6-50 alphanumeric characters)", example = "REF789ABC123")
    private String referenceNumber;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;




}