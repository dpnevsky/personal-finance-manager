package com.dpnevsky.finance.service;

import com.dpnevsky.finance.model.FinanceOperationResult;
import com.dpnevsky.finance.model.TransactionType;
import com.dpnevsky.finance.model.User;
import com.dpnevsky.finance.model.Wallet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinanceServiceTest {

    private final BudgetService budgetService =
            new BudgetService();

    private final FinanceService service =
            new FinanceService(budgetService);

    private final User alice =
            new User("alice", "test-password-hash");

    @Test
    void shouldAddIncomeAndReturnBalance() {
        FinanceOperationResult result = service.addIncome(
                alice, new BigDecimal("20000"), "Salary"
        );

        assertEquals(
                TransactionType.INCOME,
                result.transaction().getType()
        );

        assertEquals(
                new BigDecimal("20000"),
                result.balance()
        );

        assertTrue(result.alerts().isEmpty());
    }

    @Test
    void shouldAddExpenseAndUpdateBalance() {
        service.addIncome(
                alice, new BigDecimal("2000"), "Salary"
        );

        FinanceOperationResult result = service.addExpense(
                alice, new BigDecimal("500"), "Food"
        );

        assertEquals(
                new BigDecimal("1500"),
                result.balance()
        );

        assertEquals(
                new BigDecimal("500"),
                alice.getWallet().getTotalExpenses()
        );
    }

    @Test
    void shouldNotAlertWithoutBudget() {
        service.addIncome(
                alice, new BigDecimal("2000"), "Salary"
        );

        FinanceOperationResult result = service.addExpense(
                alice, new BigDecimal("100"), "Food"
        );

        assertTrue(result.alerts().isEmpty());
    }

    @Test
    void shouldAlertAtEightyPercent() {
        Wallet wallet = alice.getWallet();

        wallet.createCategory(TransactionType.EXPENSE, "Food");

        budgetService.setBudget(
                wallet, "Food", new BigDecimal("1000")
        );

        service.addIncome(
                alice, new BigDecimal("2000"), "Salary"
        );

        FinanceOperationResult result = service.addExpense(
                alice, new BigDecimal("800"), "Food"
        );

        assertTrue(result.alerts().stream()
                .anyMatch(alert -> alert.contains("80%")));
    }

    @Test
    void shouldAlertWhenBudgetLimitReached() {
        Wallet wallet = alice.getWallet();

        wallet.createCategory(TransactionType.EXPENSE, "Food");

        budgetService.setBudget(
                wallet, "Food", new BigDecimal("500")
        );

        service.addIncome(
                alice, new BigDecimal("1000"), "Salary"
        );

        FinanceOperationResult result = service.addExpense(
                alice, new BigDecimal("500"), "Food"
        );

        assertTrue(result.alerts().stream()
                .anyMatch(alert -> alert.contains("limit reached")));
    }

    @Test
    void shouldAlertWhenBudgetExceeded() {
        Wallet wallet = alice.getWallet();

        wallet.createCategory(TransactionType.EXPENSE, "Food");

        budgetService.setBudget(
                wallet, "Food", new BigDecimal("500")
        );

        service.addIncome(
                alice, new BigDecimal("1000"), "Salary"
        );

        FinanceOperationResult result = service.addExpense(
                alice, new BigDecimal("600"), "Food"
        );

        assertTrue(result.alerts().stream()
                .anyMatch(alert -> alert.contains("Budget exceeded")));
    }

    @Test
    void shouldWarnWhenExpensesExceedIncome() {
        service.addIncome(
                alice, new BigDecimal("100"), "Salary"
        );

        FinanceOperationResult result = service.addExpense(
                alice, new BigDecimal("150"), "Food"
        );

        assertEquals(
                new BigDecimal("-50"),
                result.balance()
        );

        assertTrue(result.alerts().stream()
                .anyMatch(alert -> alert.contains(
                        "Expenses exceed income"
                )));
    }

    @Test
    void shouldWarnWhenBalanceIsZero() {
        service.addIncome(
                alice, new BigDecimal("100"), "Salary"
        );

        FinanceOperationResult result = service.addExpense(
                alice, new BigDecimal("100"), "Food"
        );

        assertTrue(result.alerts().contains(
                "Wallet balance is zero"
        ));
    }

    @Test
    void shouldCalculateTotalsForMultipleCategories() {
        Wallet wallet = alice.getWallet();

        wallet.addExpense(new BigDecimal("300"), "Food");
        wallet.addExpense(new BigDecimal("500"), "Food");
        wallet.addExpense(new BigDecimal("150"), "Taxi");
        wallet.addExpense(new BigDecimal("200"), "Entertainment");

        BigDecimal total = service.getTotalByCategories(
                alice,
                TransactionType.EXPENSE,
                List.of("Food", "Taxi")
        );

        assertEquals(new BigDecimal("950"), total);
    }

    @Test
    void shouldNotCountDuplicateCategoriesTwice() {
        Wallet wallet = alice.getWallet();

        wallet.addExpense(new BigDecimal("300"), "Food");
        wallet.addExpense(new BigDecimal("150"), "Taxi");

        BigDecimal total = service.getTotalByCategories(
                alice,
                TransactionType.EXPENSE,
                List.of("Food", " Food ", "Taxi")
        );

        assertEquals(new BigDecimal("450"), total);
    }

    @Test
    void shouldReturnZeroForUnusedCategory() {
        alice.getWallet().createCategory(
                TransactionType.EXPENSE, "Food"
        );

        BigDecimal total = service.getTotalByCategories(
                alice,
                TransactionType.EXPENSE,
                List.of("Food")
        );

        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    void shouldRejectUnknownCategory() {
        alice.getWallet().addExpense(
                new BigDecimal("300"), "Food"
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.getTotalByCategories(
                        alice,
                        TransactionType.EXPENSE,
                        List.of("Food", "Unknown")
                ));
    }

    @Test
    void shouldRejectEmptyCategorySelection() {
        assertThrows(IllegalArgumentException.class, () ->
                service.getTotalByCategories(
                        alice,
                        TransactionType.EXPENSE,
                        List.of()
                ));
    }

    @Test
    void shouldNotRecordInvalidExpense() {
        assertThrows(IllegalArgumentException.class, () ->
                service.addExpense(
                        alice, BigDecimal.ZERO, "Food"
                ));

        assertTrue(
                alice.getWallet().getTransactions().isEmpty()
        );
    }

    @Test
    void shouldKeepUserWalletsSeparate() {
        User bob = new User("bob", "test-password-hash");

        service.addIncome(
                alice, new BigDecimal("1000"), "Salary"
        );

        assertEquals(
                new BigDecimal("1000"),
                service.getBalance(alice)
        );

        assertEquals(
                BigDecimal.ZERO,
                service.getBalance(bob)
        );
    }

    @Test
    void shouldReturnImmutableAlerts() {
        service.addIncome(
                alice, new BigDecimal("100"), "Salary"
        );

        FinanceOperationResult result = service.addExpense(
                alice, new BigDecimal("100"), "Food"
        );

        assertThrows(UnsupportedOperationException.class,
                () -> result.alerts().add("Another alert"));
    }

    @Test
    void shouldRejectNullUser() {
        assertThrows(NullPointerException.class, () ->
                service.addIncome(
                        null, BigDecimal.TEN, "Salary"
                ));
    }
}
