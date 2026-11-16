package com.dpnevsky.finance.service;

import com.dpnevsky.finance.model.FinanceOperationResult;
import com.dpnevsky.finance.model.TransferResult;
import com.dpnevsky.finance.model.User;
import com.dpnevsky.finance.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Objects;

public class TransferService {

    private final UserRepository userRepository;
    private final FinanceService financeService;

    public TransferService(UserRepository userRepository,
                           FinanceService financeService) {
        this.userRepository = Objects.requireNonNull(
                userRepository, "User repository cannot be null"
        );

        this.financeService = Objects.requireNonNull(
                financeService, "Finance service cannot be null"
        );
    }

    public TransferResult transfer(User sender,
                                   String recipientLogin,
                                   BigDecimal amount) {
        Objects.requireNonNull(sender, "Sender cannot be null");

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be greater than zero"
            );
        }

        if (recipientLogin == null || recipientLogin.isBlank()) {
            throw new IllegalArgumentException(
                    "Recipient login cannot be empty"
            );
        }

        String normalizedLogin = recipientLogin.trim();

        User registeredSender = userRepository
                .findByLogin(sender.getLogin())
                .orElseThrow(() -> new SecurityException(
                        "Sender is not registered"
                ));

        if (registeredSender != sender) {
            throw new SecurityException(
                    "Invalid authenticated sender"
            );
        }

        if (sender.getLogin().equals(normalizedLogin)) {
            throw new IllegalArgumentException(
                    "Cannot transfer money to yourself"
            );
        }

        User recipient = userRepository
                .findByLogin(normalizedLogin)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recipient not found: " + normalizedLogin
                ));

        if (financeService.getBalance(sender).compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient funds"
            );
        }

        FinanceOperationResult outgoing = financeService.addExpense(
                sender,
                amount,
                "Transfer to " + recipient.getLogin()
        );

        financeService.addIncome(
                recipient,
                amount,
                "Transfer from " + sender.getLogin()
        );

        return new TransferResult(
                sender.getLogin(),
                recipient.getLogin(),
                amount,
                outgoing.balance(),
                outgoing.alerts()
        );
    }
}
