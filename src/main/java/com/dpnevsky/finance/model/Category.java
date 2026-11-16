package com.dpnevsky.finance.model;

import java.util.Objects;

public final class Category {

    private final String name;
    private final TransactionType type;

    public Category(String name, TransactionType type) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Category name cannot be empty"
            );
        }

        this.name = name.trim();
        this.type = Objects.requireNonNull(
                type, "Category type cannot be null"
        );
    }

    public String getName() {
        return name;
    }

    public TransactionType getType() {
        return type;
    }
}
