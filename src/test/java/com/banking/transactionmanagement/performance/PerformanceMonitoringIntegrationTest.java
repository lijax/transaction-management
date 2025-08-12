package com.banking.transactionmanagement.performance;

import com.banking.transactionmanagement.dto.TransactionCreateDTO;
import com.banking.transactionmanagement.model.TransactionType;
import com.banking.transactionmanagement.service.QueryPerformanceMonitoringService;
import com.banking.transactionmanagement.service.TransactionService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for performance monitoring functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PerformanceMonitoringIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private QueryPerformanceMonitoringService performanceMonitoringService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void shouldRecordPerformanceMetricsForTransactionCreation() {
        // Given
        TransactionCreateDTO createDTO = new TransactionCreateDTO();
        createDTO.setAmount(new BigDecimal("100.00"));
        createDTO.setDescription("Performance test transaction");
        createDTO.setType(TransactionType.DEBIT);
        createDTO.setTimestamp(LocalDateTime.now());
        createDTO.setCategory("Test");
        createDTO.setAccountNumber("1234567890");
        createDTO.setReferenceNumber("REFPERF001");

        // When
        transactionService.createTransaction(createDTO);

        // Then - Verify that performance metrics were recorded
        Timer createTimer = meterRegistry.find("database.query.duration")
                .tag("operation", "create_transaction")
                .timer();
        
        assertThat(createTimer).isNotNull();
        assertThat(createTimer.count()).isGreaterThan(0);

        // Verify counter metrics
        assertThat(meterRegistry.find("transaction.created")
                .tag("type", "DEBIT")
                .counter()).isNotNull();
    }

    @Test
    void shouldRecordCustomMetrics() {
        // Given
        String metricName = "test.custom.metric";
        double metricValue = 42.0;

        // When
        performanceMonitoringService.recordCustomMetric(metricName, metricValue);

        // Then
        assertThat(meterRegistry.find(metricName).gauge()).isNotNull();
        assertThat(meterRegistry.find(metricName).gauge().value()).isEqualTo(metricValue);
    }

    @Test
    void shouldIncrementCounters() {
        // Given
        String counterName = "test.counter";
        String tagKey = "test.tag";
        String tagValue = "test.value";

        // When
        performanceMonitoringService.incrementCounter(counterName, tagKey, tagValue);
        performanceMonitoringService.incrementCounter(counterName, tagKey, tagValue);

        // Then
        assertThat(meterRegistry.find(counterName)
                .tag(tagKey, tagValue)
                .counter()).isNotNull();
        assertThat(meterRegistry.find(counterName)
                .tag(tagKey, tagValue)
                .counter().count()).isEqualTo(2.0);
    }

    @Test
    void shouldMonitorQueryExecutionTime() {
        // Given
        String operationName = "test.operation";

        // When
        String result = performanceMonitoringService.monitorQuery(operationName, () -> {
            // Simulate some work
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "test result";
        });

        // Then
        assertThat(result).isEqualTo("test result");
        
        Timer operationTimer = meterRegistry.find("database.query.duration")
                .tag("operation", operationName)
                .timer();
        
        assertThat(operationTimer).isNotNull();
        assertThat(operationTimer.count()).isEqualTo(1);
        assertThat(operationTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }
}