package com.dpnevsky.finance.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record TransferResult(
        String senderLogin,
        String recipientLogin,
        BigDecimal amount,
        BigDecimal senderBalance,
        List<String> alerts
) {

    public TransferResult {
        Objects.requireNonNull(senderLogin, "Sender cannot be null");
        Objects.requireNonNull(recipientLogin, "Recipient cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(senderBalance, "Balance cannot be null");

        alerts = List.copyOf(
                Objects.requireNonNull(alerts, "Alerts cannot be null")
        );
    }
}
