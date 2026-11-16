package com.dpnevsky.finance.cli;

import com.dpnevsky.finance.repository.InMemoryUserRepository;
import com.dpnevsky.finance.repository.UserRepository;
import com.dpnevsky.finance.security.PasswordHasher;
import com.dpnevsky.finance.service.AuthService;
import com.dpnevsky.finance.service.BudgetService;
import com.dpnevsky.finance.service.FinanceService;
import com.dpnevsky.finance.service.TransferService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleApplicationTest {

    @Test
    void shouldRegisterAndLogin() {
        String result = runCommands(
                "register",
                "alice",
                "password123",
                "login",
                "alice",
                "password123",
                "exit"
        );

        assertTrue(result.contains("User registered: alice"));
        assertTrue(result.contains("Logged in as: alice"));
        assertTrue(result.contains("Goodbye!"));
    }

    @Test
    void shouldRejectFinancialCommandsWithoutLogin() {
        String result = runCommands(
                "income",
                "exit"
        );

        assertTrue(result.contains("Please log in first"));
        assertTrue(result.contains("Goodbye!"));
    }

    @Test
    void shouldAddIncomeAndExpense() {
        String result = runCommands(
                "register",
                "alice",
                "password123",
                "login",
                "alice",
                "password123",
                "income",
                "20000",
                "Salary",
                "expense",
                "500",
                "Food",
                "report",
                "exit"
        );

        assertTrue(result.contains("Total income: 20000"));
        assertTrue(result.contains("Total expenses: 500"));
        assertTrue(result.contains("Balance: 19500"));
    }

    @Test
    void shouldShowBudgetWarning() {
        String result = runCommands(
                "register",
                "alice",
                "password123",
                "login",
                "alice",
                "password123",
                "category",
                "expense",
                "Food",
                "budget",
                "Food",
                "1000",
                "income",
                "2000",
                "Salary",
                "expense",
                "800",
                "Food",
                "exit"
        );

        assertTrue(result.contains("80%"));
        assertTrue(result.contains("Balance: 1200"));
    }

    @Test
    void shouldCalculateMultipleCategories() {
        String result = runCommands(
                "register",
                "alice",
                "password123",
                "login",
                "alice",
                "password123",
                "expense",
                "300",
                "Food",
                "expense",
                "500",
                "Food",
                "expense",
                "150",
                "Taxi",
                "total",
                "expense",
                "Food, Taxi",
                "exit"
        );

        assertTrue(result.contains("Selected total: 950"));
    }

    @Test
    void shouldKeepDifferentUsersSeparate() {
        String result = runCommands(
                "register",
                "alice",
                "password123",
                "register",
                "bob",
                "password456",
                "login",
                "alice",
                "password123",
                "income",
                "1000",
                "Salary",
                "logout",
                "login",
                "bob",
                "password456",
                "report",
                "exit"
        );

        assertTrue(result.contains("Logged out: alice"));
        assertTrue(result.contains("Logged in as: bob"));
        assertTrue(result.contains("Total income: 0"));
    }

    @Test
    void shouldHandleInvalidAmountWithoutCrashing() {
        String result = runCommands(
                "register",
                "alice",
                "password123",
                "login",
                "alice",
                "password123",
                "income",
                "not-a-number",
                "report",
                "exit"
        );

        assertTrue(result.contains("Invalid amount"));
        assertTrue(result.contains("Total income: 0"));
        assertTrue(result.contains("Goodbye!"));
    }

    @Test
    void shouldHandleUnknownCommand() {
        String result = runCommands(
                "unknown",
                "help",
                "exit"
        );

        assertTrue(result.contains("Unknown command"));
        assertTrue(result.contains("Available Commands"));
        assertTrue(result.contains("Goodbye!"));
    }

    @Test
    void shouldRejectWrongPasswordAndContinue() {
        String result = runCommands(
                "register",
                "alice",
                "password123",
                "login",
                "alice",
                "wrongpassword",
                "login",
                "alice",
                "password123",
                "exit"
        );

        assertTrue(result.contains("Invalid login or password"));
        assertTrue(result.contains("Logged in as: alice"));
    }

    @Test
    void shouldExitGracefullyOnEndOfInput() {
        String result = runCommands(
                "register",
                "alice"
        );

        assertTrue(result.contains("Input closed."));
    }

    @Test
    void shouldTransferMoneyThroughCli() {
        String result = runCommands(
                "register", "alice", "password123",
                "register", "bob", "password456",
                "login", "alice", "password123",
                "income", "1000", "Salary",
                "transfer", "bob", "300",
                "logout",
                "login", "bob", "password456",
                "report",
                "exit"
        );

        assertTrue(result.contains("Transferred 300 to bob"));
        assertTrue(result.contains("Balance: 700"));
        assertTrue(result.contains("Logged in as: bob"));
        assertTrue(result.contains("Total income: 300"));
    }

    @Test
    void shouldRejectTransferWithoutLogin() {
        String result = runCommands(
                "transfer",
                "exit"
        );

        assertTrue(result.contains("Please log in first"));
        assertTrue(result.contains("Goodbye!"));
    }

    @Test
    void shouldRejectInsufficientTransferAndContinue() {
        String result = runCommands(
                "register", "alice", "password123",
                "register", "bob", "password456",
                "login", "alice", "password123",
                "income", "100", "Salary",
                "transfer", "bob", "200",
                "report",
                "exit"
        );

        assertTrue(result.contains("Insufficient funds"));
        assertTrue(result.contains("Total expenses: 0"));
        assertTrue(result.contains("Balance: 100"));
        assertTrue(result.contains("Goodbye!"));
    }

    private String runCommands(String... commands) {
        String input = String.join("\n", commands) + "\n";

        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(
                        input.getBytes(StandardCharsets.UTF_8)
                );

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        UserRepository repository = new InMemoryUserRepository();

        AuthService authService = new AuthService(
                repository, new PasswordHasher()
        );

        BudgetService budgetService = new BudgetService();

        FinanceService financeService =
                new FinanceService(budgetService);

        TransferService transferService = new TransferService(
                repository,
                financeService
        );

        try (Scanner scanner = new Scanner(
                inputStream, StandardCharsets.UTF_8);
             PrintStream output = new PrintStream(
                     outputStream, true, StandardCharsets.UTF_8)) {

            ConsoleApplication application = new ConsoleApplication(
                    scanner,
                    output,
                    authService,
                    financeService,
                    budgetService,
                    transferService
            );

            application.run();
        }

        return outputStream.toString(StandardCharsets.UTF_8);
    }
}
