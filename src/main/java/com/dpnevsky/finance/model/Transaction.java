package com.dpnevsky.finance.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Transaction {

    private final UUID id;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String category;
    private final LocalDateTime createdAt;

    public Transaction(TransactionType type,
                       BigDecimal amount,
                       String category) {
        this(UUID.randomUUID(), type, amount, category, LocalDateTime.now());
    }

    public Transaction(UUID id,
                       TransactionType type,
                       BigDecimal amount,
                       String category,
                       LocalDateTime createdAt) {

        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.type = Objects.requireNonNull(type, "Type cannot be null");

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero");
        }

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException(
                    "Category cannot be empty");
        }

        this.amount = amount;
        this.category = category.trim();
        this.createdAt = Objects.requireNonNull(
                createdAt, "Creation date cannot be null");
    }

    public UUID getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
