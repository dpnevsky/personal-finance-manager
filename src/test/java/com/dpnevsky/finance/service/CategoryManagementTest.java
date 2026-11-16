package com.dpnevsky.finance.service;

import com.dpnevsky.finance.model.Category;
import com.dpnevsky.finance.model.Transaction;
import com.dpnevsky.finance.model.TransactionType;
import com.dpnevsky.finance.model.User;
import com.dpnevsky.finance.model.Wallet;
import com.dpnevsky.finance.storage.JsonFileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryManagementTest {

    private final BudgetService service = new BudgetService();
    private final Wallet wallet = new Wallet();

    @Test
    void shouldRenameCategoryAndPreserveTransactionData() {
        Transaction original = wallet.addExpense(
                new BigDecimal("500"), "Food"
        );

        Category renamed = service.renameCategory(
                wallet, TransactionType.EXPENSE,
                "Food", "Groceries"
        );

        Transaction updated = wallet.getTransactions().get(0);

        assertEquals("Groceries", renamed.getName());
        assertEquals("Groceries", updated.getCategory());
        assertEquals(original.getId(), updated.getId());
        assertEquals(original.getCreatedAt(), updated.getCreatedAt());
        assertEquals(original.getAmount(), updated.getAmount());
        assertEquals(original.getType(), updated.getType());

        assertTrue(wallet.findCategory(
                TransactionType.EXPENSE, "Food"
        ).isEmpty());
    }

    @Test
    void shouldMoveBudgetToRenamedCategory() {
        wallet.addExpense(new BigDecimal("800"), "Food");

        service.setBudget(
                wallet, "Food", new BigDecimal("4000")
        );

        service.renameCategory(
                wallet, TransactionType.EXPENSE,
                "Food", "Groceries"
        );

        assertTrue(wallet.findBudget("Food").isEmpty());

        assertEquals(
                new BigDecimal("4000"),
                service.getStatus(wallet, "Groceries").limit()
        );

        assertEquals(
                new BigDecimal("800"),
                service.getStatus(wallet, "Groceries").spent()
        );

        assertEquals(
                new BigDecimal("3200"),
                service.getStatus(wallet, "Groceries").remaining()
        );
    }

    @Test
    void shouldPreserveCategoryOrderAfterRename() {
        wallet.createCategory(TransactionType.EXPENSE, "Food");
        wallet.createCategory(TransactionType.EXPENSE, "Taxi");

        service.renameCategory(
                wallet, TransactionType.EXPENSE,
                "Food", "Groceries"
        );

        List<String> names = wallet.getCategories(
                TransactionType.EXPENSE
        ).stream().map(Category::getName).toList();

        assertEquals(List.of("Groceries", "Taxi"), names);
    }

    @Test
    void shouldOnlyRenameCategoryOfSelectedType() {
        wallet.addIncome(new BigDecimal("300"), "Other");
        wallet.addExpense(new BigDecimal("100"), "Other");

        service.renameCategory(
                wallet, TransactionType.EXPENSE,
                "Other", "Miscellaneous"
        );

        assertEquals(
                new BigDecimal("300"),
                wallet.getTotalByCategory(
                        TransactionType.INCOME, "Other"
                )
        );

        assertEquals(
                new BigDecimal("100"),
                wallet.getTotalByCategory(
                        TransactionType.EXPENSE, "Miscellaneous"
                )
        );
    }

    @Test
    void shouldRejectRenamingUnknownCategory() {
        assertThrows(IllegalArgumentException.class, () ->
                service.renameCategory(
                        wallet, TransactionType.EXPENSE,
                        "Unknown", "New"
                ));
    }

    @Test
    void shouldRejectRenamingToExistingCategory() {
        wallet.createCategory(TransactionType.EXPENSE, "Food");
        wallet.createCategory(TransactionType.EXPENSE, "Taxi");

        assertThrows(IllegalArgumentException.class, () ->
                service.renameCategory(
                        wallet, TransactionType.EXPENSE,
                        "Food", "Taxi"
                ));

        assertEquals(
                2, wallet.getCategories(TransactionType.EXPENSE).size()
        );
    }

    @Test
    void shouldRejectBlankNewCategoryName() {
        wallet.createCategory(TransactionType.EXPENSE, "Food");

        assertThrows(IllegalArgumentException.class, () ->
                service.renameCategory(
                        wallet, TransactionType.EXPENSE,
                        "Food", "   "
                ));
    }

    @Test
    void shouldRejectRenamingToSameName() {
        wallet.createCategory(TransactionType.EXPENSE, "Food");

        assertThrows(IllegalArgumentException.class, () ->
                service.renameCategory(
                        wallet, TransactionType.EXPENSE,
                        "Food", "Food"
                ));
    }

    @Test
    void shouldDeleteUnusedCategory() {
        wallet.createCategory(
                TransactionType.EXPENSE, "Entertainment"
        );

        service.deleteCategory(
                wallet, TransactionType.EXPENSE, "Entertainment"
        );

        assertTrue(wallet.findCategory(
                TransactionType.EXPENSE, "Entertainment"
        ).isEmpty());
    }

    @Test
    void shouldRejectDeletingCategoryWithTransactions() {
        wallet.addExpense(new BigDecimal("500"), "Food");

        assertThrows(IllegalArgumentException.class, () ->
                service.deleteCategory(
                        wallet, TransactionType.EXPENSE, "Food"
                ));

        assertEquals(1, wallet.getTransactions().size());
    }

    @Test
    void shouldRejectDeletingCategoryWithBudget() {
        wallet.createCategory(TransactionType.EXPENSE, "Food");
        service.setBudget(
                wallet, "Food", new BigDecimal("4000")
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.deleteCategory(
                        wallet, TransactionType.EXPENSE, "Food"
                ));

        assertTrue(wallet.findBudget("Food").isPresent());
    }

    @Test
    void shouldDeleteBudgetAndThenCategory() {
        wallet.createCategory(TransactionType.EXPENSE, "Food");

        service.setBudget(
                wallet, "Food", new BigDecimal("4000")
        );

        service.deleteBudget(wallet, "Food");

        assertTrue(wallet.findBudget("Food").isEmpty());
        assertTrue(wallet.findCategory(
                TransactionType.EXPENSE, "Food"
        ).isPresent());

        service.deleteCategory(
                wallet, TransactionType.EXPENSE, "Food"
        );

        assertTrue(wallet.getCategories(
                TransactionType.EXPENSE
        ).isEmpty());
    }

    @Test
    void shouldRejectDeletingUnknownBudget() {
        assertThrows(IllegalArgumentException.class, () ->
                service.deleteBudget(wallet, "Unknown"));
    }

    @Test
    void shouldNotAffectOtherUsersWallet() {
        Wallet otherWallet = new Wallet();

        wallet.addExpense(new BigDecimal("300"), "Food");
        otherWallet.addExpense(new BigDecimal("500"), "Food");

        service.renameCategory(
                wallet, TransactionType.EXPENSE,
                "Food", "Groceries"
        );

        assertTrue(otherWallet.findCategory(
                TransactionType.EXPENSE, "Food"
        ).isPresent());

        assertEquals(
                new BigDecimal("500"),
                otherWallet.getTotalByCategory(
                        TransactionType.EXPENSE, "Food"
                )
        );
    }

    @Test
    void shouldPersistRenamedCategory(@TempDir Path tempDir) {
        User user = new User("alice", "stored-hash");

        Wallet userWallet = user.getWallet();

        userWallet.addExpense(
                new BigDecimal("800"), "Food"
        );

        service.setBudget(
                userWallet, "Food", new BigDecimal("4000")
        );

        service.renameCategory(
                userWallet, TransactionType.EXPENSE,
                "Food", "Groceries"
        );

        JsonFileStorage storage = new JsonFileStorage(
                tempDir.resolve("users.json")
        );

        storage.save(List.of(user));

        Wallet restored = storage.load().get(0).getWallet();

        assertTrue(restored.findCategory(
                TransactionType.EXPENSE, "Groceries"
        ).isPresent());

        assertEquals(
                new BigDecimal("800"),
                restored.getTotalByCategory(
                        TransactionType.EXPENSE, "Groceries"
                )
        );

        assertEquals(
                new BigDecimal("3200"),
                service.getStatus(
                        restored, "Groceries"
                ).remaining()
        );
    }
}
