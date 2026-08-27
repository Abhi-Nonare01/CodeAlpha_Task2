package com.trading.repository;

import com.trading.domain.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface & SQLite implementation for User accounts.
 */
public class UserRepository {
    private final DatabaseManager dbManager;

    public UserRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(User user) {
        String sql = """
            INSERT INTO users (id, username, full_name, email, password_hash, default_portfolio_id, risk_profile, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                username = excluded.username,
                full_name = excluded.full_name,
                email = excluded.email,
                password_hash = excluded.password_hash,
                default_portfolio_id = excluded.default_portfolio_id,
                risk_profile = excluded.risk_profile;
        """;

        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getPasswordHash());
            pstmt.setString(6, user.getDefaultPortfolioId());
            pstmt.setString(7, user.getRiskProfile() != null ? user.getRiskProfile().name() : "MODERATE");
            pstmt.setString(8, user.getCreatedAt().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + e.getMessage(), e);
        }
    }

    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query user by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query user by username: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at ASC";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query all users: " + e.getMessage(), e);
        }
        return list;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        String riskStr = rs.getString("risk_profile");
        User.RiskProfile profile = User.RiskProfile.MODERATE;
        if (riskStr != null) {
            try { profile = User.RiskProfile.valueOf(riskStr); } catch (Exception ignored) {}
        }
        User user = new User(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                profile
        );
        user.setDefaultPortfolioId(rs.getString("default_portfolio_id"));
        return user;
    }
}
