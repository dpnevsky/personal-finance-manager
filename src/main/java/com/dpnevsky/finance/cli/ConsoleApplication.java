package com.dpnevsky.finance.cli;

import com.dpnevsky.finance.model.Budget;
import com.dpnevsky.finance.model.BudgetStatus;
import com.dpnevsky.finance.model.Category;
import com.dpnevsky.finance.model.FinanceOperationResult;
import com.dpnevsky.finance.model.Transaction;
import com.dpnevsky.finance.model.TransactionType;
import com.dpnevsky.finance.model.User;
import com.dpnevsky.finance.model.Wallet;
import com.dpnevsky.finance.service.AuthService;
import com.dpnevsky.finance.service.BudgetService;
import com.dpnevsky.finance.service.FinanceService;
import com.dpnevsky.finance.model.TransferResult;
import com.dpnevsky.finance.service.TransferService;
import com.dpnevsky.finance.service.CsvTransactionService;

import java.nio.file.Path;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;

public class ConsoleApplication {

    private final Scanner scanner;
    private final PrintStream output;
    private final AuthService authService;
    private final FinanceService financeService;
    private final BudgetService budgetService;
    private final TransferService transferService;
    private final CsvTransactionService csvTransactionService;

    private User currentUser;

    public ConsoleApplication(Scanner scanner,
                              PrintStream output,
                              AuthService authService,
                              FinanceService financeService,
                              BudgetService budgetService,
                              TransferService transferService,
                              CsvTransactionService csvTransactionService) {
        this.scanner = Objects.requireNonNull(scanner);
        this.output = Objects.requireNonNull(output);
        this.authService = Objects.requireNonNull(authService);
        this.financeService = Objects.requireNonNull(financeService);
        this.budgetService = Objects.requireNonNull(budgetService);
        this.transferService = Objects.requireNonNull(transferService);
        this.csvTransactionService = Objects.requireNonNull(
                csvTransactionService
        );
    }


    public void run() {
        output.println("Personal Finance Manager");
        printHelp();

        while (true) {
            output.print("> ");
            output.flush();

            if (!scanner.hasNextLine()) {
                output.println("Input closed.");
                return;
            }

            String command = scanner.nextLine()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (command.equals("exit")) {
                output.println("Goodbye!");
                return;
            }

            try {
                executeCommand(command);
            } catch (IllegalArgumentException | SecurityException e) {
                output.println("Error: " + e.getMessage());
            } catch (NoSuchElementException e) {
                output.println("Input closed.");
                return;
            }
        }
    }

    private void executeCommand(String command) {
        switch (command) {
            case "register" -> register();
            case "login" -> login();
            case "logout" -> logout();
            case "income" -> addIncome();
            case "expense" -> addExpense();
            case "category" -> createCategory();
            case "categories" -> showCategories();
            case "budget" -> setBudget();
            case "budgets" -> showBudgets();
            case "report" -> showReport();
            case "total" -> showSelectedTotal();
            case "transactions" -> showTransactions();
            case "transfer" -> transfer();
            case "rename-category" -> renameCategory();
            case "delete-category" -> deleteCategory();
            case "delete-budget" -> deleteBudget();
            case "export-csv" -> exportCsv();
            case "import-csv" -> importCsv();
            case "balance" -> showBalance();
            case "whoami" -> showCurrentUser();
            case "help" -> printHelp();
            case "" -> {
                // Ignore empty commands.
            }
            default -> output.println(
                    "Unknown command: " + command + ". Type help."
            );
        }
    }

    private void register() {
        String login = prompt("Login: ");
        String password = prompt("Password: ");

        User user = authService.register(login, password);

        output.println("User registered: " + user.getLogin());
    }

    private void login() {
        String login = prompt("Login: ");
        String password = prompt("Password: ");

        User user = authService.login(login, password);
        currentUser = user;

        output.println("Logged in as: " + user.getLogin());
    }

    private void logout() {
        requireUser();

        output.println("Logged out: " + currentUser.getLogin());
        currentUser = null;
    }

    private void addIncome() {
        User user = requireUser();

        BigDecimal amount = readAmount();
        String category = prompt("Category: ");

        FinanceOperationResult result = financeService.addIncome(
                user, amount, category
        );

        printOperationResult(result);
    }

    private void addExpense() {
        User user = requireUser();

        BigDecimal amount = readAmount();
        String category = prompt("Category: ");

        FinanceOperationResult result = financeService.addExpense(
                user, amount, category
        );

        printOperationResult(result);
    }

