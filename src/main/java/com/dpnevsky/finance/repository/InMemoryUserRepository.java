package com.dpnevsky.finance.repository;

import com.dpnevsky.finance.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> users = new HashMap<>();

    @Override
    public Optional<User> findByLogin(String login) {
        return Optional.ofNullable(users.get(login));
    }

    @Override
    public void save(User user) {
        Objects.requireNonNull(user, "User cannot be null");

        if (users.putIfAbsent(user.getLogin(), user) != null) {
            throw new IllegalArgumentException(
                    "User already exists"
            );
        }
    }
}
