package com.dpnevsky.finance.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Wallet {

    private final List<Transaction> transactions = new ArrayList<>();

    public Transaction addIncome(BigDecimal amount, String category) {
        Transaction transaction = new Transaction(
                TransactionType.INCOME,
                amount,
                category
        );

        addTransaction(transaction);
        return transaction;
    }

    public Transaction addExpense(BigDecimal amount, String category) {
        Transaction transaction = new Transaction(
                TransactionType.EXPENSE,
                amount,
                category
        );

        addTransaction(transaction);
        return transaction;
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(Objects.requireNonNull(
                transaction, "Transaction cannot be null"
        ));
    }

    public BigDecimal getTotalIncome() {
        return calculateTotal(TransactionType.INCOME);
    }

    public BigDecimal getTotalExpenses() {
        return calculateTotal(TransactionType.EXPENSE);
    }

    public BigDecimal getBalance() {
        return getTotalIncome().subtract(getTotalExpenses());
    }

    public BigDecimal getTotalByCategory(TransactionType type,
                                         String category) {
        Objects.requireNonNull(type, "Transaction type cannot be null");

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException(
                    "Category cannot be empty"
            );
        }

        String normalizedCategory = category.trim();

        return transactions.stream()
                .filter(t -> t.getType() == type)
                .filter(t -> t.getCategory().equals(normalizedCategory))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Transaction> getTransactions() {
        return List.copyOf(transactions);
    }

    private BigDecimal calculateTotal(TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
