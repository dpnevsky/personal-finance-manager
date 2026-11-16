package com.dpnevsky.finance.service;

import com.dpnevsky.finance.model.TransactionType;
import com.dpnevsky.finance.model.TransferResult;
import com.dpnevsky.finance.model.User;
import com.dpnevsky.finance.repository.InMemoryUserRepository;
import com.dpnevsky.finance.repository.UserRepository;
import com.dpnevsky.finance.storage.JsonFileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {

    private final UserRepository repository =
            new InMemoryUserRepository();

    private final BudgetService budgetService =
            new BudgetService();

    private final FinanceService financeService =
            new FinanceService(budgetService);

    private final TransferService transferService =
            new TransferService(repository, financeService);

    private final User alice = new User("alice", "test-hash");
    private final User bob = new User("bob", "test-hash");

    @BeforeEach
    void setUp() {
        repository.save(alice);
        repository.save(bob);
    }

    @Test
    void shouldTransferMoney() {
        alice.getWallet().addIncome(
                new BigDecimal("1000"), "Salary"
        );

        TransferResult result = transferService.transfer(
                alice, "bob", new BigDecimal("300")
        );

        assertEquals(
                new BigDecimal("700"),
                alice.getWallet().getBalance()
        );

        assertEquals(
                new BigDecimal("300"),
                bob.getWallet().getBalance()
        );

        assertEquals("alice", result.senderLogin());
        assertEquals("bob", result.recipientLogin());
        assertEquals(new BigDecimal("300"), result.amount());
    }

    @Test
    void shouldRecordCounterpartyNames() {
        alice.getWallet().addIncome(
                new BigDecimal("1000"), "Salary"
        );

        transferService.transfer(
                alice, "bob", new BigDecimal("200")
        );

        assertEquals(
                new BigDecimal("200"),
                alice.getWallet().getTotalByCategory(
                        TransactionType.EXPENSE,
                        "Transfer to bob"
                )
        );

        assertEquals(
                new BigDecimal("200"),
                bob.getWallet().getTotalByCategory(
                        TransactionType.INCOME,
                        "Transfer from alice"
                )
        );
    }

    @Test
    void shouldRejectUnknownRecipient() {
        alice.getWallet().addIncome(
                new BigDecimal("1000"), "Salary"
        );

        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(
                        alice, "unknown", BigDecimal.TEN
                ));

        assertEquals(
                new BigDecimal("1000"),
                alice.getWallet().getBalance()
        );
    }

    @Test
    void shouldRejectTransferToSelf() {
        alice.getWallet().addIncome(
                new BigDecimal("1000"), "Salary"
        );

        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(
                        alice, "alice", BigDecimal.TEN
                ));
    }

    @Test
    void shouldRejectInsufficientFundsWithoutChanges() {
        alice.getWallet().addIncome(
                new BigDecimal("100"), "Salary"
        );

        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(
                        alice, "bob", new BigDecimal("200")
                ));

        assertEquals(1, alice.getWallet().getTransactions().size());
        assertTrue(bob.getWallet().getTransactions().isEmpty());
        assertEquals(
                new BigDecimal("100"),
                alice.getWallet().getBalance()
        );
    }

    @Test
    void shouldRejectZeroAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(
                        alice, "bob", BigDecimal.ZERO
                ));
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(
                        alice, "bob", new BigDecimal("-100")
                ));
    }

    @Test
    void shouldRejectNullAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(alice, "bob", null));
    }

    @Test
    void shouldRejectBlankRecipient() {
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(
                        alice, "   ", BigDecimal.TEN
                ));
    }

    @Test
    void shouldTrimRecipientLogin() {
        alice.getWallet().addIncome(
                new BigDecimal("500"), "Salary"
        );

        TransferResult result = transferService.transfer(
                alice, "  bob  ", new BigDecimal("100")
        );

        assertEquals("bob", result.recipientLogin());
        assertEquals(
                new BigDecimal("100"),
                bob.getWallet().getBalance()
        );
    }

    @Test
    void shouldAllowTransferOfFullBalance() {
        alice.getWallet().addIncome(
                new BigDecimal("100"), "Salary"
        );

        TransferResult result = transferService.transfer(
                alice, "bob", new BigDecimal("100")
        );

        assertEquals(BigDecimal.ZERO, result.senderBalance());
        assertTrue(result.alerts().contains(
                "Wallet balance is zero"
        ));
    }

    @Test
    void shouldReturnBudgetWarning() {
        alice.getWallet().addIncome(
                new BigDecimal("500"), "Salary"
        );

        alice.getWallet().createCategory(
                TransactionType.EXPENSE, "Transfer to bob"
        );

        budgetService.setBudget(
                alice.getWallet(),
                "Transfer to bob",
                new BigDecimal("100")
        );

        TransferResult result = transferService.transfer(
                alice, "bob", new BigDecimal("120")
        );

        assertTrue(result.alerts().stream()
                .anyMatch(alert ->
                        alert.contains("Budget exceeded")));
    }

    @Test
    void shouldRejectNullSender() {
        assertThrows(NullPointerException.class, () ->
                transferService.transfer(
                        null, "bob", BigDecimal.TEN
                ));
    }

    @Test
    void shouldRejectUnregisteredSender() {
        User fakeAlice = new User("alice", "another-hash");

        assertThrows(SecurityException.class, () ->
                transferService.transfer(
                        fakeAlice, "bob", BigDecimal.TEN
                ));
    }

    @Test
    void shouldLeaveUnrelatedUserUnchanged() {
        User charlie = new User("charlie", "test-hash");
        repository.save(charlie);

        charlie.getWallet().addIncome(
                new BigDecimal("500"), "Salary"
        );

        alice.getWallet().addIncome(
                new BigDecimal("200"), "Salary"
        );

        transferService.transfer(
                alice, "bob", new BigDecimal("100")
        );

        assertEquals(
                new BigDecimal("500"),
                charlie.getWallet().getBalance()
        );
    }

    @Test
    void shouldPersistTransferBetweenRuns(@TempDir Path tempDir) {
        alice.getWallet().addIncome(
                new BigDecimal("1000"), "Salary"
        );

        transferService.transfer(
                alice, "bob", new BigDecimal("250")
        );

        JsonFileStorage storage = new JsonFileStorage(
                tempDir.resolve("users.json")
        );

        storage.save(repository.findAll());

        List<User> restored = storage.load();

        User restoredAlice = restored.stream()
                .filter(user -> user.getLogin().equals("alice"))
                .findFirst()
                .orElseThrow();

        User restoredBob = restored.stream()
                .filter(user -> user.getLogin().equals("bob"))
                .findFirst()
                .orElseThrow();

        assertEquals(
                new BigDecimal("750"),
                restoredAlice.getWallet().getBalance()
        );

        assertEquals(
                new BigDecimal("250"),
                restoredBob.getWallet().getBalance()
        );
    }
}
