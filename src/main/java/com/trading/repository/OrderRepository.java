package com.trading.repository;

import com.trading.domain.Order;
import com.trading.domain.OrderSide;
import com.trading.domain.OrderStatus;
import com.trading.domain.OrderType;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Order persistence and order history queries.
 */
public class OrderRepository {
    private final DatabaseManager dbManager;

    public OrderRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(Order order) {
        String sql = """
            INSERT INTO orders (id, user_id, portfolio_id, symbol, side, type, quantity, filled_quantity,
                               limit_price, stop_price, trailing_percent, status, created_at, filled_at, execution_price, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                filled_quantity = excluded.filled_quantity,
                status = excluded.status,
                filled_at = excluded.filled_at,
                execution_price = excluded.execution_price,
                notes = excluded.notes;
        """;

        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, order.getId());
            pstmt.setString(2, order.getUserId());
            pstmt.setString(3, order.getPortfolioId());
            pstmt.setString(4, order.getSymbol());
            pstmt.setString(5, order.getSide().name());
            pstmt.setString(6, order.getType().name());
            pstmt.setInt(7, order.getQuantity());
            pstmt.setInt(8, order.getFilledQuantity());
            if (order.getLimitPrice() != null) pstmt.setDouble(9, order.getLimitPrice()); else pstmt.setNull(9, Types.REAL);
            if (order.getStopPrice() != null) pstmt.setDouble(10, order.getStopPrice()); else pstmt.setNull(10, Types.REAL);
            if (order.getTrailingPercent() != null) pstmt.setDouble(11, order.getTrailingPercent()); else pstmt.setNull(11, Types.REAL);
            pstmt.setString(12, order.getStatus().name());
            pstmt.setString(13, order.getCreatedAt().toString());
            pstmt.setString(14, order.getFilledAt() != null ? order.getFilledAt().toString() : null);
            pstmt.setDouble(15, order.getExecutionPrice());
            pstmt.setString(16, order.getNotes());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save order: " + e.getMessage(), e);
        }
    }

    public List<Order> findByUserId(String userId) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToOrder(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find orders by user id: " + e.getMessage(), e);
        }
        return list;
    }

    private Order mapRowToOrder(ResultSet rs) throws SQLException {
        Double limit = rs.getObject("limit_price") != null ? rs.getDouble("limit_price") : null;
        Double stop = rs.getObject("stop_price") != null ? rs.getDouble("stop_price") : null;
        Double trailing = rs.getObject("trailing_percent") != null ? rs.getDouble("trailing_percent") : null;

        Order order = new Order(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("portfolio_id"),
                rs.getString("symbol"),
                OrderSide.valueOf(rs.getString("side")),
                OrderType.valueOf(rs.getString("type")),
                rs.getInt("quantity"),
                limit,
                stop,
                trailing
        );
        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
        order.setExecutionPrice(rs.getDouble("execution_price"));
        order.setNotes(rs.getString("notes"));
        String filledAt = rs.getString("filled_at");
        if (filledAt != null) {
            try {
                order.setFilledAt(LocalDateTime.parse(filledAt));
            } catch (Exception ignored) {}
        }
        return order;
    }
}
