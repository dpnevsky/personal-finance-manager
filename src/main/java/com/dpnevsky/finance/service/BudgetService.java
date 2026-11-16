package com.dpnevsky.finance.service;

import com.dpnevsky.finance.model.Budget;
import com.dpnevsky.finance.model.BudgetStatus;
import com.dpnevsky.finance.model.Category;
import com.dpnevsky.finance.model.TransactionType;
import com.dpnevsky.finance.model.Wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BudgetService {

    public Category createCategory(Wallet wallet,
                                   TransactionType type,
                                   String name) {
        Objects.requireNonNull(wallet, "Wallet cannot be null");

        return wallet.createCategory(type, name);
    }

    public Budget setBudget(Wallet wallet,
                            String categoryName,
                            BigDecimal limit) {
        Objects.requireNonNull(wallet, "Wallet cannot be null");

        Category category = wallet.findCategory(
                TransactionType.EXPENSE, categoryName
        ).orElseThrow(() -> new IllegalArgumentException(
                "Expense category not found: " + categoryName
        ));

        Budget budget = new Budget(category, limit);

        wallet.saveBudget(budget);

        return budget;
    }

    public BudgetStatus getStatus(Wallet wallet,
                                  String categoryName) {
        Objects.requireNonNull(wallet, "Wallet cannot be null");

        Budget budget = wallet.findBudget(categoryName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Budget not found: " + categoryName
                ));

        BigDecimal spent = wallet.getTotalByCategory(
                TransactionType.EXPENSE,
                budget.getCategory().getName()
        );

        return new BudgetStatus(
                budget.getCategory().getName(),
                budget.getLimit(),
                spent
        );
    }

    public List<BudgetStatus> getAllStatuses(Wallet wallet) {
        Objects.requireNonNull(wallet, "Wallet cannot be null");

        return wallet.getBudgets().stream()
                .map(budget -> getStatus(
                        wallet, budget.getCategory().getName()
                ))
                .toList();
    }

    public Optional<String> getAlert(Wallet wallet,
                                     String categoryName) {
        BudgetStatus status = getStatus(wallet, categoryName);

        if (status.isExceeded()) {
            return Optional.of(
                    "Budget exceeded for " + status.category()
                            + " by " + status.remaining().abs()
            );
        }

        if (status.limit().signum() > 0
                && status.remaining().signum() == 0) {
            return Optional.of(
                    "Budget limit reached for " + status.category()
            );
        }

        if (status.isAtLeastEightyPercent()) {
            return Optional.of(
                    "At least 80% of budget used for "
                            + status.category()
            );
        }

        return Optional.empty();
    }

    public Category renameCategory(Wallet wallet,
                                   TransactionType type,
                                   String oldName,
                                   String newName) {
        Objects.requireNonNull(wallet, "Wallet cannot be null");

        return wallet.renameCategory(type, oldName, newName);
    }

    public void deleteCategory(Wallet wallet,
                               TransactionType type,
                               String name) {
        Objects.requireNonNull(wallet, "Wallet cannot be null");

        wallet.deleteCategory(type, name);
    }

    public void deleteBudget(Wallet wallet, String categoryName) {
        Objects.requireNonNull(wallet, "Wallet cannot be null");

        wallet.deleteBudget(categoryName);
    }
}
