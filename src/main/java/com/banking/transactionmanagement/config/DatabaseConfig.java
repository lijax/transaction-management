package com.banking.transactionmanagement.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Database configuration class for optimizing H2 database settings and connection pooling.
 * Provides custom configuration for different environments and performance tuning.
 */
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driverClassName}")
    private String driverClassName;

    /**
     * Custom HikariCP DataSource configuration for production-like performance.
     * This bean is only created when the custom datasource property is enabled and not in test profile.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.database.custom-pool", havingValue = "true", matchIfMissing = true)
    @Profile("!test")
    public DataSource customDataSource() {
        HikariConfig config = new HikariConfig();
        
        // Basic connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        
        // Connection pool optimization
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(20000); // 20 seconds
        config.setIdleTimeout(300000); // 5 minutes
        config.setMaxLifetime(1200000); // 20 minutes
        config.setLeakDetectionThreshold(60000); // 1 minute
        
        // Performance settings
        config.setAutoCommit(true);
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        // Pool naming and monitoring
        config.setPoolName("TransactionManagementPool");
        config.setRegisterMbeans(true);
        
        // H2 specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        return new HikariDataSource(config);
    }


}