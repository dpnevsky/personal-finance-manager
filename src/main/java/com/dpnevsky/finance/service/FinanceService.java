package com.dpnevsky.finance.service;

import com.dpnevsky.finance.model.Category;
import com.dpnevsky.finance.model.FinanceOperationResult;
import com.dpnevsky.finance.model.Transaction;
import com.dpnevsky.finance.model.TransactionType;
import com.dpnevsky.finance.model.User;
import com.dpnevsky.finance.model.Wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FinanceService {

    private final BudgetService budgetService;

    public FinanceService(BudgetService budgetService) {
        this.budgetService = Objects.requireNonNull(
                budgetService, "Budget service cannot be null"
        );
    }

    public FinanceOperationResult addIncome(User user,
                                            BigDecimal amount,
                                            String category) {
        Wallet wallet = getWallet(user);

        Transaction transaction = wallet.addIncome(
                amount, category
        );

        return new FinanceOperationResult(
                transaction,
                wallet.getBalance(),
                List.of()
        );
    }

    public FinanceOperationResult addExpense(User user,
                                             BigDecimal amount,
                                             String category) {
        Wallet wallet = getWallet(user);

        Transaction transaction = wallet.addExpense(
                amount, category
        );

        List<String> alerts = new ArrayList<>();

        wallet.findBudget(transaction.getCategory())
                .ifPresent(budget -> budgetService.getAlert(
                        wallet, transaction.getCategory()
                ).ifPresent(alerts::add));

        BigDecimal balance = wallet.getBalance();

        if (balance.signum() < 0) {
            alerts.add(
                    "Expenses exceed income by " + balance.abs()
            );
        } else if (balance.signum() == 0) {
            alerts.add("Wallet balance is zero");
        }

        return new FinanceOperationResult(
                transaction,
                balance,
                alerts
        );
    }

    public BigDecimal getBalance(User user) {
        return getWallet(user).getBalance();
    }

    public BigDecimal getTotalByCategories(
            User user,
            TransactionType type,
            List<String> categoryNames) {

        Wallet wallet = getWallet(user);

        Objects.requireNonNull(
                type, "Transaction type cannot be null"
        );

        Objects.requireNonNull(
                categoryNames, "Categories cannot be null"
        );

        if (categoryNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one category must be selected"
            );
        }

        return categoryNames.stream()
                .map(name -> wallet.findCategory(type, name)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Category not found: " + name
                                )))
                .map(Category::getName)
                .distinct()
                .map(name -> wallet.getTotalByCategory(type, name))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Wallet getWallet(User user) {
        return Objects.requireNonNull(
                user, "User cannot be null"
        ).getWallet();
    }
}
