package com.banking.transactionmanagement.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for JVM performance monitoring and optimization
 */
@Configuration
public class JvmPerformanceConfig {

    private static final Logger logger = LoggerFactory.getLogger(JvmPerformanceConfig.class);

    private final MeterRegistry meterRegistry;

    @Autowired
    public JvmPerformanceConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initializeJvmMetrics() {
        // Register JVM metrics
        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);
        new ClassLoaderMetrics().bindTo(meterRegistry);
        new ProcessorMetrics().bindTo(meterRegistry);
        new UptimeMetrics().bindTo(meterRegistry);

        logJvmConfiguration();
    }

    /**
     * Get JVM memory health status
     */
    public JvmMemoryHealthStatus getJvmMemoryHealth() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapMemoryUsage = memoryBean.getNonHeapMemoryUsage();

            long heapUsed = heapMemoryUsage.getUsed();
            long heapMax = heapMemoryUsage.getMax();
            long nonHeapUsed = nonHeapMemoryUsage.getUsed();
            long nonHeapMax = nonHeapMemoryUsage.getMax();

            double heapUsagePercent = (double) heapUsed / heapMax * 100;
            double nonHeapUsagePercent = nonHeapMax > 0 ? (double) nonHeapUsed / nonHeapMax * 100 : 0;

            Map<String, Object> details = new HashMap<>();
            details.put("heap.used", formatBytes(heapUsed));
            details.put("heap.max", formatBytes(heapMax));
            details.put("heap.usage.percent", String.format("%.2f%%", heapUsagePercent));
            details.put("non-heap.used", formatBytes(nonHeapUsed));
            details.put("non-heap.max", nonHeapMax > 0 ? formatBytes(nonHeapMax) : "unlimited");
            details.put("non-heap.usage.percent", String.format("%.2f%%", nonHeapUsagePercent));

            // Mark as down if heap usage is above 90%
            if (heapUsagePercent > 90) {
                details.put("reason", "High heap memory usage");
                return JvmMemoryHealthStatus.down("High heap memory usage", details);
            }

            return JvmMemoryHealthStatus.up(details);
        } catch (Exception e) {
            return JvmMemoryHealthStatus.down(e.getMessage(), new HashMap<>());
        }
    }

    private void logJvmConfiguration() {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        logger.info("=== JVM Performance Configuration ===");
        logger.info("Available processors: {}", runtime.availableProcessors());
        logger.info("Max memory: {}", formatBytes(runtime.maxMemory()));
        logger.info("Total memory: {}", formatBytes(runtime.totalMemory()));
        logger.info("Free memory: {}", formatBytes(runtime.freeMemory()));
        logger.info("Heap memory usage: {}", memoryBean.getHeapMemoryUsage());
        logger.info("Non-heap memory usage: {}", memoryBean.getNonHeapMemoryUsage());
        
        // Log JVM arguments
        ManagementFactory.getRuntimeMXBean().getInputArguments().forEach(arg -> 
            logger.info("JVM argument: {}", arg));
        
        logger.info("=====================================");
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Simple health status class for JVM memory health.
     */
    public static class JvmMemoryHealthStatus {
        private final boolean healthy;
        private final String message;
        private final Map<String, Object> details;

        private JvmMemoryHealthStatus(boolean healthy, String message, Map<String, Object> details) {
            this.healthy = healthy;
            this.message = message;
            this.details = details != null ? details : new HashMap<>();
        }

        public static JvmMemoryHealthStatus up(Map<String, Object> details) {
            return new JvmMemoryHealthStatus(true, "UP", details);
        }

        public static JvmMemoryHealthStatus down(String message, Map<String, Object> details) {
            return new JvmMemoryHealthStatus(false, message, details);
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