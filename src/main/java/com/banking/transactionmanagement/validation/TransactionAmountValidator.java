package com.banking.transactionmanagement.validation;

import com.banking.transactionmanagement.dto.TransactionCreateDTO;
import com.banking.transactionmanagement.dto.TransactionUpdateDTO;
import com.banking.transactionmanagement.model.TransactionType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/**
 * Validator for transaction amounts based on transaction type.
 * Implements business rules for different transaction types.
 */
public class TransactionAmountValidator implements ConstraintValidator<ValidTransactionAmount, Object> {

    private static final BigDecimal MAX_WITHDRAWAL_AMOUNT = new BigDecimal("10000.00");
    private static final BigDecimal MAX_TRANSFER_AMOUNT = new BigDecimal("50000.00");
    private static final BigDecimal MIN_DEPOSIT_AMOUNT = new BigDecimal("1.00");

    @Override
    public void initialize(ValidTransactionAmount constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }

        BigDecimal amount = null;
        TransactionType type = null;

        if (obj instanceof TransactionCreateDTO) {
            TransactionCreateDTO dto = (TransactionCreateDTO) obj;
            amount = dto.getAmount();
            type = dto.getType();
        } else if (obj instanceof TransactionUpdateDTO) {
            TransactionUpdateDTO dto = (TransactionUpdateDTO) obj;
            amount = dto.getAmount();
            type = dto.getType();
            
            // For updates, if either amount or type is null, skip validation
            if (amount == null || type == null) {
                return true;
            }
        }

        if (amount == null || type == null) {
            return true;
        }

        return validateAmountForType(amount, type, context);
    }

    private boolean validateAmountForType(BigDecimal amount, TransactionType type, ConstraintValidatorContext context) {
        switch (type) {
            case WITHDRAWAL:
                if (amount.compareTo(MAX_WITHDRAWAL_AMOUNT) > 0) {
                    addConstraintViolation(context, "Withdrawal amount cannot exceed " + MAX_WITHDRAWAL_AMOUNT);
                    return false;
                }
                break;
            case TRANSFER:
                if (amount.compareTo(MAX_TRANSFER_AMOUNT) > 0) {
                    addConstraintViolation(context, "Transfer amount cannot exceed " + MAX_TRANSFER_AMOUNT);
                    return false;
                }
                break;
            case DEPOSIT:
                if (amount.compareTo(MIN_DEPOSIT_AMOUNT) < 0) {
                    addConstraintViolation(context, "Deposit amount must be at least " + MIN_DEPOSIT_AMOUNT);
                    return false;
                }
                break;
            case DEBIT:
            case CREDIT:
                // No specific limits for debit/credit transactions
                break;
        }
        return true;
    }

    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
               .addPropertyNode("amount")
               .addConstraintViolation();
    }
}