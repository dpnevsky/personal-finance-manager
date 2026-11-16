package com.dpnevsky.finance.service;

import com.dpnevsky.finance.model.BudgetStatus;
import com.dpnevsky.finance.model.TransactionType;
import com.dpnevsky.finance.model.Wallet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BudgetServiceTest {

    private final Wallet wallet = new Wallet();
    private final BudgetService service = new BudgetService();

    @Test
    void shouldCreateExpenseCategory() {
        service.createCategory(
                wallet, TransactionType.EXPENSE, "Food"
        );

        assertTrue(wallet.findCategory(
                TransactionType.EXPENSE, "Food"
        ).isPresent());
    }

    @Test
    void shouldAllowSameNameForDifferentTypes() {
        service.createCategory(
                wallet, TransactionType.INCOME, "Other"
        );

        service.createCategory(
                wallet, TransactionType.EXPENSE, "Other"
        );

        assertEquals(1, wallet.getCategories(
                TransactionType.INCOME).size());

        assertEquals(1, wallet.getCategories(
                TransactionType.EXPENSE).size());
    }

    @Test
    void shouldRejectDuplicateCategory() {
        service.createCategory(
                wallet, TransactionType.EXPENSE, "Food"
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.createCategory(
                        wallet, TransactionType.EXPENSE, "Food"
                ));
    }

    @Test
    void shouldAutomaticallyCreateTransactionCategory() {
        wallet.addExpense(new BigDecimal("300"), "Food");

        assertTrue(wallet.findCategory(
                TransactionType.EXPENSE, "Food"
        ).isPresent());
    }

    @Test
    void shouldSetBudget() {
        wallet.createCategory(TransactionType.EXPENSE, "Food");

        service.setBudget(
                wallet, "Food", new BigDecimal("4000")
        );

        assertEquals(
                new BigDecimal("4000"),
                wallet.findBudget("Food").orElseThrow().getLimit()
        );
    }

    @Test
    void shouldRejectUnknownCategory() {
        assertThrows(IllegalArgumentException.class, () ->
                service.setBudget(
                        wallet, "Unknown", BigDecimal.TEN
                ));
    }

    @Test
    void shouldRejectBudgetForIncomeCategory() {
        wallet.createCategory(TransactionType.INCOME, "Salary");

        assertThrows(IllegalArgumentException.class, () ->
                service.setBudget(
                        wallet, "Salary", BigDecimal.TEN
                ));
    }

    @Test
    void shouldRejectNegativeBudget() {
        wallet.createCategory(TransactionType.EXPENSE, "Food");

        assertThrows(IllegalArgumentException.class, () ->
                service.setBudget(
                        wallet, "Food", new BigDecimal("-100")
                ));
    }

    @Test
    void shouldAllowBudgetUpdate() {
        wallet.createCategory(TransactionType.EXPENSE, "Food");

        service.setBudget(wallet, "Food", new BigDecimal("4000"));
        service.setBudget(wallet, "Food", new BigDecimal("6000"));

        assertEquals(
                new BigDecimal("6000"),
                service.getStatus(wallet, "Food").limit()
        );

        assertEquals(1, wallet.getBudgets().size());
    }

    @Test
    void shouldCalculateRemainingBudget() {
        wallet.addExpense(new BigDecimal("300"), "Food");
        wallet.addExpense(new BigDecimal("500"), "Food");

        service.setBudget(wallet, "Food", new BigDecimal("4000"));

        BudgetStatus status = service.getStatus(wallet, "Food");

        assertEquals(new BigDecimal("800"), status.spent());
        assertEquals(new BigDecimal("3200"), status.remaining());
        assertFalse(status.isExceeded());
    }

    @Test
    void shouldDetectExceededBudget() {
        wallet.addExpense(new BigDecimal("3000"), "Utilities");

        service.setBudget(
                wallet, "Utilities", new BigDecimal("2500")
        );

        BudgetStatus status = service.getStatus(wallet, "Utilities");

        assertEquals(new BigDecimal("-500"), status.remaining());
        assertTrue(status.isExceeded());
        assertTrue(service.getAlert(wallet, "Utilities").isPresent());
    }

    @Test
    void shouldAlertAtEightyPercent() {
        wallet.addExpense(new BigDecimal("800"), "Food");

        service.setBudget(wallet, "Food", new BigDecimal("1000"));

        assertTrue(service.getStatus(
                wallet, "Food").isAtLeastEightyPercent());

        assertTrue(service.getAlert(wallet, "Food").isPresent());
    }

    @Test
    void shouldNotAlertBelowEightyPercent() {
        wallet.addExpense(new BigDecimal("500"), "Food");

        service.setBudget(wallet, "Food", new BigDecimal("1000"));

        assertTrue(service.getAlert(wallet, "Food").isEmpty());
    }

    @Test
    void shouldKeepBudgetsSeparateBetweenWallets() {
        Wallet otherWallet = new Wallet();

        wallet.createCategory(TransactionType.EXPENSE, "Food");
        otherWallet.createCategory(TransactionType.EXPENSE, "Food");

        service.setBudget(wallet, "Food", new BigDecimal("4000"));
        service.setBudget(otherWallet, "Food", new BigDecimal("8000"));

        assertEquals(
                new BigDecimal("4000"),
                service.getStatus(wallet, "Food").limit()
        );

        assertEquals(
                new BigDecimal("8000"),
                service.getStatus(otherWallet, "Food").limit()
        );
    }

    @Test
    void shouldListAllBudgetStatuses() {
        wallet.addExpense(new BigDecimal("500"), "Food");
        wallet.addExpense(new BigDecimal("1000"), "Entertainment");

        service.setBudget(wallet, "Food", new BigDecimal("4000"));
        service.setBudget(
                wallet, "Entertainment", new BigDecimal("3000")
        );

        assertEquals(2, service.getAllStatuses(wallet).size());
    }
}
