package com.dpnevsky.finance.cli;

import com.dpnevsky.finance.model.Transaction;
import com.dpnevsky.finance.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CliTableFormatterTest {

    @Test
    void shouldFormatEmptyTransactions() {
        String result = CliTableFormatter.formatTransactions(List.of());

        assertEquals("No transactions.", result);
    }

    @Test
    void shouldFormatTransactionWithReadableDate() {
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                TransactionType.EXPENSE,
                new BigDecimal("1250.50"),
                "Food",
                LocalDateTime.of(2026, 11, 16, 7, 30)
        );

        String result = CliTableFormatter.formatTransactions(
                List.of(transaction)
        );

        assertTrue(result.contains("Date"));
        assertTrue(result.contains("Category"));
        assertTrue(result.contains("2026-11-16 07:30"));
        assertTrue(result.contains("EXPENSE"));
        assertTrue(result.contains("1250.50"));
    }

    @Test
    void shouldReplaceLineBreaksInCategoryDisplay() {
        Transaction transaction = new Transaction(
                TransactionType.EXPENSE,
                BigDecimal.TEN,
                "Food\nTaxi"
        );

        String result = CliTableFormatter.formatTransactions(
                List.of(transaction)
        );

        assertTrue(result.contains("Food Taxi"));
        assertFalse(result.contains("Food\nTaxi"));
    }

    @Test
    void shouldRejectNullTransactionList() {
        assertThrows(NullPointerException.class, () ->
                CliTableFormatter.formatTransactions(null));
    }
}
