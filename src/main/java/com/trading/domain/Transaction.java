package com.trading.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable transaction record representing an executed trade, cash transfer, or dividend.
 */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        BUY,
        SELL,
        DIVIDEND,
        DEPOSIT,
        WITHDRAWAL
    }

    private final String id;
    private final String userId;
    private final String portfolioId;
    private final String orderId;
    private final String symbol;
    private final Type type;
    private final int quantity;
    private final double price;
    private final double totalAmount;
    private final double fee;
    private final LocalDateTime timestamp;
    private final String notes;

    public Transaction(String id, String userId, String portfolioId, String orderId,
                       String symbol, Type type, int quantity, double price,
                       double totalAmount, double fee, LocalDateTime timestamp, String notes) {
        this.id = (id == null || id.isBlank()) ? "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() : id;
        this.userId = userId;
        this.portfolioId = portfolioId;
        this.orderId = orderId;
        this.symbol = (symbol != null) ? symbol.toUpperCase() : "";
        this.type = type;
        this.quantity = quantity;
        this.price = Math.round(price * 100.0) / 100.0;
        this.totalAmount = Math.round(totalAmount * 100.0) / 100.0;
        this.fee = Math.round(fee * 100.0) / 100.0;
        this.timestamp = (timestamp != null) ? timestamp : LocalDateTime.now();
        this.notes = (notes != null) ? notes : "";
    }

    public static Transaction createTrade(String userId, String portfolioId, String orderId,
                                          String symbol, Type type, int quantity,
                                          double price, double fee, String notes) {
        double total = (quantity * price) + (type == Type.BUY ? fee : -fee);
        return new Transaction(null, userId, portfolioId, orderId, symbol, type, quantity, price, total, fee, LocalDateTime.now(), notes);
    }

    public static Transaction createCashFlow(String userId, String portfolioId, Type type, double amount, String notes) {
        return new Transaction(null, userId, portfolioId, null, "CASH", type, 1, amount, amount, 0.0, LocalDateTime.now(), notes);
    }

    public static Transaction createDividend(String userId, String portfolioId, String symbol, double amount, String notes) {
        return new Transaction(null, userId, portfolioId, null, symbol, Type.DIVIDEND, 0, amount, amount, 0.0, LocalDateTime.now(), notes);
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getPortfolioId() { return portfolioId; }
    public String getOrderId() { return orderId; }
    public String getSymbol() { return symbol; }
    public Type getType() { return type; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public double getTotalAmount() { return totalAmount; }
    public double getFee() { return fee; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getNotes() { return notes; }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s %s %d @ $%.2f (Total: $%.2f, Fee: $%.2f)",
                timestamp.toLocalTime().toString().substring(0, 8), id, type, symbol, quantity, price, totalAmount, fee);
    }
}
