package com.banking.transactionmanagement.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Database monitoring service for connectivity and performance monitoring.
 * Provides detailed information about database status, connection pool, and basic performance metrics.
 */
@Component
public class DatabaseHealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Checks database health and returns status information.
     */
    public DatabaseHealthStatus checkHealth() {
        try {
            return checkDatabaseHealth();
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            return DatabaseHealthStatus.down(e.getMessage());
        }
    }

    private DatabaseHealthStatus checkDatabaseHealth() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // Test basic connectivity
            if (!connection.isValid(5)) {
                return DatabaseHealthStatus.down("Database connection is not valid");
            }

            // Get database metadata
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            String databaseProductVersion = connection.getMetaData().getDatabaseProductVersion();
            String driverName = connection.getMetaData().getDriverName();
            String driverVersion = connection.getMetaData().getDriverVersion();

            // Test query execution and get transaction count
            long transactionCount = getTransactionCount(connection);
            
            // Get connection pool information (if available)
            String connectionPoolInfo = getConnectionPoolInfo();

            Map<String, Object> details = new HashMap<>();
            details.put("database", databaseProductName);
            details.put("version", databaseProductVersion);
            details.put("driver", driverName + " " + driverVersion);
            details.put("transactionCount", transactionCount);
            details.put("connectionPool", connectionPoolInfo);
            details.put("status", "Database is accessible and responsive");

            return DatabaseHealthStatus.up(details);
        }
    }

    private long getTransactionCount(Connection connection) throws SQLException {
        String query = "SELECT COUNT(*) FROM transactions";
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            // If transactions table doesn't exist yet, return -1 to indicate schema not initialized
            return -1;
        }
    }

    private String getConnectionPoolInfo() {
        try {
            // Try to get HikariCP information if available
            if (dataSource.getClass().getName().contains("HikariDataSource")) {
                return "HikariCP connection pool active";
            }
            return "Connection pool information not available";
        } catch (Exception e) {
            return "Unable to determine connection pool status";
        }
    }

    /**
     * Simple health status class to represent database health.
     */
    public static class DatabaseHealthStatus {
        private final boolean healthy;
        private final String message;
        private final Map<String, Object> details;

        private DatabaseHealthStatus(boolean healthy, String message, Map<String, Object> details) {
            this.healthy = healthy;
            this.message = message;
            this.details = details != null ? details : new HashMap<>();
        }

        public static DatabaseHealthStatus up(Map<String, Object> details) {
            return new DatabaseHealthStatus(true, "UP", details);
        }

        public static DatabaseHealthStatus down(String message) {
            Map<String, Object> details = new HashMap<>();
            details.put("error", message);
            return new DatabaseHealthStatus(false, "DOWN", details);
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