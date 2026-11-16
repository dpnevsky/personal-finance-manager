package com.dpnevsky.finance;

import com.dpnevsky.finance.cli.ConsoleApplication;
import com.dpnevsky.finance.repository.InMemoryUserRepository;
import com.dpnevsky.finance.repository.UserRepository;
import com.dpnevsky.finance.security.PasswordHasher;
import com.dpnevsky.finance.service.AuthService;
import com.dpnevsky.finance.service.BudgetService;
import com.dpnevsky.finance.service.FinanceService;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        UserRepository userRepository =
                new InMemoryUserRepository();

        PasswordHasher passwordHasher =
                new PasswordHasher();

        AuthService authService = new AuthService(
                userRepository, passwordHasher
        );

        BudgetService budgetService = new BudgetService();

        FinanceService financeService = new FinanceService(
                budgetService
        );

        ConsoleApplication application = new ConsoleApplication(
                new Scanner(System.in, StandardCharsets.UTF_8),
                System.out,
                authService,
                financeService,
                budgetService
        );

        application.run();
    }
}
