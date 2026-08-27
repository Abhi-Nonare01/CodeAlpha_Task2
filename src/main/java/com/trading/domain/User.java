package com.trading.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a registered trader/investor on the platform.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum RiskProfile {
        CONSERVATIVE,
        MODERATE,
        AGGRESSIVE
    }

    private final String id;
    private final String username;
    private String fullName;
    private String email;
    private String passwordHash;
    private String defaultPortfolioId;
    private RiskProfile riskProfile;
    private final LocalDateTime createdAt;

    public User(String id, String username, String fullName, String email, String passwordHash, RiskProfile riskProfile) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString().substring(0, 8).toUpperCase() : id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.riskProfile = (riskProfile != null) ? riskProfile : RiskProfile.MODERATE;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDefaultPortfolioId() { return defaultPortfolioId; }
    public void setDefaultPortfolioId(String defaultPortfolioId) { this.defaultPortfolioId = defaultPortfolioId; }
    public RiskProfile getRiskProfile() { return riskProfile; }
    public void setRiskProfile(RiskProfile riskProfile) { this.riskProfile = riskProfile; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return String.format("User[%s (@%s), Profile: %s]", fullName, username, riskProfile);
    }
}
