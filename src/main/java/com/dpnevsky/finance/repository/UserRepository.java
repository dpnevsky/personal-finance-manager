package com.dpnevsky.finance.repository;

import com.dpnevsky.finance.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByLogin(String login);

    void save(User user);

    List<User> findAll();
}
