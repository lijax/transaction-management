package com.banking.transactionmanagement.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for the Transaction Management System.
 * Configures Caffeine cache with performance optimizations and eviction policies.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String TRANSACTIONS_CACHE = "transactions";
    public static final String TRANSACTION_BY_ID_CACHE = "transactionById";
    public static final String PAGINATED_TRANSACTIONS_CACHE = "paginatedTransactions";
    public static final String TRANSACTIONS_LIST_CACHE = "transactionsList";

    /**
     * Configures the Caffeine cache manager with optimized settings.
     * 
     * @return CacheManager configured with Caffeine
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configure default cache settings
        cacheManager.setCaffeine(caffeineCacheBuilder());
        
        // Set cache names
        cacheManager.setCacheNames(java.util.Arrays.asList(
            TRANSACTIONS_CACHE,
            TRANSACTION_BY_ID_CACHE,
            PAGINATED_TRANSACTIONS_CACHE,
            TRANSACTIONS_LIST_CACHE
        ));
        
        return cacheManager;
    }

    /**
     * Builds Caffeine cache with performance-optimized settings.
     * 
     * @return Caffeine builder with configured settings
     */
    @Bean
    public Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                // Maximum number of entries in cache
                .maximumSize(1000)
                // Expire entries after write
                .expireAfterWrite(10, TimeUnit.MINUTES)
                // Expire entries after access (for frequently accessed data)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                // Initial capacity for performance
                .initialCapacity(100)
                // Enable statistics for monitoring
                .recordStats()
                // Weak references for keys to allow garbage collection
                .weakKeys();
    }
}