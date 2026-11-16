package com.dpnevsky.finance.cli;

import com.dpnevsky.finance.model.Transaction;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CliTableFormatter {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private CliTableFormatter() {
    }

    public static String formatTransactions(
            List<Transaction> transactions) {

        Objects.requireNonNull(
                transactions, "Transactions cannot be null"
        );

        if (transactions.isEmpty()) {
            return "No transactions.";
        }

        StringBuilder result = new StringBuilder();

        result.append(String.format(
                Locale.ROOT,
                "%-16s | %-7s | %-24s | %s%n",
                "Date", "Type", "Category", "Amount"
        ));

        result.append("-".repeat(67))
                .append(System.lineSeparator());

        for (Transaction transaction : transactions) {
            String category = transaction.getCategory()
                    .replace('\n', ' ')
                    .replace('\r', ' ');

            result.append(String.format(
                    Locale.ROOT,
                    "%-16s | %-7s | %-24s | %s%n",
                    DATE_FORMAT.format(transaction.getCreatedAt()),
                    transaction.getType(),
                    category,
                    transaction.getAmount().toPlainString()
            ));
        }

        return result.toString().stripTrailing();
    }
}
