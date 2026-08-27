package com.trading.repository;

import com.trading.domain.Transaction;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for transaction logs and financial audit trails.
 */
public class TransactionRepository {
    private final DatabaseManager dbManager;

    public TransactionRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(Transaction tx) {
        String sql = """
            INSERT INTO transactions (id, user_id, portfolio_id, order_id, symbol, type, quantity, price, total_amount, fee, timestamp, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tx.getId());
            pstmt.setString(2, tx.getUserId());
            pstmt.setString(3, tx.getPortfolioId());
            pstmt.setString(4, tx.getOrderId());
            pstmt.setString(5, tx.getSymbol());
            pstmt.setString(6, tx.getType().name());
            pstmt.setInt(7, tx.getQuantity());
            pstmt.setDouble(8, tx.getPrice());
            pstmt.setDouble(9, tx.getTotalAmount());
            pstmt.setDouble(10, tx.getFee());
            pstmt.setString(11, tx.getTimestamp().toString());
            pstmt.setString(12, tx.getNotes());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save transaction: " + e.getMessage(), e);
        }
    }

    public List<Transaction> findByPortfolioId(String portfolioId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE portfolio_id = ? ORDER BY timestamp DESC";
        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, portfolioId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find transactions by portfolio id: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Transaction> findByUserId(String userId) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY timestamp DESC";
        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find transactions by user id: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Transaction> findAll() {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToTransaction(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all transactions: " + e.getMessage(), e);
        }
        return list;
    }

    private Transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        LocalDateTime ts = LocalDateTime.now();
        try {
            ts = LocalDateTime.parse(rs.getString("timestamp"));
        } catch (Exception ignored) {}

        return new Transaction(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("portfolio_id"),
                rs.getString("order_id"),
                rs.getString("symbol"),
                Transaction.Type.valueOf(rs.getString("type")),
                rs.getInt("quantity"),
                rs.getDouble("price"),
                rs.getDouble("total_amount"),
                rs.getDouble("fee"),
                ts,
                rs.getString("notes")
        );
    }
}
