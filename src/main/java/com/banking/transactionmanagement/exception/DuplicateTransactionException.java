package com.banking.transactionmanagement.exception;

/**
 * Exception thrown when attempting to create a duplicate transaction.
 */
public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String message) {
        super(message);
    }

    public DuplicateTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}