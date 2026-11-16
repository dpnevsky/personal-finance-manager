package com.dpnevsky.finance.service;

import com.dpnevsky.finance.model.User;
import com.dpnevsky.finance.repository.InMemoryUserRepository;
import com.dpnevsky.finance.repository.UserRepository;
import com.dpnevsky.finance.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private final UserRepository repository =
            new InMemoryUserRepository();

    private final PasswordHasher passwordHasher =
            new PasswordHasher();

    private final AuthService authService =
            new AuthService(repository, passwordHasher);

    @Test
    void shouldRegisterUser() {
        User user = authService.register(
                "alice", "password123"
        );

        assertEquals("alice", user.getLogin());
        assertNotEquals("password123", user.getPasswordHash());
        assertNotNull(user.getWallet());
        assertTrue(repository.findByLogin("alice").isPresent());
    }

    @Test
    void shouldLoginWithCorrectCredentials() {
        User registered = authService.register(
                "alice", "password123"
        );

        User authenticated = authService.login(
                "alice", "password123"
        );

        assertSame(registered, authenticated);
    }

    @Test
    void shouldRejectWrongPassword() {
        authService.register("alice", "password123");

        assertThrows(SecurityException.class, () ->
                authService.login("alice", "wrongpassword"));
    }

    @Test
    void shouldRejectUnknownUser() {
        assertThrows(SecurityException.class, () ->
                authService.login("unknown", "password123"));
    }

    @Test
    void shouldRejectDuplicateLogin() {
        authService.register("alice", "password123");

        assertThrows(IllegalArgumentException.class, () ->
                authService.register("alice", "anotherpassword"));
    }

    @Test
    void shouldKeepWalletsSeparate() {
        User alice = authService.register(
                "alice", "password123"
        );

        User bob = authService.register(
                "bob", "password456"
        );

        alice.getWallet().addIncome(
                new BigDecimal("5000"), "Salary"
        );

        assertEquals(
                new BigDecimal("5000"),
                alice.getWallet().getBalance()
        );

        assertEquals(
                BigDecimal.ZERO,
                bob.getWallet().getBalance()
        );
    }

    @Test
    void shouldRejectInvalidLogin() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.register("ab", "password123"));

        assertThrows(IllegalArgumentException.class, () ->
                authService.register("bad login", "password123"));

        assertThrows(IllegalArgumentException.class, () ->
                authService.register(null, "password123"));
    }

    @Test
    void shouldRejectInvalidPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.register("alice", "123"));

        assertThrows(IllegalArgumentException.class, () ->
                authService.register("alice", null));

        assertThrows(IllegalArgumentException.class, () ->
                authService.register("alice", "        "));
    }

    @Test
    void shouldGenerateDifferentPasswordHashes() {
        String first = passwordHasher.hash("password123");
        String second = passwordHasher.hash("password123");

        assertNotEquals(first, second);
        assertTrue(passwordHasher.matches("password123", first));
        assertTrue(passwordHasher.matches("password123", second));
    }

    @Test
    void shouldAuthenticateUserWithStoredHash() {
        String hash = passwordHasher.hash("password123");

        repository.save(new User("alice", hash));

        User user = authService.login("alice", "password123");

        assertEquals("alice", user.getLogin());
    }
}
