-- Transaction Management System Sample Data
-- Initial data for development and testing

-- Insert sample transactions for development
INSERT INTO transactions (amount, description, type, timestamp, category, account_number, reference_number) VALUES
(1500.00, 'Initial deposit', 'DEPOSIT', '2024-01-01 09:00:00', 'Banking', '1234567890', 'REF001'),
(250.75, 'Grocery shopping', 'DEBIT', '2024-01-02 14:30:00', 'Food', '1234567890', 'REF002'),
(3000.00, 'Salary deposit', 'CREDIT', '2024-01-03 08:00:00', 'Income', '1234567890', 'REF003'),
(100.00, 'ATM withdrawal', 'WITHDRAWAL', '2024-01-04 16:45:00', 'Cash', '1234567890', 'REF004'),
(500.00, 'Transfer to savings', 'TRANSFER', '2024-01-05 10:15:00', 'Savings', '1234567890', 'REF005'),
(75.50, 'Coffee shop', 'DEBIT', '2024-01-06 07:30:00', 'Food', '1234567890', 'REF006'),
(2000.00, 'Rent payment', 'DEBIT', '2024-01-07 09:00:00', 'Housing', '1234567890', 'REF007'),
(150.00, 'Utility bill', 'DEBIT', '2024-01-08 11:20:00', 'Utilities', '1234567890', 'REF008'),
(1000.00, 'Investment deposit', 'DEPOSIT', '2024-01-09 13:45:00', 'Investment', '1234567890', 'REF009'),
(45.25, 'Gas station', 'DEBIT', '2024-01-10 18:00:00', 'Transportation', '1234567890', 'REF010');

-- Additional sample data for different account
INSERT INTO transactions (amount, description, type, timestamp, category, account_number, reference_number) VALUES
(5000.00, 'Business deposit', 'DEPOSIT', '2024-01-01 10:00:00', 'Business', '9876543210', 'REF011'),
(800.00, 'Office supplies', 'DEBIT', '2024-01-02 15:30:00', 'Business', '9876543210', 'REF012'),
(1200.00, 'Client payment', 'CREDIT', '2024-01-03 09:15:00', 'Business', '9876543210', 'REF013'),
(300.00, 'Business lunch', 'DEBIT', '2024-01-04 12:30:00', 'Business', '9876543210', 'REF014'),
(2000.00, 'Equipment purchase', 'DEBIT', '2024-01-05 14:00:00', 'Business', '9876543210', 'REF015');