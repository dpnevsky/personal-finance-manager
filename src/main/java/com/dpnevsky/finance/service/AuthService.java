package com.dpnevsky.finance.service;

import com.dpnevsky.finance.model.User;
import com.dpnevsky.finance.repository.UserRepository;
import com.dpnevsky.finance.security.PasswordHasher;

import java.util.Objects;

public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository userRepository,
                       PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
    }

    public User register(String login, String password) {
        String normalizedLogin = validateLogin(login);
        validatePassword(password);

        if (userRepository.findByLogin(normalizedLogin).isPresent()) {
            throw new IllegalArgumentException(
                    "User already exists"
            );
        }

        String passwordHash = passwordHasher.hash(password);
        User user = new User(normalizedLogin, passwordHash);

        userRepository.save(user);

        return user;
    }

    public User login(String login, String password) {
        if (login == null || password == null) {
            throw new SecurityException(
                    "Invalid login or password"
            );
        }

        User user = userRepository.findByLogin(login.trim())
                .orElseThrow(() -> new SecurityException(
                        "Invalid login or password"
                ));

        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            throw new SecurityException(
                    "Invalid login or password"
            );
        }

        return user;
    }

    private String validateLogin(String login) {
        if (login == null) {
            throw new IllegalArgumentException(
                    "Login cannot be null"
            );
        }

        String normalized = login.trim();

        if (!normalized.matches("[\\p{L}\\p{N}._-]{3,32}")) {
            throw new IllegalArgumentException(
                    "Login must contain 3-32 valid characters"
            );
        }

        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null
                || password.isBlank()
                || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters"
            );
        }
    }
}
