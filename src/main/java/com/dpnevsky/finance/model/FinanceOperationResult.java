package com.dpnevsky.finance.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record FinanceOperationResult(
        Transaction transaction,
        BigDecimal balance,
        List<String> alerts
) {

    public FinanceOperationResult {
        Objects.requireNonNull(
                transaction, "Transaction cannot be null"
        );

        Objects.requireNonNull(
                balance, "Balance cannot be null"
        );

        alerts = List.copyOf(
                Objects.requireNonNull(
                        alerts, "Alerts cannot be null"
                )
        );
    }
}
