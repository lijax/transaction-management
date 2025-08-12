package com.banking.transactionmanagement.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Service for monitoring query performance and collecting metrics
 */
@Service
public class QueryPerformanceMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(QueryPerformanceMonitoringService.class);
    
    private final MeterRegistry meterRegistry;

    @Autowired
    public QueryPerformanceMonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Monitor the execution time of a database operation
     */
    public <T> T monitorQuery(String operationName, Supplier<T> operation) {
        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.nanoTime();
        
        try {
            T result = operation.get();
            long executionTime = System.nanoTime() - startTime;
            
            // Log performance if it exceeds threshold (100ms)
            if (executionTime > 100_000_000) { // 100ms in nanoseconds
                logger.warn("Slow query detected - Operation: {}, Duration: {}ms", 
                           operationName, TimeUnit.NANOSECONDS.toMillis(executionTime));
            } else {
                logger.debug("Query executed - Operation: {}, Duration: {}ms", 
                            operationName, TimeUnit.NANOSECONDS.toMillis(executionTime));
            }
            
            return result;
        } finally {
            sample.stop(Timer.builder("database.query.duration")
                    .description("Database query execution time")
                    .tag("operation", operationName)
                    .register(meterRegistry));
        }
    }

    /**
     * Monitor the execution time of a database operation without return value
     */
    public void monitorQuery(String operationName, Runnable operation) {
        monitorQuery(operationName, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * Record custom performance metrics
     */
    public void recordCustomMetric(String metricName, double value, String... tags) {
        meterRegistry.gauge(metricName, value);
        logger.debug("Custom metric recorded - {}: {}", metricName, value);
    }

    /**
     * Increment a counter metric
     */
    public void incrementCounter(String counterName, String... tags) {
        meterRegistry.counter(counterName, tags).increment();
    }
}