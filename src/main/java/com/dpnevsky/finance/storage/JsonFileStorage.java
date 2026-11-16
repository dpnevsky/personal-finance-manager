package com.dpnevsky.finance.storage;

import com.dpnevsky.finance.model.Budget;
import com.dpnevsky.finance.model.Category;
import com.dpnevsky.finance.model.Transaction;
import com.dpnevsky.finance.model.TransactionType;
import com.dpnevsky.finance.model.User;
import com.dpnevsky.finance.model.Wallet;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class JsonFileStorage {

    private final Path filePath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonFileStorage(Path filePath) {
        this.filePath = Objects.requireNonNull(
                filePath, "File path cannot be null"
        ).toAbsolutePath();
    }

    public List<User> load() {
        if (Files.notExists(filePath)) {
            return List.of();
        }

        try {
            UserData[] data = objectMapper.readValue(
                    filePath.toFile(), UserData[].class
            );

            if (data == null) {
                throw new IllegalArgumentException(
                        "Expected a JSON array of users"
                );
            }

            List<User> users = new ArrayList<>();

            for (UserData snapshot : data) {
                users.add(restoreUser(snapshot));
            }

            return List.copyOf(users);

        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to load data from " + filePath, e
            );
        }
    }

    public void save(Collection<User> users) {
        Objects.requireNonNull(users, "Users cannot be null");

        List<UserData> snapshots = users.stream()
                .map(this::createSnapshot)
                .toList();

        try {
            Files.createDirectories(filePath.getParent());

            Path temporaryFile = Files.createTempFile(
                    filePath.getParent(), "finance-", ".tmp"
            );

            try {
                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(temporaryFile.toFile(), snapshots);

                try {
                    Files.move(
                            temporaryFile,
                            filePath,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE
                    );
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(
                            temporaryFile,
                            filePath,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            } finally {
                Files.deleteIfExists(temporaryFile);
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to save data to " + filePath, e
            );
        }
    }

    private UserData createSnapshot(User user) {
        Objects.requireNonNull(user, "User cannot be null");

        Wallet wallet = user.getWallet();

        List<String> incomeCategories = wallet.getCategories(
                TransactionType.INCOME
        ).stream().map(Category::getName).toList();

        List<String> expenseCategories = wallet.getCategories(
                TransactionType.EXPENSE
        ).stream().map(Category::getName).toList();

        List<TransactionData> transactions =
                wallet.getTransactions().stream()
                        .map(transaction -> new TransactionData(
                                transaction.getId().toString(),
                                transaction.getType().name(),
                                transaction.getAmount().toPlainString(),
                                transaction.getCategory(),
                                transaction.getCreatedAt().toString()
                        ))
                        .toList();

        List<BudgetData> budgets = wallet.getBudgets().stream()
                .map(budget -> new BudgetData(
                        budget.getCategory().getName(),
                        budget.getLimit().toPlainString()
                ))
                .toList();

        return new UserData(
                user.getLogin(),
                user.getPasswordHash(),
                incomeCategories,
                expenseCategories,
                transactions,
                budgets
        );
    }

    private User restoreUser(UserData snapshot) {
        User user = new User(
                snapshot.login(),
                snapshot.passwordHash()
        );

        Wallet wallet = user.getWallet();

        for (String name : snapshot.incomeCategories()) {
            wallet.createCategory(TransactionType.INCOME, name);
        }

        for (String name : snapshot.expenseCategories()) {
            wallet.createCategory(TransactionType.EXPENSE, name);
        }

        for (TransactionData data : snapshot.transactions()) {
            Transaction transaction = new Transaction(
                    UUID.fromString(data.id()),
                    TransactionType.valueOf(data.type()),
                    new BigDecimal(data.amount()),
                    data.category(),
                    LocalDateTime.parse(data.createdAt())
            );

            wallet.addTransaction(transaction);
        }

        for (BudgetData data : snapshot.budgets()) {
            Category category = wallet.findCategory(
                    TransactionType.EXPENSE, data.category()
            ).orElseThrow(() -> new IllegalArgumentException(
                    "Unknown budget category: " + data.category()
            ));

            wallet.saveBudget(new Budget(
                    category, new BigDecimal(data.limit())
            ));
        }

        return user;
    }

    public record UserData(
            String login,
            String passwordHash,
            List<String> incomeCategories,
            List<String> expenseCategories,
            List<TransactionData> transactions,
            List<BudgetData> budgets
    ) {
    }

    public record TransactionData(
            String id,
            String type,
            String amount,
            String category,
            String createdAt
    ) {
    }

    public record BudgetData(
            String category,
            String limit
    ) {
    }
}