    private void createCategory() {
        Wallet wallet = requireUser().getWallet();

        TransactionType type = readTransactionType();
        String name = prompt("Category name: ");

        Category category = budgetService.createCategory(
                wallet, type, name
        );

        output.println("Category created: " + category.getName());
    }

    private void setBudget() {
        Wallet wallet = requireUser().getWallet();

        String categoryName = prompt("Expense category: ");
        BigDecimal limit = readAmount();

        Budget budget = budgetService.setBudget(
                wallet, categoryName, limit
        );

        output.println(
                "Budget saved: " + budget.getCategory().getName()
                        + " = " + budget.getLimit().toPlainString()
        );
    }

    private void showCategories() {
        Wallet wallet = requireUser().getWallet();

        printCategorySummary(wallet, TransactionType.INCOME);
        printCategorySummary(wallet, TransactionType.EXPENSE);
    }

    private void showBudgets() {
        Wallet wallet = requireUser().getWallet();
        printBudgetStatuses(wallet);
    }

    private void showReport() {
        Wallet wallet = requireUser().getWallet();

        output.println("=== Financial Report ===");

        output.println(
                "Total income: " + wallet.getTotalIncome().toPlainString()
        );

        output.println(
                "Total expenses: " + wallet.getTotalExpenses().toPlainString()
        );

        output.println(
                "Balance: " + wallet.getBalance().toPlainString()
        );

        printCategorySummary(wallet, TransactionType.INCOME);
        printCategorySummary(wallet, TransactionType.EXPENSE);
        printBudgetStatuses(wallet);
    }

    private void showSelectedTotal() {
        User user = requireUser();

        TransactionType type = readTransactionType();

        String input = prompt(
                "Categories (comma-separated): "
        );

        List<String> categories = Arrays.stream(input.split(",", -1))
                .map(String::trim)
                .toList();

        if (categories.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException(
                    "Category names cannot be empty"
            );
        }

        BigDecimal total = financeService.getTotalByCategories(
                user, type, categories
        );

        output.println("Selected total: " + total.toPlainString());
    }

    private void showTransactions() {
        List<Transaction> transactions = requireUser()
                .getWallet()
                .getTransactions();

        output.println("=== Transaction History ===");

        output.println(
                CliTableFormatter.formatTransactions(transactions)
        );
    }

    private void printCategorySummary(Wallet wallet,
                                      TransactionType type) {
        output.println("=== " + type + " Categories ===");

        List<Category> categories = wallet.getCategories(type);

        if (categories.isEmpty()) {
            output.println("No categories.");
            return;
        }

        for (Category category : categories) {
            BigDecimal total = wallet.getTotalByCategory(
                    type, category.getName()
            );

            output.printf(
                    "%s: %s%n",
                    category.getName(),
                    total.toPlainString()
            );
        }
    }

    private void printBudgetStatuses(Wallet wallet) {
        output.println("=== Budgets ===");

        List<BudgetStatus> statuses =
                budgetService.getAllStatuses(wallet);

        if (statuses.isEmpty()) {
            output.println("No budgets.");
            return;
        }

        for (BudgetStatus status : statuses) {
            output.printf(
                    "%s | Limit: %s | Spent: %s | Remaining: %s%n",
                    status.category(),
                    status.limit().toPlainString(),
                    status.spent().toPlainString(),
                    status.remaining().toPlainString()
            );

            budgetService.getAlert(wallet, status.category())
                    .ifPresent(alert ->
                            output.println("WARNING: " + alert));
        }
    }

    private void printOperationResult(FinanceOperationResult result) {
        Transaction transaction = result.transaction();

        output.printf(
                "%s recorded: %s (%s)%n",
                transaction.getType(),
                transaction.getAmount().toPlainString(),
                transaction.getCategory()
        );

        output.println(
                "Balance: " + result.balance().toPlainString()
        );

        result.alerts().forEach(alert ->
                output.println("WARNING: " + alert));
    }

    private TransactionType readTransactionType() {
        String value = prompt("Type (income/expense): ")
                .trim()
                .toUpperCase(Locale.ROOT);

        return switch (value) {
            case "INCOME" -> TransactionType.INCOME;
            case "EXPENSE" -> TransactionType.EXPENSE;
            default -> throw new IllegalArgumentException(
                    "Type must be income or expense"
            );
        };
    }

