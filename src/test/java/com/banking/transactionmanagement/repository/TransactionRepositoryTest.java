package com.banking.transactionmanagement.repository;

import com.banking.transactionmanagement.model.Transaction;
import com.banking.transactionmanagement.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TransactionRepository using @DataJpaTest.
 * Tests custom query methods, pagination, and performance-optimized queries.
 */
@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    private Transaction transaction1;
    private Transaction transaction2;
    private Transaction transaction3;
    private Transaction transaction4;

    @BeforeEach
    void setUp() {
        // Create test transactions with different attributes
        transaction1 = new Transaction();
        transaction1.setAmount(new BigDecimal("100.50"));
        transaction1.setDescription("Grocery shopping");
        transaction1.setType(TransactionType.DEBIT);
        transaction1.setTimestamp(LocalDateTime.now().minusDays(1));
        transaction1.setCategory("Food");
        transaction1.setAccountNumber("ACC001");
        transaction1.setReferenceNumber("REF001");

        transaction2 = new Transaction();
        transaction2.setAmount(new BigDecimal("500.00"));
        transaction2.setDescription("Salary deposit");
        transaction2.setType(TransactionType.CREDIT);
        transaction2.setTimestamp(LocalDateTime.now().minusDays(2));
        transaction2.setCategory("Income");
        transaction2.setAccountNumber("ACC001");
        transaction2.setReferenceNumber("REF002");

        transaction3 = new Transaction();
        transaction3.setAmount(new BigDecimal("75.25"));
        transaction3.setDescription("Gas station");
        transaction3.setType(TransactionType.DEBIT);
        transaction3.setTimestamp(LocalDateTime.now().minusDays(3));
        transaction3.setCategory("Transportation");
        transaction3.setAccountNumber("ACC002");
        transaction3.setReferenceNumber("REF003");

        transaction4 = new Transaction();
        transaction4.setAmount(new BigDecimal("200.00"));
        transaction4.setDescription("ATM withdrawal");
        transaction4.setType(TransactionType.WITHDRAWAL);
        transaction4.setTimestamp(LocalDateTime.now().minusDays(4));
        transaction4.setCategory("Cash");
        transaction4.setAccountNumber("ACC001");
        transaction4.setReferenceNumber("REF004");

        // Persist test data
        entityManager.persistAndFlush(transaction1);
        entityManager.persistAndFlush(transaction2);
        entityManager.persistAndFlush(transaction3);
        entityManager.persistAndFlush(transaction4);
    }

    @Test
    void testFindByAccountNumber() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("timestamp").descending());

        // When
        Page<Transaction> result = transactionRepository.findByAccountNumber("ACC001", pageable);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).extracting(Transaction::getAccountNumber)
                .containsOnly("ACC001");
    }

    @Test
    void testFindByType() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> result = transactionRepository.findByType(TransactionType.DEBIT, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Transaction::getType)
                .containsOnly(TransactionType.DEBIT);
    }

    @Test
    void testFindByCategory() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> result = transactionRepository.findByCategory("Food", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategory()).isEqualTo("Food");
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Grocery shopping");
    }

    @Test
    void testFindByTimestampBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(4);
        LocalDateTime endDate = LocalDateTime.now().minusDays(2);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> result = transactionRepository.findByTimestampBetween(startDate, endDate, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Transaction::getDescription)
                .containsExactlyInAnyOrder("Salary deposit", "Gas station");
    }

    @Test
    void testFindByAmountBetween() {
        // Given
        BigDecimal minAmount = new BigDecimal("50.00");
        BigDecimal maxAmount = new BigDecimal("150.00");
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> result = transactionRepository.findByAmountBetween(minAmount, maxAmount, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(Transaction::getAmount)
                .containsExactlyInAnyOrder(new BigDecimal("100.50"), new BigDecimal("75.25"));
    }

    @Test
    void testFindByAccountNumberAndType() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> result = transactionRepository.findByAccountNumberAndType("ACC001", TransactionType.DEBIT, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAccountNumber()).isEqualTo("ACC001");
        assertThat(result.getContent().get(0).getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Grocery shopping");
    }

    @Test
    void testFindByAccountNumberAndTimestampBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(5);
        LocalDateTime endDate = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> result = transactionRepository.findByAccountNumberAndTimestampBetween(
                "ACC001", startDate, endDate, pageable);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).extracting(Transaction::getAccountNumber)
                .containsOnly("ACC001");
    }

    @Test
    void testFindByReferenceNumber() {
        // When
        Optional<Transaction> result = transactionRepository.findByReferenceNumber("REF001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).isEqualTo("Grocery shopping");
        assertThat(result.get().getReferenceNumber()).isEqualTo("REF001");
    }

    @Test
    void testFindByReferenceNumber_NotFound() {
        // When
        Optional<Transaction> result = transactionRepository.findByReferenceNumber("NONEXISTENT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testExistsByAmountAndDescriptionAndTimestamp() {
        // Given
        BigDecimal amount = transaction1.getAmount();
        String description = transaction1.getDescription();
        LocalDateTime timestamp = transaction1.getTimestamp();

        // When
        boolean exists = transactionRepository.existsByAmountAndDescriptionAndTimestamp(amount, description, timestamp);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByAmountAndDescriptionAndTimestamp_NotFound() {
        // Given
        BigDecimal amount = new BigDecimal("999.99");
        String description = "Non-existent transaction";
        LocalDateTime timestamp = LocalDateTime.now();

        // When
        boolean exists = transactionRepository.existsByAmountAndDescriptionAndTimestamp(amount, description, timestamp);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void testFindRecentTransactionsByAccountNumber() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        List<Transaction> result = transactionRepository.findRecentTransactionsByAccountNumber("ACC001", pageable);

        // Then
        assertThat(result).hasSize(2);
        // Should be ordered by timestamp descending (most recent first)
        assertThat(result.get(0).getTimestamp()).isAfter(result.get(1).getTimestamp());
        assertThat(result).extracting(Transaction::getAccountNumber)
                .containsOnly("ACC001");
    }

    @Test
    void testCountTransactionsByType() {
        // When
        List<Object[]> result = transactionRepository.countTransactionsByType();

        // Then
        assertThat(result).hasSize(3); // DEBIT, CREDIT, WITHDRAWAL
        
        // Verify counts
        for (Object[] row : result) {
            TransactionType type = (TransactionType) row[0];
            Long count = (Long) row[1];
            
            switch (type) {
                case DEBIT -> assertThat(count).isEqualTo(2L);
                case CREDIT -> assertThat(count).isEqualTo(1L);
                case WITHDRAWAL -> assertThat(count).isEqualTo(1L);
                default -> throw new AssertionError("Unexpected transaction type: " + type);
            }
        }
    }

    @Test
    void testSumAmountByTransactionType() {
        // When
        List<Object[]> result = transactionRepository.sumAmountByTransactionType();

        // Then
        assertThat(result).hasSize(3); // DEBIT, CREDIT, WITHDRAWAL
        
        // Verify sums
        for (Object[] row : result) {
            TransactionType type = (TransactionType) row[0];
            BigDecimal sum = (BigDecimal) row[1];
            
            switch (type) {
                case DEBIT -> assertThat(sum).isEqualByComparingTo(new BigDecimal("175.75")); // 100.50 + 75.25
                case CREDIT -> assertThat(sum).isEqualByComparingTo(new BigDecimal("500.00"));
                case WITHDRAWAL -> assertThat(sum).isEqualByComparingTo(new BigDecimal("200.00"));
                default -> throw new AssertionError("Unexpected transaction type: " + type);
            }
        }
    }

    @Test
    void testFindByDescriptionContainingIgnoreCase() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> result = transactionRepository.findByDescriptionContainingIgnoreCase("GROCERY", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Grocery shopping");
    }

    @Test
    void testCalculateAccountBalance() {
        // When
        BigDecimal balance = transactionRepository.calculateAccountBalance("ACC001");

        // Then
        // ACC001 has: +500.00 (CREDIT) - 100.50 (DEBIT) - 200.00 (WITHDRAWAL) = 199.50
        assertThat(balance).isEqualByComparingTo(new BigDecimal("199.50"));
    }

    @Test
    void testCalculateAccountBalance_NoTransactions() {
        // When
        BigDecimal balance = transactionRepository.calculateAccountBalance("NONEXISTENT");

        // Then
        assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void testFindByCreatedAtBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> result = transactionRepository.findByCreatedAtBetween(startDate, endDate, pageable);

        // Then
        assertThat(result.getContent()).hasSize(4); // All transactions were created recently
    }

    @Test
    void testFindByUpdatedAtBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Transaction> result = transactionRepository.findByUpdatedAtBetween(startDate, endDate, pageable);

        // Then
        assertThat(result.getContent()).hasSize(4); // All transactions were updated recently
    }

    @Test
    void testPaginationFunctionality() {
        // Given
        Pageable firstPage = PageRequest.of(0, 2, Sort.by("timestamp").descending());
        Pageable secondPage = PageRequest.of(1, 2, Sort.by("timestamp").descending());

        // When
        Page<Transaction> firstPageResult = transactionRepository.findAll(firstPage);
        Page<Transaction> secondPageResult = transactionRepository.findAll(secondPage);

        // Then
        assertThat(firstPageResult.getContent()).hasSize(2);
        assertThat(secondPageResult.getContent()).hasSize(2);
        assertThat(firstPageResult.getTotalElements()).isEqualTo(4);
        assertThat(secondPageResult.getTotalElements()).isEqualTo(4);
        assertThat(firstPageResult.getTotalPages()).isEqualTo(2);
        assertThat(secondPageResult.getTotalPages()).isEqualTo(2);
        
        // Verify no overlap between pages
        assertThat(firstPageResult.getContent()).doesNotContainAnyElementsOf(secondPageResult.getContent());
    }

    @Test
    void testSortingFunctionality() {
        // Given
        Pageable sortByAmountAsc = PageRequest.of(0, 10, Sort.by("amount").ascending());
        Pageable sortByAmountDesc = PageRequest.of(0, 10, Sort.by("amount").descending());

        // When
        Page<Transaction> ascResult = transactionRepository.findAll(sortByAmountAsc);
        Page<Transaction> descResult = transactionRepository.findAll(sortByAmountDesc);

        // Then
        assertThat(ascResult.getContent().get(0).getAmount())
                .isEqualByComparingTo(new BigDecimal("75.25"));
        assertThat(descResult.getContent().get(0).getAmount())
                .isEqualByComparingTo(new BigDecimal("500.00"));
    }
}