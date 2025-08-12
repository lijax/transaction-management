package com.banking.transactionmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Service responsible for database initialization and validation.
 * Performs post-startup checks to ensure database is properly configured and accessible.
 */
@Service
@Profile("!test")
public class DatabaseInitializationService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializationService.class);

    private final DataSource dataSource;

    public DatabaseInitializationService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Validates database initialization after application startup.
     * This method is called when the application is fully started and ready to serve requests.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateDatabaseInitialization() {
        logger.info("Starting database initialization validation...");
        
        try {
            validateDatabaseConnection();
            validateSchemaInitialization();
            validateDataInitialization();
            validateIndexes();
            
            logger.info("Database initialization validation completed successfully");
        } catch (Exception e) {
            logger.error("Database initialization validation failed", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void validateDatabaseConnection() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(5)) {
                throw new SQLException("Database connection is not valid");
            }
            
            String databaseProduct = connection.getMetaData().getDatabaseProductName();
            String databaseVersion = connection.getMetaData().getDatabaseProductVersion();
            
            logger.info("Database connection validated: {} {}", databaseProduct, databaseVersion);
        }
    }

    private void validateSchemaInitialization() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // Check if transactions table exists and has expected structure
            String query = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TRANSACTIONS'";
            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {
                
                if (resultSet.next() && resultSet.getInt(1) == 1) {
                    logger.info("Transactions table schema validated successfully");
                } else {
                    throw new SQLException("Transactions table not found or not properly initialized");
                }
            }
        }
    }

    private void validateDataInitialization() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // Check if sample data was loaded
            String query = "SELECT COUNT(*) FROM transactions";
            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {
                
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    logger.info("Database initialized with {} sample transactions", count);
                } else {
                    logger.warn("No sample data found in transactions table");
                }
            }
        }
    }

    private void validateIndexes() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // Check if indexes were created
            String query = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'TRANSACTIONS'";
            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {
                
                if (resultSet.next()) {
                    int indexCount = resultSet.getInt(1);
                    logger.info("Found {} indexes on transactions table", indexCount);
                    
                    if (indexCount < 5) { // We expect at least 5 indexes (including primary key)
                        logger.warn("Expected more indexes for optimal performance");
                    }
                }
            }
        }
    }

    /**
     * Provides database statistics for monitoring and debugging.
     */
    public DatabaseStats getDatabaseStats() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseStats stats = new DatabaseStats();
            
            // Get transaction count
            String countQuery = "SELECT COUNT(*) FROM transactions";
            try (PreparedStatement statement = connection.prepareStatement(countQuery);
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    stats.setTransactionCount(resultSet.getLong(1));
                }
            }
            
            // Get database size (H2 specific)
            String sizeQuery = "SELECT SUM(FILE_SIZE) FROM INFORMATION_SCHEMA.FILES";
            try (PreparedStatement statement = connection.prepareStatement(sizeQuery);
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    stats.setDatabaseSize(resultSet.getLong(1));
                }
            } catch (SQLException e) {
                // Size query might not work in all H2 modes, ignore
                stats.setDatabaseSize(-1);
            }
            
            return stats;
        } catch (SQLException e) {
            logger.error("Failed to get database statistics", e);
            return new DatabaseStats();
        }
    }

    /**
     * Simple data class to hold database statistics.
     */
    public static class DatabaseStats {
        private long transactionCount;
        private long databaseSize;

        public long getTransactionCount() {
            return transactionCount;
        }

        public void setTransactionCount(long transactionCount) {
            this.transactionCount = transactionCount;
        }

        public long getDatabaseSize() {
            return databaseSize;
        }

        public void setDatabaseSize(long databaseSize) {
            this.databaseSize = databaseSize;
        }
    }
}