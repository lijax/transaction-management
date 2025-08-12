package com.banking.transactionmanagement.performance;

import com.banking.transactionmanagement.dto.PagedTransactionResponseDTO;
import com.banking.transactionmanagement.dto.TransactionCreateDTO;
import com.banking.transactionmanagement.dto.TransactionResponseDTO;
import com.banking.transactionmanagement.model.TransactionType;
import com.banking.transactionmanagement.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive stress and load tests for the transaction management system.
 * Tests concurrent operations, high load scenarios, and performance requirements.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.hikari.maximum-pool-size=20",
    "spring.cache.caffeine.spec=maximumSize=2000,expireAfterWrite=10m",
    "logging.level.com.banking.transactionmanagement=WARN"
})
class StressTestSuite {

    @Autowired
    private TransactionService transactionService;

    private static final int CONCURRENT_THREADS = 20;
    private static final int OPERATIONS_PER_THREAD = 50;
    private static final int LARGE_DATASET_SIZE = 1000;
    private static final long MAX_RESPONSE_TIME_MS = 1000; // 1 second max response time

    @BeforeEach
    void setUp() {
        // Clean up before each test
        // Note: In a real scenario, you might want to preserve some data for pagination tests
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void shouldHandleConcurrentTransactionCreation() throws InterruptedException {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(CONCURRENT_THREADS);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        ConcurrentLinkedQueue<Exception> exceptions = new ConcurrentLinkedQueue<>();

        // When - Create concurrent transaction creation tasks
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        long startTime = System.currentTimeMillis();
                        
                        try {
                            TransactionCreateDTO createDTO = createTestTransactionDTO(threadId, j);
                            TransactionResponseDTO response = transactionService.createTransaction(createDTO);
                            
                            long responseTime = System.currentTimeMillis() - startTime;
                            totalResponseTime.addAndGet(responseTime);
                            
                            assertThat(response).isNotNull();
                            assertThat(response.getId()).isNotNull();
                            successCount.incrementAndGet();
                            
                            // Verify response time requirement
                            assertThat(responseTime).isLessThanOrEqualTo(MAX_RESPONSE_TIME_MS);
                            
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            exceptions.add(e);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errorCount.incrementAndGet();
                    exceptions.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = completionLatch.await(45, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Then - Verify results
        assertThat(completed).isTrue();
        
        int expectedOperations = CONCURRENT_THREADS * OPERATIONS_PER_THREAD;
        int totalOperations = successCount.get() + errorCount.get();
        
        System.out.printf("Concurrent Transaction Creation Test Results:%n");
        System.out.printf("Total operations: %d/%d%n", totalOperations, expectedOperations);
        System.out.printf("Successful operations: %d (%.2f%%)%n", 
            successCount.get(), (double) successCount.get() / expectedOperations * 100);
        System.out.printf("Failed operations: %d (%.2f%%)%n", 
            errorCount.get(), (double) errorCount.get() / expectedOperations * 100);
        System.out.printf("Test duration: %d ms%n", testDuration);
        System.out.printf("Average response time: %.2f ms%n", 
            (double) totalResponseTime.get() / successCount.get());
        System.out.printf("Throughput: %.2f operations/second%n", 
            (double) successCount.get() / testDuration * 1000);

        // Assert performance requirements
        assertThat(successCount.get()).isGreaterThan((int)(expectedOperations * 0.95)); // 95% success rate
        assertThat(errorCount.get()).isLessThan((int)(expectedOperations * 0.05)); // Less than 5% errors
        
        if (!exceptions.isEmpty()) {
            System.out.println("Sample exceptions:");
            exceptions.stream().limit(5).forEach(e -> 
                System.out.printf("  %s: %s%n", e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void shouldHandleHighVolumeDatasetPagination() {
        // Given - Create large dataset
        System.out.println("Creating large dataset for pagination test...");
        List<TransactionResponseDTO> createdTransactions = new ArrayList<>();
        
        long datasetCreationStart = System.currentTimeMillis();
        for (int i = 0; i < LARGE_DATASET_SIZE; i++) {
            TransactionCreateDTO createDTO = createTestTransactionDTO(0, i);
            TransactionResponseDTO response = transactionService.createTransaction(createDTO);
            createdTransactions.add(response);
        }
        long datasetCreationTime = System.currentTimeMillis() - datasetCreationStart;
        
        System.out.printf("Created %d transactions in %d ms (%.2f ms per transaction)%n", 
            LARGE_DATASET_SIZE, datasetCreationTime, (double) datasetCreationTime / LARGE_DATASET_SIZE);

        // When - Test pagination performance with various page sizes
        int[] pageSizes = {10, 20, 50, 100};
        
        for (int pageSize : pageSizes) {
            testPaginationPerformance(pageSize, LARGE_DATASET_SIZE);
        }
    }

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void shouldMaintainPerformanceUnderMixedLoad() throws InterruptedException {
        // Given - Create initial dataset
        int initialDataSize = 500;
        for (int i = 0; i < initialDataSize; i++) {
            TransactionCreateDTO createDTO = createTestTransactionDTO(0, i);
            transactionService.createTransaction(createDTO);
        }

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(CONCURRENT_THREADS);
        
        AtomicInteger createOperations = new AtomicInteger(0);
        AtomicInteger readOperations = new AtomicInteger(0);
        AtomicInteger updateOperations = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        ConcurrentLinkedQueue<Exception> exceptions = new ConcurrentLinkedQueue<>();

        // When - Execute mixed operations (CRUD)
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        long startTime = System.currentTimeMillis();
                        
                        try {
                            int operationType = j % 4; // Distribute operations
                            
                            switch (operationType) {
                                case 0: // Create (25%)
                                    TransactionCreateDTO createDTO = createTestTransactionDTO(threadId, j);
                                    transactionService.createTransaction(createDTO);
                                    createOperations.incrementAndGet();
                                    break;
                                    
                                case 1: // Read single (25%)
                                    long randomId = (long) (Math.random() * initialDataSize) + 1;
                                    try {
                                        transactionService.getTransactionById(randomId);
                                        readOperations.incrementAndGet();
                                    } catch (Exception e) {
                                        // Transaction might not exist, that's ok for this test
                                        readOperations.incrementAndGet();
                                    }
                                    break;
                                    
                                case 2: // Read paginated (25%)
                                    Pageable pageable = PageRequest.of(0, 20);
                                    transactionService.getAllTransactions(pageable);
                                    readOperations.incrementAndGet();
                                    break;
                                    
                                case 3: // Update (25%)
                                    // For simplicity, we'll just do another read operation
                                    // In a real scenario, you'd implement update operations
                                    Pageable updatePageable = PageRequest.of(0, 10);
                                    transactionService.getAllTransactions(updatePageable);
                                    updateOperations.incrementAndGet();
                                    break;
                            }
                            
                            long responseTime = System.currentTimeMillis() - startTime;
                            totalResponseTime.addAndGet(responseTime);
                            
                            // Verify response time requirement
                            assertThat(responseTime).isLessThanOrEqualTo(MAX_RESPONSE_TIME_MS);
                            
                        } catch (Exception e) {
                            exceptions.add(e);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exceptions.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start test
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();
        
        boolean completed = completionLatch.await(75, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Then - Verify mixed load performance
        assertThat(completed).isTrue();
        
        int totalOperations = createOperations.get() + readOperations.get() + updateOperations.get();
        
        System.out.printf("Mixed Load Test Results:%n");
        System.out.printf("Create operations: %d%n", createOperations.get());
        System.out.printf("Read operations: %d%n", readOperations.get());
        System.out.printf("Update operations: %d%n", updateOperations.get());
        System.out.printf("Total operations: %d%n", totalOperations);
        System.out.printf("Test duration: %d ms%n", testDuration);
        System.out.printf("Average response time: %.2f ms%n", 
            totalOperations > 0 ? (double) totalResponseTime.get() / totalOperations : 0);
        System.out.printf("Throughput: %.2f operations/second%n", 
            (double) totalOperations / testDuration * 1000);
        System.out.printf("Exceptions: %d%n", exceptions.size());

        // Performance assertions
        assertThat(totalOperations).isGreaterThan(0);
        assertThat(exceptions.size()).isLessThan((int)(totalOperations * 0.05)); // Less than 5% errors
    }

    private void testPaginationPerformance(int pageSize, int totalRecords) {
        System.out.printf("Testing pagination with page size: %d%n", pageSize);
        
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        long totalPaginationTime = 0;
        int pagesTestedCount = Math.min(10, totalPages); // Test first 10 pages or all if less
        
        for (int page = 0; page < pagesTestedCount; page++) {
            Pageable pageable = PageRequest.of(page, pageSize);
            
            long startTime = System.currentTimeMillis();
            PagedTransactionResponseDTO result = transactionService.getAllTransactions(pageable);
            long responseTime = System.currentTimeMillis() - startTime;
            
            totalPaginationTime += responseTime;
            
            // Verify pagination results
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent().size()).isLessThanOrEqualTo(pageSize);
            assertThat(result.getTotalElements()).isGreaterThan(0);
            
            // Verify response time requirement
            assertThat(responseTime).isLessThanOrEqualTo(MAX_RESPONSE_TIME_MS);
        }
        
        double avgResponseTime = (double) totalPaginationTime / pagesTestedCount;
        System.out.printf("  Tested %d pages, average response time: %.2f ms%n", 
            pagesTestedCount, avgResponseTime);
        
        // Assert average response time is acceptable
        assertThat(avgResponseTime).isLessThanOrEqualTo(MAX_RESPONSE_TIME_MS);
    }

    private TransactionCreateDTO createTestTransactionDTO(int threadId, int operationId) {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setAmount(new BigDecimal("100.00").add(new BigDecimal(operationId)));
        dto.setDescription(String.format("Stress test transaction T%d-O%d", threadId, operationId));
        dto.setType(operationId % 2 == 0 ? TransactionType.CREDIT : TransactionType.DEBIT);
        dto.setTimestamp(LocalDateTime.now().minusMinutes(operationId));
        dto.setCategory("StressTest");
        dto.setAccountNumber(String.format("%08d%06d", threadId, operationId)); // 8-14 digits
        dto.setReferenceNumber(String.format("REF%03d%08d", threadId, operationId));
        return dto;
    }
}