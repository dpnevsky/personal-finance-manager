package com.dpnevsky.finance.storage;

import com.dpnevsky.finance.model.Transaction;
import com.dpnevsky.finance.model.TransactionType;
import com.dpnevsky.finance.model.User;
import com.dpnevsky.finance.model.Wallet;
import com.dpnevsky.finance.repository.InMemoryUserRepository;
import com.dpnevsky.finance.repository.UserRepository;
import com.dpnevsky.finance.security.PasswordHasher;
import com.dpnevsky.finance.service.AuthService;
import com.dpnevsky.finance.service.BudgetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnEmptyListWhenFileDoesNotExist() {
        assertTrue(storage().load().isEmpty());
    }

    @Test
    void shouldRestoreTransactionsWithOriginalIdsAndDates() {
        User alice = new User("alice", "stored-hash");

        Transaction original = alice.getWallet().addIncome(
                new BigDecimal("20000.50"), "Salary"
        );

        alice.getWallet().addExpense(
                new BigDecimal("800.25"), "Food"
        );

        storage().save(List.of(alice));

        User restored = storage().load().get(0);
        Wallet wallet = restored.getWallet();

        assertEquals(2, wallet.getTransactions().size());
        assertEquals(
                new BigDecimal("19200.25"),
                wallet.getBalance()
        );

        Transaction loaded = wallet.getTransactions().get(0);

        assertEquals(original.getId(), loaded.getId());
        assertEquals(original.getCreatedAt(), loaded.getCreatedAt());
        assertEquals(original.getAmount(), loaded.getAmount());
        assertEquals(original.getCategory(), loaded.getCategory());
    }

    @Test
    void shouldRestoreBudgetsAndUnusedCategories() {
        User alice = new User("alice", "stored-hash");
        Wallet wallet = alice.getWallet();

        wallet.createCategory(TransactionType.INCOME, "Bonus");
        wallet.createCategory(TransactionType.EXPENSE, "Food");

        new BudgetService().setBudget(
                wallet, "Food", new BigDecimal("4000")
        );

        storage().save(List.of(alice));

        Wallet restored = storage().load().get(0).getWallet();

        assertTrue(restored.findCategory(
                TransactionType.INCOME, "Bonus"
        ).isPresent());

        assertTrue(restored.findCategory(
                TransactionType.EXPENSE, "Food"
        ).isPresent());

        assertEquals(
                new BigDecimal("4000"),
                restored.findBudget("Food").orElseThrow().getLimit()
        );

        assertTrue(restored.getTransactions().isEmpty());
    }

    @Test
    void shouldKeepMultipleUsersSeparate() {
        User alice = new User("alice", "alice-hash");
        User bob = new User("bob", "bob-hash");

        alice.getWallet().addIncome(
                new BigDecimal("5000"), "Salary"
        );

        bob.getWallet().addIncome(
                new BigDecimal("2000"), "Bonus"
        );

        storage().save(List.of(alice, bob));

        List<User> restored = storage().load();

        assertEquals(2, restored.size());

        User loadedAlice = restored.stream()
                .filter(user -> user.getLogin().equals("alice"))
                .findFirst()
                .orElseThrow();

        User loadedBob = restored.stream()
                .filter(user -> user.getLogin().equals("bob"))
                .findFirst()
                .orElseThrow();

        assertEquals(
                new BigDecimal("5000"),
                loadedAlice.getWallet().getBalance()
        );

        assertEquals(
                new BigDecimal("2000"),
                loadedBob.getWallet().getBalance()
        );
    }

    @Test
    void shouldAuthenticateAfterReload() {
        UserRepository repository = new InMemoryUserRepository();

        AuthService authService = new AuthService(
                repository, new PasswordHasher()
        );

        authService.register("alice", "password123");

        storage().save(repository.findAll());

        UserRepository restoredRepository =
                new InMemoryUserRepository();

        storage().load().forEach(restoredRepository::save);

        AuthService restoredAuthService = new AuthService(
                restoredRepository, new PasswordHasher()
        );

        User user = restoredAuthService.login(
                "alice", "password123"
        );

        assertEquals("alice", user.getLogin());

        assertThrows(SecurityException.class, () ->
                restoredAuthService.login("alice", "wrongpassword"));
    }

    @Test
    void shouldOverwriteFileWithLatestState() {
        User alice = new User("alice", "stored-hash");

        alice.getWallet().addIncome(
                new BigDecimal("1000"), "Salary"
        );

        storage().save(List.of(alice));

        alice.getWallet().addExpense(
                new BigDecimal("200"), "Food"
        );

        storage().save(List.of(alice));

        User restored = storage().load().get(0);

        assertEquals(
                new BigDecimal("800"),
                restored.getWallet().getBalance()
        );

        assertEquals(
                2,
                restored.getWallet().getTransactions().size()
        );
    }

    @Test
    void shouldRejectCorruptedJson() throws IOException {
        Path file = tempDir.resolve("broken.json");

        Files.writeString(file, "{ invalid json");

        JsonFileStorage storage = new JsonFileStorage(file);

        assertThrows(IllegalStateException.class, storage::load);
    }

    @Test
    void shouldSaveAndRestoreEmptyUserList() {
        storage().save(List.of());

        assertTrue(Files.exists(
                tempDir.resolve("data").resolve("users.json")
        ));

        assertTrue(storage().load().isEmpty());
    }

    @Test
    void shouldCreateMissingDirectories() {
        Path file = tempDir.resolve(
                "nested/data/users.json"
        );

        JsonFileStorage storage = new JsonFileStorage(file);

        storage.save(List.of(
                new User("alice", "stored-hash")
        ));

        assertTrue(Files.exists(file));
        assertEquals(1, storage.load().size());
    }

    private JsonFileStorage storage() {
        return new JsonFileStorage(
                tempDir.resolve("data").resolve("users.json")
        );
    }
}
