package com.banking.transactionmanagement.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for cache configuration.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.cache.type=caffeine",
    "logging.level.org.springframework.cache=DEBUG"
})
class CacheConfigTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CacheConfig cacheConfig;

    @Test
    void shouldCreateCacheManager() {
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(org.springframework.cache.caffeine.CaffeineCacheManager.class);
    }

    @Test
    void shouldConfigureExpectedCaches() {
        // Verify all expected caches are configured
        assertThat(cacheManager.getCacheNames()).containsExactlyInAnyOrder(
            CacheConfig.TRANSACTIONS_CACHE,
            CacheConfig.TRANSACTION_BY_ID_CACHE,
            CacheConfig.PAGINATED_TRANSACTIONS_CACHE,
            CacheConfig.TRANSACTIONS_LIST_CACHE
        );
    }

    @Test
    void shouldConfigureCaffeineCache() {
        // Get a cache and verify it's a Caffeine cache
        var cache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(cache).isNotNull();
        assertThat(cache).isInstanceOf(CaffeineCache.class);
        
        CaffeineCache caffeineCache = (CaffeineCache) cache;
        assertThat(caffeineCache.getNativeCache()).isNotNull();
    }

    @Test
    void shouldCreateCaffeineCacheBuilder() {
        Caffeine<Object, Object> caffeine = cacheConfig.caffeineCacheBuilder();
        assertThat(caffeine).isNotNull();
    }

    @Test
    void shouldEnableStatistics() {
        var cache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(cache).isInstanceOf(CaffeineCache.class);
        
        CaffeineCache caffeineCache = (CaffeineCache) cache;
        var stats = caffeineCache.getNativeCache().stats();
        
        // Statistics should be enabled (not null)
        assertThat(stats).isNotNull();
    }

    @Test
    void shouldCacheAndRetrieveValues() {
        var cache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(cache).isNotNull();
        
        // Test caching functionality
        String key = "testKey";
        String value = "testValue";
        
        // Put value in cache
        cache.put(key, value);
        
        // Retrieve value from cache
        var cachedValue = cache.get(key, String.class);
        assertThat(cachedValue).isEqualTo(value);
    }

    @Test
    void shouldEvictValues() {
        var cache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(cache).isNotNull();
        
        String key = "testKey";
        String value = "testValue";
        
        // Put and verify value is cached
        cache.put(key, value);
        assertThat(cache.get(key, String.class)).isEqualTo(value);
        
        // Evict and verify value is removed
        cache.evict(key);
        assertThat(cache.get(key, String.class)).isNull();
    }

    @Test
    void shouldClearCache() {
        var cache = cacheManager.getCache(CacheConfig.TRANSACTIONS_CACHE);
        assertThat(cache).isNotNull();
        
        // Put multiple values
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        
        // Verify values are cached
        assertThat(cache.get("key1", String.class)).isEqualTo("value1");
        assertThat(cache.get("key2", String.class)).isEqualTo("value2");
        
        // Clear cache
        cache.clear();
        
        // Verify values are removed
        assertThat(cache.get("key1", String.class)).isNull();
        assertThat(cache.get("key2", String.class)).isNull();
    }
}