package com.banking.transactionmanagement.config;

import com.banking.transactionmanagement.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom health indicator for transaction service
 */
@Component
public class TransactionHealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(TransactionHealthIndicator.class);
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionHealthIndicator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionHealthStatus checkHealth() {
        try {
            // Check if we can query the database
            long transactionCount = transactionRepository.count();
            
            Map<String, Object> details = new HashMap<>();
            details.put("transaction_count", transactionCount);
            details.put("database_accessible", true);
            details.put("service", "transaction-management");
            
            return TransactionHealthStatus.up(details);
        } catch (Exception e) {
            logger.error("Transaction service health check failed", e);
            return TransactionHealthStatus.down(e.getMessage());
        }
    }

    /**
     * Simple health status class to represent transaction service health.
     */
    public static class TransactionHealthStatus {
        private final boolean healthy;
        private final String message;
        private final Map<String, Object> details;

        private TransactionHealthStatus(boolean healthy, String message, Map<String, Object> details) {
            this.healthy = healthy;
            this.message = message;
            this.details = details != null ? details : new HashMap<>();
        }

        public static TransactionHealthStatus up(Map<String, Object> details) {
            return new TransactionHealthStatus(true, "UP", details);
        }

        public static TransactionHealthStatus down(String message) {
            Map<String, Object> details = new HashMap<>();
            details.put("error", message);
            return new TransactionHealthStatus(false, "DOWN", details);
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> getDetails() {
            return details;
        }
    }
}