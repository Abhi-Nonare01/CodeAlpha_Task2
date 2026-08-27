package com.trading.repository;

import com.trading.domain.Holding;
import com.trading.domain.Portfolio;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Portfolio and Holding entities with transactional consistency.
 */
public class PortfolioRepository {
    private final DatabaseManager dbManager;

    public PortfolioRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(Portfolio portfolio) {
        String upsertPortfolio = """
            INSERT INTO portfolios (id, user_id, name, cash_balance, initial_cash, created_at)
            VALUES (?, ?, ?, ?, ?, datetime('now'))
            ON CONFLICT(id) DO UPDATE SET
                name = excluded.name,
                cash_balance = excluded.cash_balance;
        """;

        String deleteHoldings = "DELETE FROM holdings WHERE portfolio_id = ?";
        String insertHolding = """
            INSERT INTO holdings (portfolio_id, symbol, quantity, total_cost_basis, realized_pnl, dividends_received)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Upsert portfolio record
                try (PreparedStatement pstmt = conn.prepareStatement(upsertPortfolio)) {
                    pstmt.setString(1, portfolio.getId());
                    pstmt.setString(2, portfolio.getUserId());
                    pstmt.setString(3, portfolio.getName());
                    pstmt.setDouble(4, portfolio.getCashBalance());
                    pstmt.setDouble(5, portfolio.getInitialCash());
                    pstmt.executeUpdate();
                }

                // Refresh holdings
                try (PreparedStatement pstmt = conn.prepareStatement(deleteHoldings)) {
                    pstmt.setString(1, portfolio.getId());
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(insertHolding)) {
                    for (Holding h : portfolio.getHoldings().values()) {
                        if (h.getQuantity() > 0 || h.getRealizedPnL() != 0 || h.getTotalDividendsReceived() != 0) {
                            pstmt.setString(1, portfolio.getId());
                            pstmt.setString(2, h.getSymbol());
                            pstmt.setInt(3, h.getQuantity());
                            pstmt.setDouble(4, h.getTotalCostBasis());
                            pstmt.setDouble(5, h.getRealizedPnL());
                            pstmt.setDouble(6, h.getTotalDividendsReceived());
                            pstmt.addBatch();
                        }
                    }
                    pstmt.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save portfolio: " + e.getMessage(), e);
        }
    }

    public Optional<Portfolio> findById(String id) {
        String sql = "SELECT * FROM portfolios WHERE id = ?";
        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Portfolio p = new Portfolio(
                            rs.getString("id"),
                            rs.getString("user_id"),
                            rs.getString("name"),
                            rs.getDouble("initial_cash")
                    );
                    // Adjust cash balance to stored value
                    double storedCash = rs.getDouble("cash_balance");
                    p.deductCash(p.getCashBalance());
                    p.addCash(storedCash);

                    // Load holdings
                    loadHoldingsForPortfolio(conn, p);
                    return Optional.of(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find portfolio by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Portfolio> findByUserId(String userId) {
        List<Portfolio> list = new ArrayList<>();
        String sql = "SELECT * FROM portfolios WHERE user_id = ? ORDER BY created_at ASC";
        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Portfolio p = new Portfolio(
                            rs.getString("id"),
                            rs.getString("user_id"),
                            rs.getString("name"),
                            rs.getDouble("initial_cash")
                    );
                    double storedCash = rs.getDouble("cash_balance");
                    p.deductCash(p.getCashBalance());
                    p.addCash(storedCash);
                    loadHoldingsForPortfolio(conn, p);
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query portfolios for user: " + e.getMessage(), e);
        }
        return list;
    }

    private void loadHoldingsForPortfolio(Connection conn, Portfolio portfolio) throws SQLException {
        String sql = "SELECT * FROM holdings WHERE portfolio_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, portfolio.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Holding h = new Holding(
                            rs.getString("symbol"),
                            rs.getInt("quantity"),
                            rs.getDouble("total_cost_basis"),
                            rs.getDouble("realized_pnl"),
                            rs.getDouble("dividends_received")
                    );
                    portfolio.addHolding(h);
                }
            }
        }
    }
}
