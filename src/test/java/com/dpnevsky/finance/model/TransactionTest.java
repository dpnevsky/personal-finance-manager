package com.dpnevsky.finance.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void shouldCreateIncomeTransaction() {
        Transaction transaction = new Transaction(
                TransactionType.INCOME,
                new BigDecimal("50000.00"),
                "Salary"
        );

        assertEquals(TransactionType.INCOME, transaction.getType());
        assertEquals(0, new BigDecimal("50000.00")
                .compareTo(transaction.getAmount()));
        assertEquals("Salary", transaction.getCategory());
        assertNotNull(transaction.getId());
        assertNotNull(transaction.getCreatedAt());
    }

    @Test
    void shouldCreateExpenseTransaction() {
        Transaction transaction = new Transaction(
                TransactionType.EXPENSE,
                new BigDecimal("1500"),
                "Food"
        );

        assertEquals(TransactionType.EXPENSE, transaction.getType());
        assertEquals("Food", transaction.getCategory());
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                        TransactionType.EXPENSE,
                        BigDecimal.ZERO,
                        "Food"
                ));
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                        TransactionType.EXPENSE,
                        new BigDecimal("-100"),
                        "Food"
                ));
    }

    @Test
    void shouldRejectNullAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                        TransactionType.INCOME,
                        null,
                        "Salary"
                ));
    }

    @Test
    void shouldRejectEmptyCategory() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                        TransactionType.EXPENSE,
                        BigDecimal.TEN,
                        "   "
                ));
    }

    @Test
    void shouldRejectNullType() {
        assertThrows(NullPointerException.class, () ->
                new Transaction(
                        null,
                        BigDecimal.TEN,
                        "Food"
                ));
    }

    @Test
    void shouldTrimCategory() {
        Transaction transaction = new Transaction(
                TransactionType.EXPENSE,
                BigDecimal.TEN,
                "  Food  "
        );

        assertEquals("Food", transaction.getCategory());
    }

    @Test
    void shouldGenerateUniqueIds() {
        Transaction first = new Transaction(
                TransactionType.INCOME,
                BigDecimal.TEN,
                "Salary"
        );

        Transaction second = new Transaction(
                TransactionType.INCOME,
                BigDecimal.TEN,
                "Salary"
        );

        assertNotEquals(first.getId(), second.getId());
    }
}
