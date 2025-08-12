package com.banking.transactionmanagement.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.banking.transactionmanagement.config.JvmPerformanceConfig.JvmMemoryHealthStatus;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JVM performance configuration
 */
@SpringBootTest
@ActiveProfiles("test")
class JvmPerformanceConfigTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private JvmPerformanceConfig jvmPerformanceConfig;

    @Test
    void shouldRegisterJvmMetrics() {
        // Verify that JVM metrics are registered
        assertThat(meterRegistry.find("jvm.memory.used").gauge()).isNotNull();
        assertThat(meterRegistry.find("jvm.gc.pause").timer()).isNotNull();
        assertThat(meterRegistry.find("jvm.threads.live").gauge()).isNotNull();
        assertThat(meterRegistry.find("jvm.classes.loaded").gauge()).isNotNull();
        assertThat(meterRegistry.find("system.cpu.usage").gauge()).isNotNull();
        assertThat(meterRegistry.find("process.uptime").gauge()).isNotNull();
    }

    @Test
    void shouldProvideJvmMemoryHealthCheck() {
        // When
        JvmMemoryHealthStatus health = jvmPerformanceConfig.getJvmMemoryHealth();

        // Then
        assertThat(health).isNotNull();
        assertThat(health.getDetails()).containsKeys(
                "heap.used", 
                "heap.max", 
                "heap.usage.percent",
                "non-heap.used",
                "non-heap.max",
                "non-heap.usage.percent"
        );

        // Verify that memory values are reasonable
        String heapUsed = (String) health.getDetails().get("heap.used");
        String heapMax = (String) health.getDetails().get("heap.max");
        String heapUsagePercent = (String) health.getDetails().get("heap.usage.percent");

        assertThat(heapUsed).isNotNull().contains("B");
        assertThat(heapMax).isNotNull().contains("B");
        assertThat(heapUsagePercent).isNotNull().contains("%");
    }

    @Test
    void shouldHaveHealthyMemoryUsageInTestEnvironment() {
        // When
        JvmMemoryHealthStatus health = jvmPerformanceConfig.getJvmMemoryHealth();

        // Then - In test environment, memory usage should be healthy
        assertThat(health.isHealthy()).isTrue();
        
        String heapUsagePercent = (String) health.getDetails().get("heap.usage.percent");
        double usagePercent = Double.parseDouble(heapUsagePercent.replace("%", ""));
        
        // Memory usage should be reasonable in test environment
        assertThat(usagePercent).isLessThan(90.0);
    }
}