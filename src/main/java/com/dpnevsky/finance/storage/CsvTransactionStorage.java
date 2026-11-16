package com.dpnevsky.finance.storage;

import com.dpnevsky.finance.model.Transaction;
import com.dpnevsky.finance.model.TransactionType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class CsvTransactionStorage {

    private static final List<String> HEADER = List.of(
            "id", "type", "amount", "category", "createdAt"
    );

    public void write(Path path, List<Transaction> transactions) {
        Objects.requireNonNull(path, "Path cannot be null");
        Objects.requireNonNull(
                transactions, "Transactions cannot be null"
        );

        Path target = path.toAbsolutePath();

        try {
            Files.createDirectories(target.getParent());

            try (Writer writer = Files.newBufferedWriter(
                    target, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(
                         writer, CSVFormat.RFC4180)) {

                printer.printRecord(HEADER);

                for (Transaction transaction : transactions) {
                    printer.printRecord(
                            transaction.getId().toString(),
                            transaction.getType().name(),
                            transaction.getAmount().toPlainString(),
                            transaction.getCategory(),
                            transaction.getCreatedAt().toString()
                    );
                }
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Cannot export CSV: " + e.getMessage(), e
            );
        }
    }

    public List<Transaction> read(Path path) {
        Objects.requireNonNull(path, "Path cannot be null");

        try (Reader reader = Files.newBufferedReader(
                path, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.RFC4180.parse(reader)) {

            List<CSVRecord> records = parser.getRecords();

            if (records.isEmpty()) {
                throw new IllegalArgumentException(
                        "CSV header is missing"
                );
            }

            validateHeader(records.get(0));

            List<Transaction> transactions = new ArrayList<>();
            Set<UUID> ids = new HashSet<>();

            for (int i = 1; i < records.size(); i++) {
                CSVRecord record = records.get(i);
                Transaction transaction = parseTransaction(record);

                if (!ids.add(transaction.getId())) {
                    throw new IllegalArgumentException(
                            "Duplicate transaction ID at CSV row "
                                    + record.getRecordNumber()
                    );
                }

                transactions.add(transaction);
            }

            return List.copyOf(transactions);

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Cannot import CSV: " + e.getMessage(), e
            );
        }
    }

    private void validateHeader(CSVRecord record) {
        if (record.size() != HEADER.size()) {
            throw new IllegalArgumentException(
                    "Invalid CSV header"
            );
        }

        for (int i = 0; i < HEADER.size(); i++) {
            if (!HEADER.get(i).equals(record.get(i))) {
                throw new IllegalArgumentException(
                        "Invalid CSV header"
                );
            }
        }
    }

    private Transaction parseTransaction(CSVRecord record) {
        if (record.size() != HEADER.size()) {
            throw new IllegalArgumentException(
                    "Invalid number of columns at CSV row "
                            + record.getRecordNumber()
            );
        }

        try {
            return new Transaction(
                    UUID.fromString(record.get(0)),
                    TransactionType.valueOf(record.get(1)),
                    new BigDecimal(record.get(2)),
                    record.get(3),
                    LocalDateTime.parse(record.get(4))
            );

        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Invalid transaction at CSV row "
                            + record.getRecordNumber(), e
            );
        }
    }
}
