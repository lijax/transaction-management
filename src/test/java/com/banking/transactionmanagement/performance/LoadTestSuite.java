package com.banking.transactionmanagement.performance;

import com.banking.transactionmanagement.dto.TransactionCreateDTO;
import com.banking.transactionmanagement.dto.TransactionResponseDTO;
import com.banking.transactionmanagement.model.TransactionType;
import com.banking.transactionmanagement.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.banking.transactionmanagement.dto.PagedTransactionResponseDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive load tests focusing on response time verification and system behavior under load.
 * Tests various load patterns and measures performance against requirements.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.hikari.maximum-pool-size=25",
    "spring.cache.caffeine.spec=maximumSize=2000,expireAfterWrite=10m",
    "logging.level.com.banking.transactionmanagement=WARN",
    "spring.jpa.properties.hibernate.jdbc.batch_size=50"
})
class LoadTestSuite {

    @Autowired
    private TransactionService transactionService;

    // Performance requirements (from requirements 5.1, 8.3, 8.5)
    private static final long MAX_RESPONSE_TIME_MS = 1000; // 1 second
    private static final long ACCEPTABLE_RESPONSE_TIME_MS = 500; // 500ms for 95th percentile
    private static final double MIN_SUCCESS_RATE = 0.95; // 95% success rate
    private static final int SUSTAINED_LOAD_DURATION_SECONDS = 30;

