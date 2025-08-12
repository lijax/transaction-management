package com.banking.transactionmanagement.dto;

import com.banking.transactionmanagement.model.TransactionType;
import com.banking.transactionmanagement.validation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for creating new transactions.
 * Contains validation annotations for input validation.
 */
@ValidTransactionAmount
@Schema(description = "Data Transfer Object for creating new transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreateDTO {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0.01")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer digits and 2 decimal places")
    @Schema(description = "Transaction amount", example = "100.50", required = true)
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Schema(description = "Transaction description", example = "Payment for services", required = true, maxLength = 255)
    private String description;

    @NotNull(message = "Transaction type is required")
    @Schema(description = "Type of transaction", example = "DEBIT", required = true, allowableValues = {"CREDIT", "DEBIT", "WITHDRAWAL", "TRANSFER", "DEPOSIT"})
    private TransactionType type;

    @NotNull(message = "Timestamp is required")
    @ValidTransactionTimestamp
    @Schema(description = "Transaction timestamp", example = "2024-01-15T10:30:00", required = true)
    private LocalDateTime timestamp;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_]+$", message = "Category can only contain letters, numbers, spaces, hyphens, and underscores")
    @Schema(description = "Transaction category", example = "Services", maxLength = 100)
    private String category;

    @Size(max = 50, message = "Account number must not exceed 50 characters")
    @ValidAccountNumber
    @Schema(description = "Account number (8-20 digits only)", example = "1234567890123456", maxLength = 50)
    private String accountNumber;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    @ValidReferenceNumber
    @Schema(description = "Reference number for the transaction (6-50 alphanumeric characters)", example = "REF789ABC123", maxLength = 100)
    private String referenceNumber;


}