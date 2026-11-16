package com.dpnevsky.finance.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Wallet {

    private final List<Transaction> transactions = new ArrayList<>();

    private final Map<String, Category> incomeCategories =
            new LinkedHashMap<>();

    private final Map<String, Category> expenseCategories =
            new LinkedHashMap<>();

    private final Map<String, Budget> budgets =
            new LinkedHashMap<>();

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
        Objects.requireNonNull(
                transaction, "Transaction cannot be null"
        );

        Map<String, Category> categories =
                getCategoryMap(transaction.getType());

        categories.putIfAbsent(
                transaction.getCategory(),
                new Category(
                        transaction.getCategory(),
                        transaction.getType()
                )
        );

        transactions.add(transaction);
    }

    public Category createCategory(TransactionType type, String name) {
        Category category = new Category(name, type);
        Map<String, Category> categories = getCategoryMap(type);

        if (categories.putIfAbsent(
                category.getName(), category) != null) {
            throw new IllegalArgumentException(
                    "Category already exists: " + category.getName()
            );
        }

        return category;
    }

    public Optional<Category> findCategory(TransactionType type,
                                           String name) {
        String normalizedName = normalizeCategoryName(name);

        return Optional.ofNullable(
                getCategoryMap(type).get(normalizedName)
        );
    }

    public List<Category> getCategories(TransactionType type) {
        return List.copyOf(getCategoryMap(type).values());
    }

    public void saveBudget(Budget budget) {
        Objects.requireNonNull(budget, "Budget cannot be null");

        String categoryName = budget.getCategory().getName();

        if (!expenseCategories.containsKey(categoryName)) {
            throw new IllegalArgumentException(
                    "Expense category not found: " + categoryName
            );
        }

        budgets.put(categoryName, budget);
    }

    public Optional<Budget> findBudget(String categoryName) {
        String normalizedName = normalizeCategoryName(categoryName);

        return Optional.ofNullable(budgets.get(normalizedName));
    }

    public List<Budget> getBudgets() {
        return List.copyOf(budgets.values());
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
        Objects.requireNonNull(
                type, "Transaction type cannot be null"
        );

        String normalizedCategory =
                normalizeCategoryName(category);

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

    private Map<String, Category> getCategoryMap(TransactionType type) {
        Objects.requireNonNull(type, "Category type cannot be null");

        return type == TransactionType.INCOME
                ? incomeCategories
                : expenseCategories;
    }

    private String normalizeCategoryName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Category cannot be empty"
            );
        }

        return name.trim();
    }
}
