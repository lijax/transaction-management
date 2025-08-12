package com.banking.transactionmanagement.controller;

import com.banking.transactionmanagement.dto.TransactionCreateDTO;
import com.banking.transactionmanagement.dto.TransactionUpdateDTO;
import com.banking.transactionmanagement.model.Transaction;
import com.banking.transactionmanagement.model.TransactionType;
import com.banking.transactionmanagement.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TransactionController.
 * Tests all REST endpoints with real HTTP requests and database interactions.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("TransactionController Integration Tests")
class TransactionControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TransactionRepository transactionRepository;

    private ObjectMapper objectMapper;
    private LocalDateTime fixedTimestamp;
    private DateTimeFormatter formatter;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        fixedTimestamp = LocalDateTime.now().minusHours(2);
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        
        // Clean database before each test
        transactionRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/v1/transactions - Create Transaction Tests")
    class CreateTransactionTests {

        @Test
        @DisplayName("Should create transaction with valid data and return 201")
        void createTransaction_ValidData_Returns201() throws Exception {
            // Arrange
            TransactionCreateDTO createDTO = new TransactionCreateDTO();
            createDTO.setAmount(new BigDecimal("150.75"));
            createDTO.setDescription("Grocery shopping");
            createDTO.setType(TransactionType.DEBIT);
            createDTO.setTimestamp(fixedTimestamp);
            createDTO.setCategory("Food");
            createDTO.setAccountNumber("1234567890");
            createDTO.setReferenceNumber("REF123");

            // Act & Assert
            mockMvc.perform(post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.amount").value(150.75))
                    .andExpect(jsonPath("$.description").value("Grocery shopping"))
                    .andExpect(jsonPath("$.type").value("DEBIT"))
                    .andExpect(jsonPath("$.timestamp").value(fixedTimestamp.format(formatter)))
                    .andExpect(jsonPath("$.category").value("Food"))
                    .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                    .andExpect(jsonPath("$.referenceNumber").value("REF123"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("Should return 400 when amount is missing")
        void createTransaction_MissingAmount_Returns400() throws Exception {
            // Arrange
            TransactionCreateDTO createDTO = new TransactionCreateDTO();
            createDTO.setDescription("Transaction without amount");
            createDTO.setType(TransactionType.DEBIT);
            createDTO.setTimestamp(fixedTimestamp);

            // Act & Assert
            mockMvc.perform(post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("Should return 409 when duplicate transaction is created")
        void createTransaction_DuplicateTransaction_Returns409() throws Exception {
            // Arrange
            TransactionCreateDTO createDTO = new TransactionCreateDTO();
            createDTO.setAmount(new BigDecimal("100.00"));
            createDTO.setDescription("Duplicate transaction");
            createDTO.setType(TransactionType.DEBIT);
            createDTO.setTimestamp(fixedTimestamp);

            // Create first transaction
            mockMvc.perform(post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated());

            // Act & Assert - Try to create duplicate
            mockMvc.perform(post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions - List Transactions Tests")
    class ListTransactionsTests {

        @Test
        @DisplayName("Should return empty list when no transactions exist")
        void getAllTransactions_NoTransactions_ReturnsEmptyList() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(true))
                    .andExpect(jsonPath("$.empty").value(true));
        }

        @Test
        @DisplayName("Should return paginated transactions with default parameters")
        void getAllTransactions_DefaultPagination_ReturnsFirstPage() throws Exception {
            // Arrange - Create test transactions
            createTestTransaction(new BigDecimal("100.00"), "Transaction 1", TransactionType.DEBIT, fixedTimestamp);
            createTestTransaction(new BigDecimal("200.00"), "Transaction 2", TransactionType.CREDIT, fixedTimestamp.plusMinutes(10));

            // Act & Assert
            mockMvc.perform(get("/api/v1/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(true))
                    .andExpect(jsonPath("$.empty").value(false));
        }

        @Test
        @DisplayName("Should return specific page with custom page size")
        void getAllTransactions_CustomPagination_ReturnsSpecificPage() throws Exception {
            // Arrange - Create 3 test transactions
            for (int i = 1; i <= 3; i++) {
                createTestTransaction(
                    new BigDecimal(i * 100), 
                    "Transaction " + i, 
                    TransactionType.DEBIT, 
                    fixedTimestamp.plusMinutes(i)
                );
            }

            // Act & Assert - Request page 1 with size 2
            mockMvc.perform(get("/api/v1/transactions")
                    .param("page", "1")
                    .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(2));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/{id} - Get Transaction by ID Tests")
    class GetTransactionByIdTests {

        @Test
        @DisplayName("Should return transaction when it exists")
        void getTransactionById_ExistingTransaction_ReturnsTransaction() throws Exception {
            // Arrange
            Transaction savedTransaction = createTestTransaction(
                new BigDecimal("150.75"), 
                "Test transaction", 
                TransactionType.DEBIT, 
                fixedTimestamp
            );

            // Act & Assert
            mockMvc.perform(get("/api/v1/transactions/{id}", savedTransaction.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(savedTransaction.getId()))
                    .andExpect(jsonPath("$.amount").value(150.75))
                    .andExpect(jsonPath("$.description").value("Test transaction"))
                    .andExpect(jsonPath("$.type").value("DEBIT"))
                    .andExpect(jsonPath("$.timestamp").value(fixedTimestamp.format(formatter)));
        }

        @Test
        @DisplayName("Should return 404 when transaction does not exist")
        void getTransactionById_NonExistentTransaction_Returns404() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/transactions/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/transactions/{id} - Update Transaction Tests")
    class UpdateTransactionTests {

        @Test
        @DisplayName("Should update transaction with valid data and return 200")
        void updateTransaction_ValidData_Returns200() throws Exception {
            // Arrange
            Transaction existingTransaction = createTestTransaction(
                new BigDecimal("100.00"), 
                "Original description", 
                TransactionType.DEBIT, 
                fixedTimestamp
            );

            TransactionUpdateDTO updateDTO = new TransactionUpdateDTO();
            updateDTO.setAmount(new BigDecimal("200.50"));
            updateDTO.setDescription("Updated description");

            // Act & Assert
            mockMvc.perform(put("/api/v1/transactions/{id}", existingTransaction.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(existingTransaction.getId()))
                    .andExpect(jsonPath("$.amount").value(200.50))
                    .andExpect(jsonPath("$.description").value("Updated description"));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent transaction")
        void updateTransaction_NonExistentTransaction_Returns404() throws Exception {
            // Arrange
            TransactionUpdateDTO updateDTO = new TransactionUpdateDTO();
            updateDTO.setAmount(new BigDecimal("100.00"));

            // Act & Assert
            mockMvc.perform(put("/api/v1/transactions/{id}", 999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Should return 400 when update data is invalid")
        void updateTransaction_InvalidData_Returns400() throws Exception {
            // Arrange
            Transaction existingTransaction = createTestTransaction(
                new BigDecimal("100.00"), 
                "Original description", 
                TransactionType.DEBIT, 
                fixedTimestamp
            );

            TransactionUpdateDTO updateDTO = new TransactionUpdateDTO();
            updateDTO.setAmount(new BigDecimal("0.00")); // Invalid amount

            // Act & Assert
            mockMvc.perform(put("/api/v1/transactions/{id}", existingTransaction.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/transactions/{id} - Delete Transaction Tests")
    class DeleteTransactionTests {

        @Test
        @DisplayName("Should delete existing transaction and return 204")
        void deleteTransaction_ExistingTransaction_Returns204() throws Exception {
            // Arrange
            Transaction existingTransaction = createTestTransaction(
                new BigDecimal("100.00"), 
                "Transaction to delete", 
                TransactionType.DEBIT, 
                fixedTimestamp
            );

            // Act & Assert
            mockMvc.perform(delete("/api/v1/transactions/{id}", existingTransaction.getId()))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            // Verify transaction is actually deleted
            mockMvc.perform(get("/api/v1/transactions/{id}", existingTransaction.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent transaction")
        void deleteTransaction_NonExistentTransaction_Returns404() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/v1/transactions/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    /**
     * Helper method to create a test transaction in the database
     */
    private Transaction createTestTransaction(BigDecimal amount, String description, 
                                            TransactionType type, LocalDateTime timestamp) {
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setType(type);
        transaction.setTimestamp(timestamp);
        transaction.setCreatedAt(timestamp);
        transaction.setUpdatedAt(timestamp);
        
        return transactionRepository.save(transaction);
    }
}