    private BigDecimal readAmount() {
        String value = prompt("Amount: ")
                .trim()
                .replace(',', '.');

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid amount. Example: 1250.50 or 1250,50"
            );
        }
    }

    private String prompt(String message) {
        output.print(message);
        output.flush();

        if (!scanner.hasNextLine()) {
            throw new NoSuchElementException("Input closed");
        }

        return scanner.nextLine();
    }

    private User requireUser() {
        if (currentUser == null) {
            throw new SecurityException("Please log in first");
        }

        return currentUser;
    }

    private void transfer() {
        User sender = requireUser();

        String recipientLogin = prompt("Recipient login: ");
        BigDecimal amount = readAmount();

        TransferResult result = transferService.transfer(
                sender,
                recipientLogin,
                amount
        );

        output.printf(
                "Transferred %s to %s%n",
                result.amount().toPlainString(),
                result.recipientLogin()
        );

        output.println(
                "Balance: " + result.senderBalance().toPlainString()
        );

        result.alerts().forEach(alert ->
                output.println("WARNING: " + alert)
        );
    }

    private void renameCategory() {
        Wallet wallet = requireUser().getWallet();

        TransactionType type = readTransactionType();
        String oldName = prompt("Current category: ");
        String newName = prompt("New category: ");

        Category category = budgetService.renameCategory(
                wallet, type, oldName, newName
        );

        output.println(
                "Category renamed: " + oldName.trim()
                        + " -> " + category.getName()
        );
    }

    private void deleteCategory() {
        Wallet wallet = requireUser().getWallet();

        TransactionType type = readTransactionType();
        String name = prompt("Category name: ");

        budgetService.deleteCategory(wallet, type, name);

        output.println("Category deleted: " + name.trim());
    }

    private void deleteBudget() {
        Wallet wallet = requireUser().getWallet();

        String categoryName = prompt("Expense category: ");

        budgetService.deleteBudget(wallet, categoryName);

        output.println(
                "Budget deleted: " + categoryName.trim()
        );
    }

    private void exportCsv() {
        User user = requireUser();
        String filename = readCsvFilename();

        int count = csvTransactionService.exportTransactions(
                user, Path.of(filename)
        );

        output.println(
                "Exported " + count + " transactions to " + filename
        );
    }

    private void importCsv() {
        User user = requireUser();
        String filename = readCsvFilename();

        int count = csvTransactionService.importTransactions(
                user, Path.of(filename)
        );

        output.println(
                "Imported " + count + " transactions from " + filename
        );
    }

    private void showBalance() {
        User user = requireUser();

        output.println(
                "Balance: "
                        + financeService.getBalance(user).toPlainString()
        );
    }

    private void showCurrentUser() {
        if (currentUser == null) {
            output.println("Not logged in.");
            return;
        }

        output.println(
                "Logged in as: " + currentUser.getLogin()
        );
    }


    private String readCsvFilename() {
        String filename = prompt("CSV file path: ").trim();

        if (filename.isEmpty()) {
            throw new IllegalArgumentException(
                    "CSV file path cannot be empty"
            );
        }

        if (!filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException(
                    "CSV file path must end with .csv"
            );
        }

        return filename;
    }

    private void printHelp() {
        output.println("=== Available Commands ===");
        output.println("register     - Register a new user");
        output.println("login        - Log in");
        output.println("logout       - Log out");
        output.println("income       - Add income");
        output.println("expense      - Add expense");
        output.println("category     - Create a category");
        output.println("categories   - Show categories");
        output.println("budget       - Create or update a budget");
        output.println("budgets      - Show budgets");
        output.println("report       - Show financial report");
        output.println("total        - Calculate selected categories");
        output.println("transactions - Show transaction history");
        output.println("transfer     - Transfer money to another user");
        output.println("rename-category - Rename a category");
        output.println("delete-category - Delete an unused category");
        output.println("delete-budget   - Remove a category budget");
        output.println("export-csv   - Export transactions to CSV");
        output.println("import-csv   - Import transactions from CSV");
        output.println("balance      - Show current balance");
        output.println("whoami       - Show current user");
        output.println("help         - Show this help");
        output.println("exit         - Exit application");
        output.println("Enter a command and follow the prompts.");
        output.println();
        output.println("=== Examples ===");
        output.println("register -> alice -> a-unique-password");
        output.println("income -> 1500,50 -> Salary");
        output.println("expense -> 500 -> Food");
        output.println("budget -> Food -> 4000");
        output.println("total -> expense -> Food, Taxi");
        output.println("export-csv -> exports/alice.csv");
        output.println();
        output.println(
                "Enter a command at '>' and respond to each prompt."
        );
    }
}
