package com.dpnevsky.finance.model;

import java.util.Objects;

public class User {

    private final String login;
    private final String passwordHash;
    private final Wallet wallet;

    public User(String login, String passwordHash) {
        this.login = Objects.requireNonNull(login);
        this.passwordHash = Objects.requireNonNull(passwordHash);

        if (login.isBlank() || passwordHash.isBlank()) {
            throw new IllegalArgumentException(
                    "Login and password hash cannot be empty"
            );
        }

        this.wallet = new Wallet();
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Wallet getWallet() {
        return wallet;
    }
}
