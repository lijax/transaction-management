package com.banking.transactionmanagement.repository;

import com.banking.transactionmanagement.model.Transaction;
import com.banking.transactionmanagement.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Transaction entity providing custom query methods
 * for efficient data retrieval and pagination with performance optimization.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find transactions by account number with pagination support.
     * Uses index on accountNumber for performance.
     */
    Page<Transaction> findByAccountNumber(String accountNumber, Pageable pageable);

    /**
     * Find transactions by transaction type with pagination support.
     * Uses index on type for performance.
     */
    Page<Transaction> findByType(TransactionType type, Pageable pageable);

    /**
     * Find transactions by category with pagination support.
     * Uses index on category for performance.
     */
    Page<Transaction> findByCategory(String category, Pageable pageable);

    /**
     * Find transactions within a date range with pagination support.
     * Uses index on timestamp for performance.
     */
    Page<Transaction> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find transactions by amount range with pagination support.
     * Optimized query for amount-based filtering.
     */
    @Query("SELECT t FROM Transaction t WHERE t.amount BETWEEN :minAmount AND :maxAmount")
    Page<Transaction> findByAmountBetween(@Param("minAmount") BigDecimal minAmount, 
                                         @Param("maxAmount") BigDecimal maxAmount, 
                                         Pageable pageable);

    /**
     * Find transactions by account number and type combination.
     * Uses composite filtering for efficient queries.
     */
    Page<Transaction> findByAccountNumberAndType(String accountNumber, TransactionType type, Pageable pageable);

    /**
     * Find transactions by account number within date range.
     * Combines account and date filtering for performance.
     */
    @Query("SELECT t FROM Transaction t WHERE t.accountNumber = :accountNumber " +
           "AND t.timestamp BETWEEN :startDate AND :endDate")
    Page<Transaction> findByAccountNumberAndTimestampBetween(@Param("accountNumber") String accountNumber,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate,
                                                           Pageable pageable);

    /**
     * Find transactions by reference number.
     * Uses index on referenceNumber for performance.
     */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    /**
     * Check if a transaction exists with the same amount, description, and timestamp.
     * Used for duplicate detection.
     */
    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.amount = :amount " +
           "AND t.description = :description AND t.timestamp = :timestamp")
    boolean existsByAmountAndDescriptionAndTimestamp(@Param("amount") BigDecimal amount,
                                                   @Param("description") String description,
                                                   @Param("timestamp") LocalDateTime timestamp);

    /**
     * Find recent transactions for an account (last N transactions).
     * Optimized for dashboard/summary views.
     */
    @Query("SELECT t FROM Transaction t WHERE t.accountNumber = :accountNumber " +
           "ORDER BY t.timestamp DESC")
    List<Transaction> findRecentTransactionsByAccountNumber(@Param("accountNumber") String accountNumber, 
                                                          Pageable pageable);

    /**
     * Get transaction count by type for reporting.
     * Aggregation query for analytics.
     */
    @Query("SELECT t.type, COUNT(t) FROM Transaction t GROUP BY t.type")
    List<Object[]> countTransactionsByType();

    /**
     * Get total amount by transaction type for reporting.
     * Aggregation query for financial summaries.
     */
    @Query("SELECT t.type, SUM(t.amount) FROM Transaction t GROUP BY t.type")
    List<Object[]> sumAmountByTransactionType();

    /**
     * Find transactions with description containing search term.
     * Case-insensitive search for transaction descriptions.
     */
    @Query("SELECT t FROM Transaction t WHERE LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Transaction> findByDescriptionContainingIgnoreCase(@Param("searchTerm") String searchTerm, 
                                                          Pageable pageable);

    /**
     * Get account balance by summing all transactions for an account.
     * Calculates running balance for account management.
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN t.type IN ('CREDIT', 'DEPOSIT') THEN t.amount " +
           "WHEN t.type IN ('DEBIT', 'WITHDRAWAL') THEN -t.amount " +
           "ELSE 0 END), 0) FROM Transaction t WHERE t.accountNumber = :accountNumber")
    BigDecimal calculateAccountBalance(@Param("accountNumber") String accountNumber);

    /**
     * Find transactions created within a specific time period.
     * Uses createdAt timestamp for audit queries.
     */
    Page<Transaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find transactions updated within a specific time period.
     * Uses updatedAt timestamp for audit queries.
     */
    Page<Transaction> findByUpdatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}