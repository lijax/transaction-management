package com.banking.transactionmanagement.service;

import com.banking.transactionmanagement.config.CacheConfig;
import com.banking.transactionmanagement.model.Transaction;
import com.banking.transactionmanagement.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for warming up caches with frequently accessed data.
 * This improves initial response times by pre-loading commonly requested data.
 */
@Service
public class CacheWarmupService {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmupService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CacheManager cacheManager;

    /**
     * Warms up caches after application startup.
     * This method is triggered when the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCaches() {
        logger.info("Starting cache warmup process...");
        
        try {
            warmupTransactionByIdCache();
            warmupPaginatedTransactionsCache();
            
            logger.info("Cache warmup completed successfully");
        } catch (Exception e) {
            logger.error("Error during cache warmup: {}", e.getMessage(), e);
        }
    }

    /**
     * Warms up the transaction by ID cache with recent transactions.
     */
    private void warmupTransactionByIdCache() {
        Cache transactionByIdCache = cacheManager.getCache(CacheConfig.TRANSACTION_BY_ID_CACHE);
        if (transactionByIdCache == null) {
            logger.warn("Transaction by ID cache not found, skipping warmup");
            return;
        }

        try {
            // Get the most recent 50 transactions to warm up the cache
            Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<Transaction> recentTransactions = transactionRepository.findAll(pageable);
            
            if (recentTransactions == null) {
                logger.warn("Repository returned null for recent transactions, skipping warmup");
                return;
            }
            
            int warmedCount = 0;
            for (Transaction transaction : recentTransactions.getContent()) {
                if (transaction != null && transaction.getId() != null) {
                    transactionByIdCache.put(transaction.getId(), transaction);
                    warmedCount++;
                }
            }
            
            logger.info("Warmed up transaction by ID cache with {} entries", warmedCount);
        } catch (Exception e) {
            logger.warn("Failed to warm up transaction by ID cache: {}", e.getMessage());
        }
    }

    /**
     * Warms up the paginated transactions cache with common page requests.
     */
    private void warmupPaginatedTransactionsCache() {
        Cache paginatedCache = cacheManager.getCache(CacheConfig.PAGINATED_TRANSACTIONS_CACHE);
        if (paginatedCache == null) {
            logger.warn("Paginated transactions cache not found, skipping warmup");
            return;
        }

        // Warm up first few pages with common sorting
        String[] commonSorts = {"timestamp,desc", "amount,desc", "id,asc"};
        int[] commonPageSizes = {10, 20, 50};
        
        int warmedCount = 0;
        for (String sortParam : commonSorts) {
            for (int pageSize : commonPageSizes) {
                try {
                    // Only warm up first 2 pages for each combination
                    for (int page = 0; page < 2; page++) {
                        String cacheKey = generatePaginatedCacheKey(page, pageSize, sortParam);
                        
                        // Create pageable and fetch data
                        Sort sort = createSortFromString(sortParam);
                        Pageable pageable = PageRequest.of(page, pageSize, sort);
                        Page<Transaction> transactions = transactionRepository.findAll(pageable);
                        
                        paginatedCache.put(cacheKey, transactions);
                        warmedCount++;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to warm up cache for sort: {}, pageSize: {}", sortParam, pageSize);
                }
            }
        }
        
        logger.info("Warmed up paginated transactions cache with {} entries", warmedCount);
    }

    /**
     * Generates a cache key for paginated results.
     */
    private String generatePaginatedCacheKey(int page, int size, String sort) {
        return String.format("page:%d:size:%d:sort:%s", page, size, sort);
    }

    /**
     * Creates a Sort object from a string representation.
     */
    private Sort createSortFromString(String sortParam) {
        if (sortParam == null || sortParam.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "timestamp");
        }
        
        String[] parts = sortParam.split(",");
        String property = parts[0];
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1]) 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
            
        return Sort.by(direction, property);
    }

    /**
     * Manually triggers cache warmup (useful for testing or manual refresh).
     */
    public void manualWarmup() {
        logger.info("Manual cache warmup triggered");
        warmupCaches();
    }

    /**
     * Clears all caches (useful for testing or manual refresh).
     */
    public void clearAllCaches() {
        logger.info("Clearing all caches");
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                logger.debug("Cleared cache: {}", cacheName);
            }
        });
        
        logger.info("All caches cleared");
    }
}