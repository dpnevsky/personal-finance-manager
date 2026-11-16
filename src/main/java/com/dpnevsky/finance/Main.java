package com.dpnevsky.finance;

import com.dpnevsky.finance.cli.ConsoleApplication;
import com.dpnevsky.finance.model.User;
import com.dpnevsky.finance.repository.InMemoryUserRepository;
import com.dpnevsky.finance.repository.UserRepository;
import com.dpnevsky.finance.security.PasswordHasher;
import com.dpnevsky.finance.service.AuthService;
import com.dpnevsky.finance.service.BudgetService;
import com.dpnevsky.finance.service.FinanceService;
import com.dpnevsky.finance.storage.JsonFileStorage;
import com.dpnevsky.finance.service.TransferService;
import com.dpnevsky.finance.service.CsvTransactionService;
import com.dpnevsky.finance.storage.CsvTransactionStorage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        UserRepository userRepository =
                new InMemoryUserRepository();

        JsonFileStorage storage = new JsonFileStorage(
                Path.of("data", "users.json")
        );

        List<User> savedUsers = storage.load();

        for (User user : savedUsers) {
            userRepository.save(user);
        }

        AuthService authService = new AuthService(
                userRepository, new PasswordHasher()
        );

        BudgetService budgetService = new BudgetService();

        FinanceService financeService = new FinanceService(
                budgetService
        );

        TransferService transferService = new TransferService(
                userRepository,
                financeService
        );

        CsvTransactionService csvTransactionService =
                new CsvTransactionService(
                        new CsvTransactionStorage()
                );

        ConsoleApplication application = new ConsoleApplication(
                new Scanner(System.in, StandardCharsets.UTF_8),
                System.out,
                authService,
                financeService,
                budgetService,
                transferService,
                csvTransactionService
        );

        try {
            application.run();
        } finally {
            storage.save(userRepository.findAll());
            System.out.println("Data saved successfully.");
        }
    }
}
