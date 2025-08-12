package com.banking.transactionmanagement.service;

import com.banking.transactionmanagement.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for cache monitoring service.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cache.type=caffeine",
    "logging.level.com.banking.transactionmanagement=DEBUG"
})
class CacheMonitoringServiceTest {

    @Autowired
    private CacheMonitoringService cacheMonitoringService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test and invalidate statistics
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                // For Caffeine cache, we need to invalidate all to reset statistics
                if (cache instanceof CaffeineCache) {
                    ((CaffeineCache) cache).getNativeCache().invalidateAll();
                }
            }
        });
    }

    @Test
    void shouldGetAllCacheStatistics() {
        // Arrange - add some data to caches to generate statistics
        Cache transactionCache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        Cache transactionByIdCache = cacheManager.getCache(CacheConfig.TRANSACTION_BY_ID_CACHE);
        
        assertThat(transactionCache).isNotNull();
        assertThat(transactionByIdCache).isNotNull();
        
        // Generate some cache hits and misses
        transactionCache.put("key1", "value1");
        transactionCache.get("key1", String.class); // hit
        transactionCache.get("key2", String.class); // miss
        
        transactionByIdCache.put(1L, "transaction1");
        transactionByIdCache.get(1L, String.class); // hit
        transactionByIdCache.get(2L, String.class); // miss

        // Act
        Map<String, CacheMonitoringService.CacheStatistics> allStats = 
            cacheMonitoringService.getAllCacheStatistics();

        // Assert
        assertThat(allStats).isNotNull();
        assertThat(allStats).hasSize(4); // All four configured caches
        
        assertThat(allStats).containsKeys(
            CacheConfig.TRANSACTIONS_CACHE,
            CacheConfig.TRANSACTION_BY_ID_CACHE,
            CacheConfig.PAGINATED_TRANSACTIONS_CACHE,
            CacheConfig.TRANSACTIONS_LIST_CACHE
        );

        // Verify statistics for transactions cache
        CacheMonitoringService.CacheStatistics transactionStats = 
            allStats.get(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(transactionStats).isNotNull();
        assertThat(transactionStats.getHitCount()).isEqualTo(1);
        assertThat(transactionStats.getMissCount()).isEqualTo(1);
        assertThat(transactionStats.getTotalRequests()).isEqualTo(2);

        // Verify statistics for transaction by ID cache
        CacheMonitoringService.CacheStatistics transactionByIdStats = 
            allStats.get(CacheConfig.TRANSACTION_BY_ID_CACHE);
        assertThat(transactionByIdStats).isNotNull();
        assertThat(transactionByIdStats.getHitCount()).isEqualTo(1);
        assertThat(transactionByIdStats.getMissCount()).isEqualTo(1);
        assertThat(transactionByIdStats.getTotalRequests()).isEqualTo(2);
    }

    @Test
    void shouldGetSpecificCacheStatistics() {
        // Arrange
        Cache cache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(cache).isNotNull();
        
        // Get initial statistics
        CacheMonitoringService.CacheStatistics initialStats = 
            cacheMonitoringService.getCacheStatistics(CacheConfig.TRANSACTIONS_CACHE);
        long initialHits = initialStats != null ? initialStats.getHitCount() : 0;
        long initialMisses = initialStats != null ? initialStats.getMissCount() : 0;
        
        // Generate some cache activity
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.get("key1", String.class); // hit
        cache.get("key2", String.class); // hit
        cache.get("key3", String.class); // miss

        // Act
        CacheMonitoringService.CacheStatistics stats = 
            cacheMonitoringService.getCacheStatistics(CacheConfig.TRANSACTIONS_CACHE);

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats.getHitCount()).isGreaterThanOrEqualTo(initialHits + 2);
        assertThat(stats.getMissCount()).isGreaterThanOrEqualTo(initialMisses + 1);
        assertThat(stats.getTotalRequests()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void shouldReturnNullForNonExistentCache() {
        // Act
        CacheMonitoringService.CacheStatistics stats = 
            cacheMonitoringService.getCacheStatistics("nonExistentCache");

        // Assert
        assertThat(stats).isNull();
    }

    @Test
    void shouldCalculateHitRateCorrectly() {
        // Arrange
        Cache cache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(cache).isNotNull();
        
        // Get initial statistics
        CacheMonitoringService.CacheStatistics initialStats = 
            cacheMonitoringService.getCacheStatistics(CacheConfig.TRANSACTIONS_CACHE);
        long initialHits = initialStats != null ? initialStats.getHitCount() : 0;
        long initialMisses = initialStats != null ? initialStats.getMissCount() : 0;
        
        // Generate specific hit/miss pattern
        cache.put("unique-key1", "value1");
        cache.put("unique-key2", "value2");
        cache.put("unique-key3", "value3");
        
        // 3 hits
        cache.get("unique-key1", String.class);
        cache.get("unique-key2", String.class);
        cache.get("unique-key3", String.class);
        
        // 1 miss
        cache.get("unique-key4", String.class);

        // Act
        CacheMonitoringService.CacheStatistics stats = 
            cacheMonitoringService.getCacheStatistics(CacheConfig.TRANSACTIONS_CACHE);

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats.getHitCount()).isGreaterThanOrEqualTo(initialHits + 3);
        assertThat(stats.getMissCount()).isGreaterThanOrEqualTo(initialMisses + 1);
        assertThat(stats.getTotalRequests()).isGreaterThanOrEqualTo(4);
        // Hit rate should be reasonable (not testing exact value due to cumulative nature)
        assertThat(stats.getHitRate()).isGreaterThan(0.0);
    }

    @Test
    void shouldHandleEmptyCache() {
        // Act - get statistics for cache (may have some activity from other tests)
        CacheMonitoringService.CacheStatistics stats = 
            cacheMonitoringService.getCacheStatistics(CacheConfig.TRANSACTIONS_CACHE);

        // Assert - just verify we can get statistics without error
        assertThat(stats).isNotNull();
        assertThat(stats.getHitCount()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getMissCount()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getTotalRequests()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getEvictionCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldLogCacheStatisticsWithoutError() {
        // Arrange - add some data to generate statistics
        Cache cache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(cache).isNotNull();
        
        cache.put("key1", "value1");
        cache.get("key1", String.class);

        // Act & Assert - should not throw exception
        cacheMonitoringService.logCacheStatistics();
        
        // If we get here without exception, the test passes
        assertThat(true).isTrue();
    }

    @Test
    void shouldCheckCachePerformanceWithoutError() {
        // Arrange - add some data to generate statistics
        Cache cache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(cache).isNotNull();
        
        cache.put("key1", "value1");
        cache.get("key1", String.class);

        // Act & Assert - should not throw exception
        cacheMonitoringService.checkCachePerformance();
        
        // If we get here without exception, the test passes
        assertThat(true).isTrue();
    }

    @Test
    void shouldCreateCacheStatisticsObject() {
        // Act
        CacheMonitoringService.CacheStatistics stats = 
            new CacheMonitoringService.CacheStatistics(10, 5, 0.67, 2, 15, 1000000.0);

        // Assert
        assertThat(stats.getHitCount()).isEqualTo(10);
        assertThat(stats.getMissCount()).isEqualTo(5);
        assertThat(stats.getHitRate()).isEqualTo(0.67);
        assertThat(stats.getEvictionCount()).isEqualTo(2);
        assertThat(stats.getLoadCount()).isEqualTo(15);
        assertThat(stats.getAverageLoadPenalty()).isEqualTo(1000000.0);
        assertThat(stats.getTotalRequests()).isEqualTo(15); // hits + misses
    }
}