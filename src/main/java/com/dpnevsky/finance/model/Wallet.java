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

    public Category renameCategory(TransactionType type,
                                   String oldName,
                                   String newName) {
        Map<String, Category> categories = getCategoryMap(type);

        String oldKey = normalizeCategoryName(oldName);
        String newKey = normalizeCategoryName(newName);

        if (!categories.containsKey(oldKey)) {
            throw new IllegalArgumentException(
                    "Category not found: " + oldKey
            );
        }

        if (oldKey.equals(newKey)) {
            throw new IllegalArgumentException(
                    "New category name must be different"
            );
        }

        if (categories.containsKey(newKey)) {
            throw new IllegalArgumentException(
                    "Category already exists: " + newKey
            );
        }

        Category renamedCategory = new Category(newKey, type);

        List<Transaction> updatedTransactions = new ArrayList<>();

        for (Transaction transaction : transactions) {
            if (transaction.getType() == type
                    && transaction.getCategory().equals(oldKey)) {

                updatedTransactions.add(new Transaction(
                        transaction.getId(),
                        transaction.getType(),
                        transaction.getAmount(),
                        newKey,
                        transaction.getCreatedAt()
                ));
            } else {
                updatedTransactions.add(transaction);
            }
        }

        Map<String, Category> updatedCategories =
                new LinkedHashMap<>();

        for (Map.Entry<String, Category> entry : categories.entrySet()) {
            if (entry.getKey().equals(oldKey)) {
                updatedCategories.put(newKey, renamedCategory);
            } else {
                updatedCategories.put(entry.getKey(), entry.getValue());
            }
        }

        Map<String, Budget> updatedBudgets = new LinkedHashMap<>();

        if (type == TransactionType.EXPENSE) {
            for (Map.Entry<String, Budget> entry : budgets.entrySet()) {
                if (entry.getKey().equals(oldKey)) {
                    updatedBudgets.put(
                            newKey,
                            new Budget(
                                    renamedCategory,
                                    entry.getValue().getLimit()
                            )
                    );
                } else {
                    updatedBudgets.put(entry.getKey(), entry.getValue());
                }
            }
        }

        transactions.clear();
        transactions.addAll(updatedTransactions);

        categories.clear();
        categories.putAll(updatedCategories);

        if (type == TransactionType.EXPENSE) {
            budgets.clear();
            budgets.putAll(updatedBudgets);
        }

        return renamedCategory;
    }

    public void deleteCategory(TransactionType type, String name) {
        Map<String, Category> categories = getCategoryMap(type);

        String normalizedName = normalizeCategoryName(name);

        if (!categories.containsKey(normalizedName)) {
            throw new IllegalArgumentException(
                    "Category not found: " + normalizedName
            );
        }

        boolean hasTransactions = transactions.stream()
                .anyMatch(transaction ->
                        transaction.getType() == type
                                && transaction.getCategory()
                                .equals(normalizedName));

        if (hasTransactions) {
            throw new IllegalArgumentException(
                    "Cannot delete category with transactions"
            );
        }

        if (type == TransactionType.EXPENSE
                && budgets.containsKey(normalizedName)) {
            throw new IllegalArgumentException(
                    "Remove category budget before deleting category"
            );
        }

        categories.remove(normalizedName);
    }

    public void deleteBudget(String categoryName) {
        String normalizedName = normalizeCategoryName(categoryName);

        if (budgets.remove(normalizedName) == null) {
            throw new IllegalArgumentException(
                    "Budget not found: " + normalizedName
            );
        }
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
