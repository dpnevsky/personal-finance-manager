package com.dpnevsky.finance.storage;

import com.dpnevsky.finance.model.Transaction;
import com.dpnevsky.finance.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvTransactionStorageTest {

    @TempDir
    Path tempDir;

    private final CsvTransactionStorage storage =
            new CsvTransactionStorage();

    @Test
    void shouldExportAndImportTransactions() {
        Path file = tempDir.resolve("transactions.csv");

        Transaction income = new Transaction(
                TransactionType.INCOME,
                new BigDecimal("20000.50"),
                "Salary"
        );

        Transaction expense = new Transaction(
                TransactionType.EXPENSE,
                new BigDecimal("800.25"),
                "Food"
        );

        storage.write(file, List.of(income, expense));

        List<Transaction> restored = storage.read(file);

        assertEquals(2, restored.size());

        assertEquals(income.getId(), restored.get(0).getId());
        assertEquals(income.getCreatedAt(),
                restored.get(0).getCreatedAt());
        assertEquals(income.getAmount(),
                restored.get(0).getAmount());

        assertEquals(expense.getId(), restored.get(1).getId());
        assertEquals(expense.getCategory(),
                restored.get(1).getCategory());
    }

    @Test
    void shouldPreserveSpecialCharactersInCategory() {
        Path file = tempDir.resolve("special.csv");

        String category = "Food, \"Cafe\"\nLunch";

        storage.write(file, List.of(
                new Transaction(
                        TransactionType.EXPENSE,
                        BigDecimal.TEN,
                        category
                )
        ));

        assertEquals(
                category,
                storage.read(file).get(0).getCategory()
        );
    }

    @Test
    void shouldSupportEmptyTransactionList() {
        Path file = tempDir.resolve("empty.csv");

        storage.write(file, List.of());

        assertTrue(Files.exists(file));
        assertTrue(storage.read(file).isEmpty());
    }

    @Test
    void shouldRejectMissingFile() {
        Path file = tempDir.resolve("missing.csv");

        assertThrows(IllegalArgumentException.class,
                () -> storage.read(file));
    }

    @Test
    void shouldRejectInvalidHeader() throws IOException {
        Path file = tempDir.resolve("invalid-header.csv");

        Files.writeString(
                file,
                "wrong,columns\n",
                StandardCharsets.UTF_8
        );

        assertThrows(IllegalArgumentException.class,
                () -> storage.read(file));
    }

    @Test
    void shouldRejectWrongNumberOfColumns() throws IOException {
        Path file = tempDir.resolve("columns.csv");

        Files.writeString(
                file,
                "id,type,amount,category,createdAt\n"
                        + "a,b,c\n",
                StandardCharsets.UTF_8
        );

        assertThrows(IllegalArgumentException.class,
                () -> storage.read(file));
    }

    @Test
    void shouldRejectInvalidAmount() throws IOException {
        Path file = tempDir.resolve("amount.csv");

        Files.writeString(
                file,
                "id,type,amount,category,createdAt\n"
                        + "550e8400-e29b-41d4-a716-446655440000,"
                        + "INCOME,invalid,Salary,2026-11-16T06:30:00\n",
                StandardCharsets.UTF_8
        );

        assertThrows(IllegalArgumentException.class,
                () -> storage.read(file));
    }

    @Test
    void shouldRejectDuplicateTransactionIds() throws IOException {
        Path file = tempDir.resolve("duplicates.csv");

        Transaction transaction = new Transaction(
                TransactionType.INCOME,
                BigDecimal.TEN,
                "Salary"
        );

        storage.write(file, List.of(transaction, transaction));

        assertThrows(IllegalArgumentException.class,
                () -> storage.read(file));
    }
}
