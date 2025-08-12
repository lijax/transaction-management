package com.banking.transactionmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a financial transaction in the banking system.
 * Includes comprehensive validation, indexing for performance, and audit
 * fields.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_timestamp", columnList = "timestamp"),
        @Index(name = "idx_transaction_type", columnList = "type"),
        @Index(name = "idx_transaction_account", columnList = "accountNumber"),
        @Index(name = "idx_transaction_reference", columnList = "referenceNumber"),
        @Index(name = "idx_transaction_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "amount", "description", "type", "timestamp", "accountNumber", "referenceNumber"})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0.01")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer digits and 2 decimal places")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Column(nullable = false, length = 255)
    private String description;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @NotNull(message = "Timestamp is required")
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    @Column(length = 100)
    private String category;

    @Size(max = 50, message = "Account number must not exceed 50 characters")
    @Column(length = 50)
    private String accountNumber;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    @Column(length = 100)
    private String referenceNumber;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructor with required fields
    public Transaction(BigDecimal amount, String description, TransactionType type, LocalDateTime timestamp) {
        this.amount = amount;
        this.description = description;
        this.type = type;
        this.timestamp = timestamp;
    }
}