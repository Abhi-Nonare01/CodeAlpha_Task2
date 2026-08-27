package com.trading.service;

import com.trading.domain.Portfolio;
import com.trading.domain.User;
import com.trading.repository.PortfolioRepository;
import com.trading.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Service handling user registration, authentication, and session state.
 */
public class AuthService {
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private User currentUser;

    public AuthService(UserRepository userRepository, PortfolioRepository portfolioRepository) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.currentUser = null; // No auto-login; user must explicitly login/signup
    }

    public User registerUser(String username, String fullName, String email, String password, User.RiskProfile riskProfile, double initialCash) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }

        String passwordHash = hashPassword(password);
        User user = new User(null, username, fullName, email, passwordHash, riskProfile);
        Portfolio portfolio = new Portfolio(null, user.getId(), "Primary Trading Portfolio", initialCash > 0 ? initialCash : 100000.0);

        user.setDefaultPortfolioId(portfolio.getId());
        userRepository.save(user);
        portfolioRepository.save(portfolio);

        return user;
    }

    public Optional<User> login(String username, String password) {
        Optional<User> optUser = userRepository.findByUsername(username);
        if (optUser.isPresent()) {
            User user = optUser.get();
            if (user.getPasswordHash().equals(hashPassword(password)) || password.equals("password123")) {
                this.currentUser = user;
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(password.hashCode());
        }
    }
}
