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
 * Data Transfer Object for updating existing transactions.
 * Supports partial updates - all fields are optional but validated when provided.
 */
@ValidTransactionAmount
@Schema(description = "Data Transfer Object for updating existing transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionUpdateDTO {

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0.01")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer digits and 2 decimal places")
    @Schema(description = "Transaction amount (optional for update)", example = "150.75")
    private BigDecimal amount;

    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
    @Schema(description = "Transaction description (optional for update)", example = "Updated payment description", maxLength = 255)
    private String description;

    @Schema(description = "Type of transaction (optional for update)", example = "CREDIT", allowableValues = {"CREDIT", "DEBIT", "WITHDRAWAL", "TRANSFER", "DEPOSIT"})
    private TransactionType type;

    @ValidTransactionTimestamp
    @Schema(description = "Transaction timestamp (optional for update)", example = "2024-01-15T11:00:00")
    private LocalDateTime timestamp;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_]+$", message = "Category can only contain letters, numbers, spaces, hyphens, and underscores")
    @Schema(description = "Transaction category (optional for update)", example = "Updated Services", maxLength = 100)
    private String category;

    @Size(max = 50, message = "Account number must not exceed 50 characters")
    @ValidAccountNumber
    @Schema(description = "Account number (optional for update, 8-20 digits only)", example = "9876543210987654", maxLength = 50)
    private String accountNumber;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    @ValidReferenceNumber
    @Schema(description = "Reference number (optional for update, 6-50 alphanumeric characters)", example = "UPD456XYZ789", maxLength = 100)
    private String referenceNumber;



    /**
     * Checks if any field has been set for update
     * @return true if at least one field is not null
     */
    public boolean hasUpdates() {
        return amount != null || description != null || type != null || 
               timestamp != null || category != null || accountNumber != null || 
               referenceNumber != null;
    }


}