    @BeforeEach
    void setUp() {
        // Clean state for each test
        System.gc();
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void shouldMeetResponseTimeRequirementsUnderLoad() throws InterruptedException {
        // Given - Load test configuration
        int numberOfThreads = 15;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
        
        ConcurrentLinkedQueue<Long> responseTimes = new ConcurrentLinkedQueue<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // When - Execute load test
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        long startTime = System.nanoTime();
                        
                        try {
                            TransactionCreateDTO createDTO = createLoadTestTransaction(threadId, j);
                            TransactionResponseDTO response = transactionService.createTransaction(createDTO);
                            
                            long responseTimeNs = System.nanoTime() - startTime;
                            long responseTimeMs = responseTimeNs / 1_000_000;
                            responseTimes.add(responseTimeMs);
                            
                            assertThat(response).isNotNull();
                            assertThat(response.getId()).isNotNull();
                            successCount.incrementAndGet();
                            
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.printf("Error in thread %d, operation %d: %s%n", 
                                threadId, j, e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errorCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = completionLatch.await(90, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Then - Analyze response times
        assertThat(completed).isTrue();
        
        List<Long> sortedResponseTimes = responseTimes.stream()
            .sorted()
            .collect(Collectors.toList());
        
        ResponseTimeStatistics stats = calculateResponseTimeStatistics(sortedResponseTimes);
        
        System.out.printf("Load Test Response Time Results:%n");
        System.out.printf("Total operations: %d%n", successCount.get() + errorCount.get());
        System.out.printf("Successful operations: %d%n", successCount.get());
        System.out.printf("Failed operations: %d%n", errorCount.get());
        System.out.printf("Success rate: %.2f%%%n", (double) successCount.get() / (successCount.get() + errorCount.get()) * 100);
        System.out.printf("Test duration: %d ms%n", testDuration);
        System.out.printf("Throughput: %.2f ops/sec%n", (double) successCount.get() / testDuration * 1000);
        System.out.printf("Response time statistics:%n");
        System.out.printf("  Min: %d ms%n", stats.min);
        System.out.printf("  Max: %d ms%n", stats.max);
        System.out.printf("  Average: %.2f ms%n", stats.average);
        System.out.printf("  Median (50th): %d ms%n", stats.p50);
        System.out.printf("  95th percentile: %d ms%n", stats.p95);
        System.out.printf("  99th percentile: %d ms%n", stats.p99);

        // Verify performance requirements
        double successRate = (double) successCount.get() / (successCount.get() + errorCount.get());
        assertThat(successRate).isGreaterThanOrEqualTo(MIN_SUCCESS_RATE);
        assertThat(stats.p95).isLessThanOrEqualTo(ACCEPTABLE_RESPONSE_TIME_MS);
        assertThat(stats.max).isLessThanOrEqualTo(MAX_RESPONSE_TIME_MS);
        assertThat(stats.average).isLessThanOrEqualTo(ACCEPTABLE_RESPONSE_TIME_MS);
    }

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void shouldHandlePaginationLoadEfficiently() {
        // Given - Create large dataset for pagination testing
        int datasetSize = 1000;
        System.out.printf("Creating dataset of %d transactions for pagination load test...%n", datasetSize);
        
        long datasetCreationStart = System.currentTimeMillis();
        for (int i = 0; i < datasetSize; i++) {
            TransactionCreateDTO createDTO = createLoadTestTransaction(0, i);
            transactionService.createTransaction(createDTO);
        }
        long datasetCreationTime = System.currentTimeMillis() - datasetCreationStart;
        System.out.printf("Dataset created in %d ms%n", datasetCreationTime);

        // When - Test pagination under concurrent load
        int numberOfThreads = 10;
        int paginationRequestsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);
        
        ConcurrentLinkedQueue<Long> paginationResponseTimes = new ConcurrentLinkedQueue<>();
        AtomicInteger successfulPaginationRequests = new AtomicInteger(0);
        AtomicInteger failedPaginationRequests = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random(threadId);
                    
                    for (int j = 0; j < paginationRequestsPerThread; j++) {
                        long startTime = System.nanoTime();
                        
                        try {
                            // Vary page size and page number
                            int pageSize = 10 + random.nextInt(40); // 10-50 items per page
                            int maxPage = Math.max(0, (datasetSize / pageSize) - 1);
                            int pageNumber = random.nextInt(maxPage + 1);
                            
                            Pageable pageable = PageRequest.of(pageNumber, pageSize, 
                                Sort.by(Sort.Direction.DESC, "timestamp"));
                            
                            PagedTransactionResponseDTO page = transactionService.getAllTransactions(pageable);
                            
                            long responseTimeNs = System.nanoTime() - startTime;
                            long responseTimeMs = responseTimeNs / 1_000_000;
                            paginationResponseTimes.add(responseTimeMs);
                            
                            // Verify pagination results
                            assertThat(page).isNotNull();
                            assertThat(page.getContent()).isNotNull();
                            assertThat(page.getContent().size()).isLessThanOrEqualTo(pageSize);
                            assertThat(page.getTotalElements()).isGreaterThan(0);
                            
                            successfulPaginationRequests.incrementAndGet();
                            
                        } catch (Exception e) {
                            failedPaginationRequests.incrementAndGet();
                            System.err.printf("Pagination error in thread %d: %s%n", threadId, e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failedPaginationRequests.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start pagination load test
        long paginationTestStart = System.currentTimeMillis();
        startLatch.countDown();
        
        boolean completed;
        try {
            completed = completionLatch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted", e);
        }
        long paginationTestDuration = System.currentTimeMillis() - paginationTestStart;
        
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - Analyze pagination performance
        assertThat(completed).isTrue();
        
        List<Long> sortedPaginationTimes = paginationResponseTimes.stream()
            .sorted()
            .collect(Collectors.toList());
        
        ResponseTimeStatistics paginationStats = calculateResponseTimeStatistics(sortedPaginationTimes);
        
        System.out.printf("Pagination Load Test Results:%n");
        System.out.printf("Dataset size: %d transactions%n", datasetSize);
        System.out.printf("Successful pagination requests: %d%n", successfulPaginationRequests.get());
        System.out.printf("Failed pagination requests: %d%n", failedPaginationRequests.get());
        System.out.printf("Pagination test duration: %d ms%n", paginationTestDuration);
        System.out.printf("Pagination throughput: %.2f requests/sec%n", 
            (double) successfulPaginationRequests.get() / paginationTestDuration * 1000);
        System.out.printf("Pagination response times:%n");
        System.out.printf("  Average: %.2f ms%n", paginationStats.average);
        System.out.printf("  95th percentile: %d ms%n", paginationStats.p95);
        System.out.printf("  Max: %d ms%n", paginationStats.max);

        // Verify pagination performance requirements
        double paginationSuccessRate = (double) successfulPaginationRequests.get() / 
            (successfulPaginationRequests.get() + failedPaginationRequests.get());
        assertThat(paginationSuccessRate).isGreaterThanOrEqualTo(MIN_SUCCESS_RATE);
        assertThat(paginationStats.p95).isLessThanOrEqualTo(ACCEPTABLE_RESPONSE_TIME_MS);
        assertThat(paginationStats.max).isLessThanOrEqualTo(MAX_RESPONSE_TIME_MS);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void shouldMaintainPerformanceUnderSustainedLoad() throws InterruptedException {
        // Given - Sustained load configuration
        int numberOfThreads = 8;
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> sustainedResponseTimes = new ConcurrentLinkedQueue<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        
        // When - Run sustained load for specified duration
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    long endTime = System.currentTimeMillis() + (SUSTAINED_LOAD_DURATION_SECONDS * 1000);
                    int operationCount = 0;
                    
                    while (System.currentTimeMillis() < endTime) {
                        long startTime = System.nanoTime();
                        
                        try {
                            TransactionCreateDTO createDTO = createLoadTestTransaction(threadId, operationCount);
                            TransactionResponseDTO response = transactionService.createTransaction(createDTO);
                            
                            long responseTimeNs = System.nanoTime() - startTime;
                            long responseTimeMs = responseTimeNs / 1_000_000;
                            sustainedResponseTimes.add(responseTimeMs);
                            
                            assertThat(response).isNotNull();
                            successfulOperations.incrementAndGet();
                            
                        } catch (Exception e) {
                            System.err.printf("Sustained load error in thread %d: %s%n", threadId, e.getMessage());
                        }
                        
                        totalOperations.incrementAndGet();
                        operationCount++;
                        
                        // Small delay to prevent overwhelming the system
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Start sustained load test
        System.out.printf("Starting sustained load test for %d seconds...%n", SUSTAINED_LOAD_DURATION_SECONDS);
        long sustainedTestStart = System.currentTimeMillis();
        startLatch.countDown();
        
        // Monitor progress
        for (int i = 0; i < SUSTAINED_LOAD_DURATION_SECONDS; i += 5) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            System.out.printf("Sustained load progress: %d/%d seconds, operations: %d%n", 
                i + 5, SUSTAINED_LOAD_DURATION_SECONDS, totalOperations.get());
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        long actualTestDuration = System.currentTimeMillis() - sustainedTestStart;

        // Then - Analyze sustained load performance
        List<Long> sortedSustainedTimes = sustainedResponseTimes.stream()
            .sorted()
            .collect(Collectors.toList());
        
        ResponseTimeStatistics sustainedStats = calculateResponseTimeStatistics(sortedSustainedTimes);
        
        System.out.printf("Sustained Load Test Results:%n");
        System.out.printf("Test duration: %d ms (target: %d ms)%n", 
            actualTestDuration, SUSTAINED_LOAD_DURATION_SECONDS * 1000);
        System.out.printf("Total operations: %d%n", totalOperations.get());
        System.out.printf("Successful operations: %d%n", successfulOperations.get());
        System.out.printf("Success rate: %.2f%%%n", 
            (double) successfulOperations.get() / totalOperations.get() * 100);
        System.out.printf("Average throughput: %.2f ops/sec%n", 
            (double) successfulOperations.get() / actualTestDuration * 1000);
        System.out.printf("Sustained response times:%n");
        System.out.printf("  Average: %.2f ms%n", sustainedStats.average);
        System.out.printf("  95th percentile: %d ms%n", sustainedStats.p95);
        System.out.printf("  Max: %d ms%n", sustainedStats.max);

        // Verify sustained load requirements
        double sustainedSuccessRate = (double) successfulOperations.get() / totalOperations.get();
        assertThat(sustainedSuccessRate).isGreaterThanOrEqualTo(MIN_SUCCESS_RATE);
        assertThat(sustainedStats.p95).isLessThanOrEqualTo(ACCEPTABLE_RESPONSE_TIME_MS);
        assertThat(sustainedStats.average).isLessThanOrEqualTo(ACCEPTABLE_RESPONSE_TIME_MS);
        assertThat(totalOperations.get()).isGreaterThan(0);
    }

    private TransactionCreateDTO createLoadTestTransaction(int threadId, int operationId) {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setAmount(new BigDecimal("75.50").add(new BigDecimal(operationId % 1000)));
        dto.setDescription(String.format("Load test transaction T%d-O%d", threadId, operationId));
        dto.setType(operationId % 3 == 0 ? TransactionType.TRANSFER : 
                   (operationId % 2 == 0 ? TransactionType.CREDIT : TransactionType.DEBIT));
        dto.setTimestamp(LocalDateTime.now().minusMinutes(operationId % 1440)); // Spread over 24 hours
        dto.setCategory("LoadTest-Cat" + (operationId % 5));
        dto.setAccountNumber(String.format("%08d%07d", threadId, operationId)); // 8-15 digits
        dto.setReferenceNumber(String.format("LOADREF%03d%09d", threadId, operationId));
        return dto;
    }

    private ResponseTimeStatistics calculateResponseTimeStatistics(List<Long> sortedResponseTimes) {
        if (sortedResponseTimes.isEmpty()) {
            return new ResponseTimeStatistics(0, 0, 0, 0, 0, 0, 0);
        }
        
        int size = sortedResponseTimes.size();
        long min = sortedResponseTimes.get(0);
        long max = sortedResponseTimes.get(size - 1);
        double average = sortedResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        
        long p50 = sortedResponseTimes.get(size * 50 / 100);
        long p95 = sortedResponseTimes.get(Math.min(size - 1, size * 95 / 100));
        long p99 = sortedResponseTimes.get(Math.min(size - 1, size * 99 / 100));
        
        return new ResponseTimeStatistics(min, max, average, p50, p95, p99, size);
    }

    private static class ResponseTimeStatistics {
        final long min;
        final long max;
        final double average;
        final long p50;
        final long p95;
        final long p99;
        final int count;

        ResponseTimeStatistics(long min, long max, double average, long p50, long p95, long p99, int count) {
            this.min = min;
            this.max = max;
            this.average = average;
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
            this.count = count;
        }
    }
}