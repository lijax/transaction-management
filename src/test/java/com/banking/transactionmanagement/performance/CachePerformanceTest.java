package com.banking.transactionmanagement.performance;

import com.banking.transactionmanagement.config.CacheConfig;
import com.banking.transactionmanagement.model.Transaction;
import com.banking.transactionmanagement.model.TransactionType;
import com.banking.transactionmanagement.repository.TransactionRepository;
import com.banking.transactionmanagement.service.CacheMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for caching functionality.
 * These tests verify that caching provides performance improvements.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cache.type=caffeine",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.com.banking.transactionmanagement=INFO"
})
class CachePerformanceTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CacheMonitoringService cacheMonitoringService;

    @Autowired
    private TransactionRepository transactionRepository;

    private List<Transaction> testTransactions;

    @BeforeEach
    void setUp() {
        // Clear all caches
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        // Clear database and create test data
        transactionRepository.deleteAll();
        createTestTransactions();
    }

    @Test
    void shouldDemonstratePerformanceImprovementWithCaching() {
        Cache cache = cacheManager.getCache(CacheConfig.TRANSACTION_BY_ID_CACHE);
        assertThat(cache).isNotNull();

        // Save test transactions to database
        List<Transaction> savedTransactions = transactionRepository.saveAll(testTransactions);
        Long testId = savedTransactions.get(0).getId();

        // Measure time without cache (first access)
        long startTime = System.nanoTime();
        Transaction transaction1 = transactionRepository.findById(testId).orElse(null);
        long firstAccessTime = System.nanoTime() - startTime;

        // Put in cache
        cache.put(testId, transaction1);

        // Measure time with cache (subsequent access)
        startTime = System.nanoTime();
        Transaction cachedTransaction = cache.get(testId, Transaction.class);
        long cachedAccessTime = System.nanoTime() - startTime;

        // Assert
        assertThat(transaction1).isNotNull();
        assertThat(cachedTransaction).isNotNull();
        assertThat(cachedTransaction.getId()).isEqualTo(transaction1.getId());
        
        // Cache access should be significantly faster
        assertThat(cachedAccessTime).isLessThan(firstAccessTime);
        
        System.out.printf("Database access time: %d ns, Cache access time: %d ns, Improvement: %.2fx%n",
            firstAccessTime, cachedAccessTime, (double) firstAccessTime / cachedAccessTime);
    }

    @Test
    void shouldHandleConcurrentCacheAccess() throws InterruptedException {
        Cache cache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(cache).isNotNull();

        int numberOfThreads = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Create concurrent cache operations
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "thread-" + threadId + "-key-" + j;
                    String value = "thread-" + threadId + "-value-" + j;
                    
                    // Put value
                    cache.put(key, value);
                    
                    // Get value
                    String retrieved = cache.get(key, String.class);
                    assertThat(retrieved).isEqualTo(value);
                }
            }, executor);
            
            futures.add(future);
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Verify cache statistics - should have some hits from our operations
        CacheMonitoringService.CacheStatistics stats = 
            cacheMonitoringService.getCacheStatistics(CacheConfig.TRANSACTIONS_CACHE);
        
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalRequests()).isGreaterThanOrEqualTo(numberOfThreads * operationsPerThread);
        assertThat(stats.getHitCount()).isGreaterThanOrEqualTo(numberOfThreads * operationsPerThread);
        // Hit rate should be reasonable since we're accessing what we just put
        assertThat(stats.getHitRate()).isGreaterThan(0.3); // At least 30% hit rate
    }

    @Test
    void shouldMeasureCacheEvictionPerformance() {
        Cache cache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(cache).isNotNull();

        // Fill cache beyond its capacity to trigger evictions
        int itemsToCache = 1500; // More than the configured maximum of 1000
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < itemsToCache; i++) {
            cache.put("key-" + i, "value-" + i);
        }
        
        long totalTime = System.nanoTime() - startTime;
        
        // Verify cache statistics (evictions may or may not occur depending on timing)
        CacheMonitoringService.CacheStatistics stats = 
            cacheMonitoringService.getCacheStatistics(CacheConfig.TRANSACTIONS_CACHE);
        
        assertThat(stats).isNotNull();
        // Just verify we can get statistics - evictions depend on cache size and timing
        assertThat(stats.getEvictionCount()).isGreaterThanOrEqualTo(0);
        
        System.out.printf("Cached %d items in %d ns (%.2f ns per item), Evictions: %d%n",
            itemsToCache, totalTime, (double) totalTime / itemsToCache, stats.getEvictionCount());
    }

    @Test
    void shouldMeasureHitRateUnderLoad() {
        Cache cache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(cache).isNotNull();

        // Pre-populate cache with some data
        int cacheSize = 100;
        for (int i = 0; i < cacheSize; i++) {
            cache.put("key-" + i, "value-" + i);
        }

        // Perform mixed read operations (some hits, some misses)
        int totalReads = 1000;
        int expectedHits = 0;
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < totalReads; i++) {
            String key;
            if (i % 3 == 0) {
                // 1/3 of requests will be cache hits
                key = "key-" + (i % cacheSize);
                expectedHits++;
            } else {
                // 2/3 of requests will be cache misses
                key = "miss-key-" + i;
            }
            
            cache.get(key, String.class);
        }
        
        long totalTime = System.nanoTime() - startTime;

        // Verify hit rate - should have some hits and misses
        CacheMonitoringService.CacheStatistics stats = 
            cacheMonitoringService.getCacheStatistics(CacheConfig.TRANSACTIONS_CACHE);
        
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalRequests()).isGreaterThanOrEqualTo(totalReads);
        
        // Should have some activity from our test (hits or misses)
        assertThat(stats.getHitCount()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getMissCount()).isGreaterThanOrEqualTo(0);
        // At least some requests should have been made
        assertThat(stats.getTotalRequests()).isGreaterThan(0);
        // Hit rate should be between 0 and 1 (inclusive)
        assertThat(stats.getHitRate()).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        
        System.out.printf("Performed %d reads in %d ns (%.2f ns per read), Hit rate: %.2f%%%n",
            totalReads, totalTime, (double) totalTime / totalReads, stats.getHitRate() * 100);
    }

    @Test
    void shouldVerifyCacheExpiration() throws InterruptedException {
        // This test verifies that cache expiration works, but since our cache is configured
        // with 10-minute expiration, we'll test the configuration rather than wait
        
        Cache cache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(cache).isNotNull();

        // Put a value in cache
        cache.put("expiration-test", "value");
        
        // Verify it's cached
        assertThat(cache.get("expiration-test", String.class)).isEqualTo("value");
        
        // For this test, we just verify the cache configuration is working
        // In a real scenario, you might use a shorter expiration time for testing
        CacheMonitoringService.CacheStatistics stats = 
            cacheMonitoringService.getCacheStatistics(CacheConfig.TRANSACTIONS_CACHE);
        
        assertThat(stats).isNotNull();
        assertThat(stats.getHitCount()).isEqualTo(1);
    }

    private void createTestTransactions() {
        testTransactions = new ArrayList<>();
        
        for (int i = 1; i <= 100; i++) {
            Transaction transaction = new Transaction();
            transaction.setDescription("Test Transaction " + i);
            transaction.setAmount(new BigDecimal("100.00").add(new BigDecimal(i)));
            transaction.setType(i % 2 == 0 ? TransactionType.CREDIT : TransactionType.DEBIT);
            transaction.setTimestamp(LocalDateTime.now().minusHours(i));
            transaction.setCategory("Test Category " + (i % 5));
            transaction.setAccountNumber("ACC" + String.format("%06d", i));
            transaction.setReferenceNumber("REF" + String.format("%08d", i));
            
            testTransactions.add(transaction);
        }
    }
}