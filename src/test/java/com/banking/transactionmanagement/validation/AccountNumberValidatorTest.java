package com.banking.transactionmanagement.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AccountNumberValidator.
 */
class AccountNumberValidatorTest {

  private AccountNumberValidator validator;

  @BeforeEach
  void setUp() {
    validator = new AccountNumberValidator();
    validator.initialize(null);
  }

  @Test
  void nullAccountNumber_shouldReturnTrue() {
    assertTrue(validator.isValid(null, null));
  }

  @Test
  void emptyAccountNumber_shouldReturnTrue() {
    assertTrue(validator.isValid("", null));
  }

  @Test
  void blankAccountNumber_shouldReturnTrue() {
    assertTrue(validator.isValid("   ", null));
  }

  @ParameterizedTest
  @ValueSource(strings = { "12345678", "123456789", "12345678901234567890" }) // 8, 9, and 20 digits
  void validAccountNumbers_shouldReturnTrue(String accountNumber) {
    assertTrue(validator.isValid(accountNumber, null));
  }

  @ParameterizedTest
  @ValueSource(strings = { "1234567", "123456789012345678901" }) // 7 and 21 digits
  void accountNumbersWithInvalidLength_shouldReturnFalse(String accountNumber) {
    assertFalse(validator.isValid(accountNumber, null));
  }

  @ParameterizedTest
  @ValueSource(strings = { "12345abc", "12345678a" })
  void accountNumbersWithInvalidCharacters_shouldReturnFalse(String accountNumber) {
    assertFalse(validator.isValid(accountNumber, null));
  }

  @Test
  void accountNumberWithSpacesAndDashes_shouldBeCleanedAndValidated() {
    // This should be cleaned to "12345678" which is valid
    assertTrue(validator.isValid("1234-5678", null));
    assertTrue(validator.isValid("1234 5678", null));
    assertTrue(validator.isValid("1234 - 5678", null));
  }

  @Test
  void accountNumberWithSpacesButInvalidLength_shouldReturnFalse() {
    // This should be cleaned to "1234567" which is too short
    assertFalse(validator.isValid("123 4567", null));
  }
}