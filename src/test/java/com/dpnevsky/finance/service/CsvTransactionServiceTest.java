package com.dpnevsky.finance.service;

import com.dpnevsky.finance.model.Transaction;
import com.dpnevsky.finance.model.TransactionType;
import com.dpnevsky.finance.model.User;
import com.dpnevsky.finance.storage.CsvTransactionStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CsvTransactionServiceTest {

    @TempDir
    Path tempDir;

    private final CsvTransactionService service =
            new CsvTransactionService(new CsvTransactionStorage());

    @Test
    void shouldImportTransactionsIntoAnotherWallet() {
        User alice = new User("alice", "test-hash");
        User bob = new User("bob", "test-hash");

        alice.getWallet().addIncome(
                new BigDecimal("1000"), "Salary"
        );

        alice.getWallet().addExpense(
                new BigDecimal("250"), "Food"
        );

        Path file = tempDir.resolve("transactions.csv");

        assertEquals(2, service.exportTransactions(alice, file));
        assertEquals(2, service.importTransactions(bob, file));

        assertEquals(
                new BigDecimal("750"),
                bob.getWallet().getBalance()
        );

        assertEquals(
                2,
                bob.getWallet().getTransactions().size()
        );
    }

    @Test
    void shouldNotImportSameTransactionsTwice() {
        User alice = new User("alice", "test-hash");

        alice.getWallet().addIncome(
                new BigDecimal("500"), "Salary"
        );

        Path file = tempDir.resolve("transactions.csv");

        service.exportTransactions(alice, file);

        assertEquals(0, service.importTransactions(alice, file));

        assertEquals(
                1, alice.getWallet().getTransactions().size()
        );
    }

    @Test
    void shouldRejectConflictingTransactionId() {
        User alice = new User("alice", "test-hash");
        User bob = new User("bob", "test-hash");

        Transaction original = alice.getWallet().addIncome(
                new BigDecimal("500"), "Salary"
        );

        Transaction conflicting = new Transaction(
                original.getId(),
                original.getType(),
                new BigDecimal("999"),
                original.getCategory(),
                original.getCreatedAt()
        );

        bob.getWallet().addTransaction(conflicting);

        Path file = tempDir.resolve("transactions.csv");

        service.exportTransactions(alice, file);

        assertThrows(IllegalArgumentException.class,
                () -> service.importTransactions(bob, file));

        assertEquals(
                new BigDecimal("999"),
                bob.getWallet().getBalance()
        );
    }

    @Test
    void shouldNotPartiallyImportInvalidFile() throws IOException {
        User alice = new User("alice", "test-hash");

        Path file = tempDir.resolve("invalid.csv");

        Transaction valid = new Transaction(
                TransactionType.INCOME,
                BigDecimal.TEN,
                "Salary"
        );

        CsvTransactionStorage storage =
                new CsvTransactionStorage();

        storage.write(file, java.util.List.of(valid));

        Files.writeString(
                file,
                "invalid,EXPENSE,100,Food,2026-11-16T06:30:00\n",
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND
        );

        assertThrows(IllegalArgumentException.class,
                () -> service.importTransactions(alice, file));

        assertTrue(alice.getWallet().getTransactions().isEmpty());
    }

    @Test
    void shouldRestoreCategoriesAfterImport() {
        User alice = new User("alice", "test-hash");
        User bob = new User("bob", "test-hash");

        alice.getWallet().addExpense(
                new BigDecimal("300"), "Food"
        );

        Path file = tempDir.resolve("transactions.csv");

        service.exportTransactions(alice, file);
        service.importTransactions(bob, file);

        assertTrue(bob.getWallet().findCategory(
                TransactionType.EXPENSE, "Food"
        ).isPresent());

        assertEquals(
                new BigDecimal("300"),
                bob.getWallet().getTotalExpenses()
        );
    }
}
