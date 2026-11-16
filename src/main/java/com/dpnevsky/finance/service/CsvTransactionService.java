package com.dpnevsky.finance.service;

import com.dpnevsky.finance.model.Transaction;
import com.dpnevsky.finance.model.User;
import com.dpnevsky.finance.model.Wallet;
import com.dpnevsky.finance.storage.CsvTransactionStorage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class CsvTransactionService {

    private final CsvTransactionStorage storage;

    public CsvTransactionService(CsvTransactionStorage storage) {
        this.storage = Objects.requireNonNull(
                storage, "CSV storage cannot be null"
        );
    }

    public int exportTransactions(User user, Path path) {
        Objects.requireNonNull(user, "User cannot be null");

        List<Transaction> transactions =
                user.getWallet().getTransactions();

        storage.write(path, transactions);

        return transactions.size();
    }

    public int importTransactions(User user, Path path) {
        Objects.requireNonNull(user, "User cannot be null");

        Wallet wallet = user.getWallet();

        // Parse and validate the whole file before modifying the wallet.
        List<Transaction> imported = storage.read(path);

        Map<UUID, Transaction> existing = new HashMap<>();

        for (Transaction transaction : wallet.getTransactions()) {
            existing.put(transaction.getId(), transaction);
        }

        List<Transaction> newTransactions = new ArrayList<>();

        for (Transaction transaction : imported) {
            Transaction previous = existing.get(transaction.getId());

            if (previous == null) {
                newTransactions.add(transaction);
            } else if (!isSameTransaction(previous, transaction)) {
                throw new IllegalArgumentException(
                        "Conflicting transaction ID: "
                                + transaction.getId()
                );
            }
        }

        for (Transaction transaction : newTransactions) {
            wallet.addTransaction(transaction);
        }

        return newTransactions.size();
    }

    private boolean isSameTransaction(Transaction first,
                                      Transaction second) {
        return first.getId().equals(second.getId())
                && first.getType() == second.getType()
                && first.getAmount().compareTo(second.getAmount()) == 0
                && first.getCategory().equals(second.getCategory())
                && first.getCreatedAt().equals(second.getCreatedAt());
    }
}
