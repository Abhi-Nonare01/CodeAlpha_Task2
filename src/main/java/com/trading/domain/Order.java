package com.trading.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a user order on the exchange.
 * Supports MARKET, LIMIT, STOP_LOSS, STOP_LIMIT, and TRAILING_STOP orders.
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String userId;
    private final String portfolioId;
    private final String symbol;
    private final OrderSide side;
    private final OrderType type;
    private final int quantity;
    private int filledQuantity;
    private final Double limitPrice;
    private final Double stopPrice;
    private final Double trailingPercent;
    private double highWaterMarkPrice; // For trailing stop calculation
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime filledAt;
    private double executionPrice;
    private String notes;

    public Order(String id, String userId, String portfolioId, String symbol,
                 OrderSide side, OrderType type, int quantity,
                 Double limitPrice, Double stopPrice, Double trailingPercent) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString().substring(0, 8).toUpperCase() : id;
        this.userId = userId;
        this.portfolioId = portfolioId;
        this.symbol = symbol.toUpperCase();
        this.side = side;
        this.type = type;
        this.quantity = quantity;
        this.filledQuantity = 0;
        this.limitPrice = limitPrice;
        this.stopPrice = stopPrice;
        this.trailingPercent = trailingPercent;
        this.highWaterMarkPrice = (limitPrice != null) ? limitPrice : 0.0;
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.executionPrice = 0.0;
        this.notes = "";
    }

    public static Order createMarketOrder(String userId, String portfolioId, String symbol, OrderSide side, int quantity) {
        return new Order(null, userId, portfolioId, symbol, side, OrderType.MARKET, quantity, null, null, null);
    }

    public static Order createLimitOrder(String userId, String portfolioId, String symbol, OrderSide side, int quantity, double limitPrice) {
        return new Order(null, userId, portfolioId, symbol, side, OrderType.LIMIT, quantity, limitPrice, null, null);
    }

    public static Order createStopLossOrder(String userId, String portfolioId, String symbol, OrderSide side, int quantity, double stopPrice) {
        return new Order(null, userId, portfolioId, symbol, side, OrderType.STOP_LOSS, quantity, null, stopPrice, null);
    }

    public static Order createTrailingStopOrder(String userId, String portfolioId, String symbol, OrderSide side, int quantity, double trailingPercent) {
        return new Order(null, userId, portfolioId, symbol, side, OrderType.TRAILING_STOP, quantity, null, null, trailingPercent);
    }

    public synchronized void fill(double execPrice, int qty) {
        this.executionPrice = execPrice;
        this.filledQuantity = qty;
        this.status = OrderStatus.FILLED;
        this.filledAt = LocalDateTime.now();
    }

    public synchronized void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.notes = reason;
    }

    public synchronized void reject(String reason) {
        this.status = OrderStatus.REJECTED;
        this.notes = reason;
    }

    public synchronized void updateTrailingHighWaterMark(double currentPrice) {
        if (type == OrderType.TRAILING_STOP) {
            if (side == OrderSide.SELL && (highWaterMarkPrice == 0 || currentPrice > highWaterMarkPrice)) {
                this.highWaterMarkPrice = currentPrice;
            } else if (side == OrderSide.BUY && (highWaterMarkPrice == 0 || currentPrice < highWaterMarkPrice)) {
                this.highWaterMarkPrice = currentPrice;
            }
        }
    }

    public double getEffectiveTrailingStopPrice() {
        if (type != OrderType.TRAILING_STOP || trailingPercent == null || highWaterMarkPrice <= 0) {
            return 0.0;
        }
        if (side == OrderSide.SELL) {
            return highWaterMarkPrice * (1.0 - trailingPercent / 100.0);
        } else {
            return highWaterMarkPrice * (1.0 + trailingPercent / 100.0);
        }
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getPortfolioId() { return portfolioId; }
    public String getSymbol() { return symbol; }
    public OrderSide getSide() { return side; }
    public OrderType getType() { return type; }
    public int getQuantity() { return quantity; }
    public synchronized int getFilledQuantity() { return filledQuantity; }
    public Double getLimitPrice() { return limitPrice; }
    public Double getStopPrice() { return stopPrice; }
    public Double getTrailingPercent() { return trailingPercent; }
    public synchronized double getHighWaterMarkPrice() { return highWaterMarkPrice; }
    public synchronized OrderStatus getStatus() { return status; }
    public synchronized void setStatus(OrderStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public synchronized LocalDateTime getFilledAt() { return filledAt; }
    public synchronized void setFilledAt(LocalDateTime filledAt) { this.filledAt = filledAt; }
    public synchronized double getExecutionPrice() { return executionPrice; }
    public synchronized void setExecutionPrice(double executionPrice) { this.executionPrice = executionPrice; }
    public synchronized String getNotes() { return notes; }
    public synchronized void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return String.format("Order[%s %s %d %s @ %s | Status: %s]",
                id, side, quantity, symbol,
                type == OrderType.MARKET ? "MKT" : (limitPrice != null ? "$" + limitPrice : "$" + stopPrice),
                status);
    }
}
