package com.dpnevsky.finance.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class Budget {

    private final Category category;
    private final BigDecimal limit;

    public Budget(Category category, BigDecimal limit) {
        this.category = Objects.requireNonNull(
                category, "Category cannot be null"
        );

        if (category.getType() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException(
                    "Budget is allowed only for expense categories"
            );
        }

        if (limit == null || limit.signum() < 0) {
            throw new IllegalArgumentException(
                    "Budget limit cannot be negative"
            );
        }

        this.limit = limit;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getLimit() {
        return limit;
    }
}
