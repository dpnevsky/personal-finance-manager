package com.dpnevsky.finance.model;

import java.math.BigDecimal;
import java.util.Objects;

public record BudgetStatus(
        String category,
        BigDecimal limit,
        BigDecimal spent
) {

    public BudgetStatus {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException(
                    "Category cannot be empty"
            );
        }

        Objects.requireNonNull(limit, "Limit cannot be null");
        Objects.requireNonNull(spent, "Spent cannot be null");

        if (limit.signum() < 0 || spent.signum() < 0) {
            throw new IllegalArgumentException(
                    "Amounts cannot be negative"
            );
        }
    }

    public BigDecimal remaining() {
        return limit.subtract(spent);
    }

    public boolean isExceeded() {
        return remaining().signum() < 0;
    }

    public boolean isAtLeastEightyPercent() {
        return limit.signum() > 0
                && spent.multiply(BigDecimal.valueOf(100))
                .compareTo(
                        limit.multiply(BigDecimal.valueOf(80))
                ) >= 0;
    }
}
