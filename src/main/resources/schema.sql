-- Transaction Management System Database Schema
-- H2 Database Schema Initialization Script

-- Drop tables if they exist (for clean initialization)
DROP TABLE IF EXISTS transactions;

-- Create transactions table with optimized structure
CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(19,2) NOT NULL CHECK (amount > 0),
    description VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('DEBIT', 'CREDIT', 'TRANSFER', 'WITHDRAWAL', 'DEPOSIT')),
    timestamp TIMESTAMP NOT NULL,
    category VARCHAR(100),
    account_number VARCHAR(20),
    reference_number VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create indexes for performance optimization
CREATE INDEX idx_transactions_timestamp ON transactions(timestamp DESC);
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_account_number ON transactions(account_number);
CREATE INDEX idx_transactions_reference_number ON transactions(reference_number);
CREATE INDEX idx_transactions_category ON transactions(category);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);

-- Create composite indexes for common query patterns
CREATE INDEX idx_transactions_type_timestamp ON transactions(type, timestamp DESC);
CREATE INDEX idx_transactions_account_timestamp ON transactions(account_number, timestamp DESC);

-- Add comments for documentation
COMMENT ON TABLE transactions IS 'Financial transactions table for banking system';
COMMENT ON COLUMN transactions.id IS 'Unique identifier for each transaction';
COMMENT ON COLUMN transactions.amount IS 'Transaction amount (must be positive)';
COMMENT ON COLUMN transactions.description IS 'Transaction description';
COMMENT ON COLUMN transactions.type IS 'Transaction type (DEBIT, CREDIT, TRANSFER, WITHDRAWAL, DEPOSIT)';
COMMENT ON COLUMN transactions.timestamp IS 'When the transaction occurred';
COMMENT ON COLUMN transactions.category IS 'Optional transaction category';
COMMENT ON COLUMN transactions.account_number IS 'Associated account number';
COMMENT ON COLUMN transactions.reference_number IS 'Unique reference number for the transaction';
COMMENT ON COLUMN transactions.created_at IS 'When the record was created';
COMMENT ON COLUMN transactions.updated_at IS 'When the record was last updated';