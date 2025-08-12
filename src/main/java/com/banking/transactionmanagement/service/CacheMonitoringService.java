package com.banking.transactionmanagement.service;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for monitoring cache performance and statistics.
 * Provides insights into cache hit rates, evictions, and overall performance.
 */
@Service
public class CacheMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(CacheMonitoringService.class);

    @Autowired
    private CacheManager cacheManager;

    /**
     * Logs cache statistics every 5 minutes.
     * This helps monitor cache performance and identify optimization opportunities.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logCacheStatistics() {
        logger.info("=== Cache Statistics Report ===");
        
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                CaffeineCache caffeineCache = (CaffeineCache) cache;
                CacheStats stats = caffeineCache.getNativeCache().stats();
                
                logCacheStats(cacheName, stats);
            }
        }
        
        logger.info("=== End Cache Statistics Report ===");
    }

    /**
     * Gets cache statistics for all caches.
     * 
     * @return Map of cache names to their statistics
     */
    public Map<String, CacheStatistics> getAllCacheStatistics() {
        Map<String, CacheStatistics> allStats = new HashMap<>();
        
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                CaffeineCache caffeineCache = (CaffeineCache) cache;
                CacheStats stats = caffeineCache.getNativeCache().stats();
                
                allStats.put(cacheName, new CacheStatistics(
                    stats.hitCount(),
                    stats.missCount(),
                    stats.hitRate(),
                    stats.evictionCount(),
                    stats.loadCount(),
                    stats.averageLoadPenalty()
                ));
            }
        }
        
        return allStats;
    }

    /**
     * Gets statistics for a specific cache.
     * 
     * @param cacheName the name of the cache
     * @return CacheStatistics or null if cache not found
     */
    public CacheStatistics getCacheStatistics(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof CaffeineCache) {
            CaffeineCache caffeineCache = (CaffeineCache) cache;
            CacheStats stats = caffeineCache.getNativeCache().stats();
            
            return new CacheStatistics(
                stats.hitCount(),
                stats.missCount(),
                stats.hitRate(),
                stats.evictionCount(),
                stats.loadCount(),
                stats.averageLoadPenalty()
            );
        }
        
        return null;
    }

    /**
     * Logs detailed statistics for a specific cache.
     */
    private void logCacheStats(String cacheName, CacheStats stats) {
        logger.info("Cache: {} | Hits: {} | Misses: {} | Hit Rate: {:.2f}% | Evictions: {} | Load Count: {} | Avg Load Time: {:.2f}ms",
            cacheName,
            stats.hitCount(),
            stats.missCount(),
            stats.hitRate() * 100,
            stats.evictionCount(),
            stats.loadCount(),
            stats.averageLoadPenalty() / 1_000_000.0 // Convert nanoseconds to milliseconds
        );
    }

    /**
     * Checks if any cache has a low hit rate and logs warnings.
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void checkCachePerformance() {
        for (String cacheName : cacheManager.getCacheNames()) {
            CacheStatistics stats = getCacheStatistics(cacheName);
            if (stats != null) {
                // Warn if hit rate is below 70%
                if (stats.getHitRate() < 0.7 && stats.getTotalRequests() > 100) {
                    logger.warn("Cache {} has low hit rate: {:.2f}% (Total requests: {})", 
                        cacheName, stats.getHitRate() * 100, stats.getTotalRequests());
                }
                
                // Warn if eviction rate is high
                if (stats.getEvictionCount() > stats.getHitCount() * 0.1) {
                    logger.warn("Cache {} has high eviction rate: {} evictions vs {} hits", 
                        cacheName, stats.getEvictionCount(), stats.getHitCount());
                }
            }
        }
    }

    /**
     * Data class to hold cache statistics.
     */
    public static class CacheStatistics {
        private final long hitCount;
        private final long missCount;
        private final double hitRate;
        private final long evictionCount;
        private final long loadCount;
        private final double averageLoadPenalty;

        public CacheStatistics(long hitCount, long missCount, double hitRate, 
                             long evictionCount, long loadCount, double averageLoadPenalty) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
            this.evictionCount = evictionCount;
            this.loadCount = loadCount;
            this.averageLoadPenalty = averageLoadPenalty;
        }

        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public double getHitRate() { return hitRate; }
        public long getEvictionCount() { return evictionCount; }
        public long getLoadCount() { return loadCount; }
        public double getAverageLoadPenalty() { return averageLoadPenalty; }
        public long getTotalRequests() { return hitCount + missCount; }
    }
}