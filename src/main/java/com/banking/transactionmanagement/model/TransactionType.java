package com.banking.transactionmanagement.model;

/**
 * Enumeration representing different types of financial transactions
 * in the banking system.
 */
public enum TransactionType {
    /**
     * Money being taken out of an account
     */
    DEBIT,
    
    /**
     * Money being added to an account
     */
    CREDIT,
    
    /**
     * Money being moved between accounts
     */
    TRANSFER,
    
    /**
     * Cash withdrawal from an account
     */
    WITHDRAWAL,
    
    /**
     * Cash or check deposit into an account
     */
    DEPOSIT
}