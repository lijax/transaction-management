package com.banking.transactionmanagement.exception;

/**
 * Exception thrown when a requested transaction is not found in the system.
 */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String message) {
        super(message);
    }

    public TransactionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionNotFoundException(Long transactionId) {
        super("Transaction not found with ID: " + transactionId);
    }
}