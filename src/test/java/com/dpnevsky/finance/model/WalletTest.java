package com.dpnevsky.finance.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WalletTest {

    @Test
    void shouldCreateEmptyWallet() {
        Wallet wallet = new Wallet();

        assertEquals(BigDecimal.ZERO, wallet.getTotalIncome());
        assertEquals(BigDecimal.ZERO, wallet.getTotalExpenses());
        assertEquals(BigDecimal.ZERO, wallet.getBalance());
        assertTrue(wallet.getTransactions().isEmpty());
    }

    @Test
    void shouldAddIncome() {
        Wallet wallet = new Wallet();

        Transaction transaction = wallet.addIncome(
                new BigDecimal("20000"),
                "Salary"
        );

        assertEquals(TransactionType.INCOME, transaction.getType());
        assertEquals(new BigDecimal("20000"), wallet.getTotalIncome());
        assertEquals(1, wallet.getTransactions().size());
    }

    @Test
    void shouldAddExpense() {
        Wallet wallet = new Wallet();

        Transaction transaction = wallet.addExpense(
                new BigDecimal("500"),
                "Food"
        );

        assertEquals(TransactionType.EXPENSE, transaction.getType());
        assertEquals(new BigDecimal("500"), wallet.getTotalExpenses());
        assertEquals(1, wallet.getTransactions().size());
    }

    @Test
    void shouldCalculateBalance() {
        Wallet wallet = new Wallet();

        wallet.addIncome(new BigDecimal("20000"), "Salary");
        wallet.addIncome(new BigDecimal("40000"), "Salary");
        wallet.addExpense(new BigDecimal("300"), "Food");
        wallet.addExpense(new BigDecimal("500"), "Food");
        wallet.addExpense(new BigDecimal("3000"), "Entertainment");

        assertEquals(new BigDecimal("60000"), wallet.getTotalIncome());
        assertEquals(new BigDecimal("3800"), wallet.getTotalExpenses());
        assertEquals(new BigDecimal("56200"), wallet.getBalance());
    }

    @Test
    void shouldCalculateIncomeByCategory() {
        Wallet wallet = new Wallet();

        wallet.addIncome(new BigDecimal("20000"), "Salary");
        wallet.addIncome(new BigDecimal("40000"), "Salary");
        wallet.addIncome(new BigDecimal("3000"), "Bonus");

        BigDecimal total = wallet.getTotalByCategory(
                TransactionType.INCOME,
                "Salary"
        );

        assertEquals(new BigDecimal("60000"), total);
    }

    @Test
    void shouldCalculateExpensesByCategory() {
        Wallet wallet = new Wallet();

        wallet.addExpense(new BigDecimal("300"), "Food");
        wallet.addExpense(new BigDecimal("500"), "Food");
        wallet.addExpense(new BigDecimal("3000"), "Entertainment");

        BigDecimal total = wallet.getTotalByCategory(
                TransactionType.EXPENSE,
                "Food"
        );

        assertEquals(new BigDecimal("800"), total);
    }

    @Test
    void shouldReturnZeroForUnknownCategory() {
        Wallet wallet = new Wallet();

        wallet.addExpense(new BigDecimal("500"), "Food");

        BigDecimal total = wallet.getTotalByCategory(
                TransactionType.EXPENSE,
                "Transport"
        );

        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    void shouldRejectNullTransaction() {
        Wallet wallet = new Wallet();

        assertThrows(NullPointerException.class,
                () -> wallet.addTransaction(null));
    }

    @Test
    void shouldRejectInvalidTransactions() {
        Wallet wallet = new Wallet();

        assertThrows(IllegalArgumentException.class,
                () -> wallet.addIncome(BigDecimal.ZERO, "Salary"));

        assertThrows(IllegalArgumentException.class,
                () -> wallet.addExpense(BigDecimal.TEN, "  "));

        assertTrue(wallet.getTransactions().isEmpty());
    }

    @Test
    void shouldReturnUnmodifiableTransactionList() {
        Wallet wallet = new Wallet();

        wallet.addIncome(new BigDecimal("1000"), "Salary");

        List<Transaction> transactions = wallet.getTransactions();

        assertThrows(UnsupportedOperationException.class,
                () -> transactions.clear());

        assertEquals(1, wallet.getTransactions().size());
    }

    @Test
    void shouldReturnTransactionSnapshot() {
        Wallet wallet = new Wallet();

        wallet.addIncome(new BigDecimal("1000"), "Salary");
        List<Transaction> snapshot = wallet.getTransactions();

        wallet.addExpense(new BigDecimal("200"), "Food");

        assertEquals(1, snapshot.size());
        assertEquals(2, wallet.getTransactions().size());
    }
}
