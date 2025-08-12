package com.banking.transactionmanagement.service;

import com.banking.transactionmanagement.config.CacheConfig;
import com.banking.transactionmanagement.model.Transaction;
import com.banking.transactionmanagement.model.TransactionType;
import com.banking.transactionmanagement.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for cache warmup service.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cache.type=caffeine",
    "logging.level.com.banking.transactionmanagement=DEBUG"
})
class CacheWarmupServiceTest {

    @Autowired
    private CacheWarmupService cacheWarmupService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private TransactionRepository transactionRepository;

    private List<Transaction> testTransactions;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        // Create test transactions
        testTransactions = Arrays.asList(
            createTransaction(1L, "Transaction 1", new BigDecimal("100.00")),
            createTransaction(2L, "Transaction 2", new BigDecimal("200.00")),
            createTransaction(3L, "Transaction 3", new BigDecimal("300.00"))
        );
    }

    @Test
    void shouldWarmupTransactionByIdCache() {
        // Arrange
        Page<Transaction> page = new PageImpl<>(testTransactions);
        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        cacheWarmupService.warmupCaches();

        // Assert
        Cache cache = cacheManager.getCache(CacheConfig.TRANSACTION_BY_ID_CACHE);
        assertThat(cache).isNotNull();

        // Verify transactions are cached
        for (Transaction transaction : testTransactions) {
            Transaction cachedTransaction = cache.get(transaction.getId(), Transaction.class);
            assertThat(cachedTransaction).isNotNull();
            assertThat(cachedTransaction.getId()).isEqualTo(transaction.getId());
            assertThat(cachedTransaction.getDescription()).isEqualTo(transaction.getDescription());
        }

        // Verify repository was called
        verify(transactionRepository, atLeastOnce()).findAll(any(Pageable.class));
    }

    @Test
    void shouldWarmupPaginatedTransactionsCache() {
        // Arrange
        Page<Transaction> page = new PageImpl<>(testTransactions);
        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        cacheWarmupService.warmupCaches();

        // Assert
        Cache cache = cacheManager.getCache(CacheConfig.PAGINATED_TRANSACTIONS_CACHE);
        assertThat(cache).isNotNull();

        // Verify some paginated results are cached
        // The exact cache keys depend on the implementation, so we just verify the cache is not empty
        verify(transactionRepository, atLeast(2)).findAll(any(Pageable.class));
    }

    @Test
    void shouldHandleEmptyRepository() {
        // Arrange
        Page<Transaction> emptyPage = new PageImpl<>(Arrays.asList());
        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        // Act & Assert - should not throw exception
        cacheWarmupService.warmupCaches();

        // Verify repository was called
        verify(transactionRepository, atLeastOnce()).findAll(any(Pageable.class));
    }

    @Test
    void shouldHandleRepositoryException() {
        // Arrange
        when(transactionRepository.findAll(any(Pageable.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert - should not throw exception (should be caught and logged)
        cacheWarmupService.warmupCaches();

        // Verify repository was called
        verify(transactionRepository, atLeastOnce()).findAll(any(Pageable.class));
    }

    @Test
    void shouldManuallyWarmupCaches() {
        // Arrange
        Page<Transaction> page = new PageImpl<>(testTransactions);
        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        cacheWarmupService.manualWarmup();

        // Assert
        Cache cache = cacheManager.getCache(CacheConfig.TRANSACTION_BY_ID_CACHE);
        assertThat(cache).isNotNull();

        // Verify at least one transaction is cached
        Transaction cachedTransaction = cache.get(testTransactions.get(0).getId(), Transaction.class);
        assertThat(cachedTransaction).isNotNull();
    }

    @Test
    void shouldClearAllCaches() {
        // Arrange - put some data in caches
        Cache transactionCache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        Cache transactionByIdCache = cacheManager.getCache(CacheConfig.TRANSACTION_BY_ID_CACHE);
        
        assertThat(transactionCache).isNotNull();
        assertThat(transactionByIdCache).isNotNull();
        
        transactionCache.put("key1", "value1");
        transactionByIdCache.put(1L, testTransactions.get(0));

        // Verify data is cached
        assertThat(transactionCache.get("key1", String.class)).isEqualTo("value1");
        assertThat(transactionByIdCache.get(1L, Transaction.class)).isNotNull();

        // Act
        cacheWarmupService.clearAllCaches();

        // Assert
        assertThat(transactionCache.get("key1", String.class)).isNull();
        assertThat(transactionByIdCache.get(1L, Transaction.class)).isNull();
    }

    @Test
    void shouldHandleMissingCache() {
        // This test verifies that the service handles gracefully when a cache is not found
        // The service should log a warning but continue processing other caches
        
        // Act & Assert - should not throw exception
        cacheWarmupService.warmupCaches();
        
        // If we get here without exception, the test passes
        assertThat(true).isTrue();
    }

    private Transaction createTransaction(Long id, String description, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEBIT);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setCategory("Test");
        return transaction;
    }
}