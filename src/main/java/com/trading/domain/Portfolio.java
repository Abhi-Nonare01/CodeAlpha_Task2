package com.trading.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a user's investment portfolio managing cash balance, stock holdings,
 * asset allocation, and performance tracking snapshots over time.
 */
public class Portfolio implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class EquityPoint implements Serializable {
        private static final long serialVersionUID = 1L;
        private final LocalDateTime timestamp;
        private final double equity;
        private final double cash;
        private final double holdingsValue;

        public EquityPoint(LocalDateTime timestamp, double equity, double cash, double holdingsValue) {
            this.timestamp = timestamp;
            this.equity = Math.round(equity * 100.0) / 100.0;
            this.cash = Math.round(cash * 100.0) / 100.0;
            this.holdingsValue = Math.round(holdingsValue * 100.0) / 100.0;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public double getEquity() { return equity; }
        public double getCash() { return cash; }
        public double getHoldingsValue() { return holdingsValue; }
    }

    private final String id;
    private final String userId;
    private String name;
    private double cashBalance;
    private final double initialCash;
    private final Map<String, Holding> holdings;
    private final List<EquityPoint> equityHistory;

    public Portfolio(String id, String userId, String name, double initialCash) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString().substring(0, 8).toUpperCase() : id;
        this.userId = userId;
        this.name = name;
        this.cashBalance = initialCash;
        this.initialCash = initialCash;
        this.holdings = new ConcurrentHashMap<>();
        this.equityHistory = Collections.synchronizedList(new ArrayList<>());
        recordEquitySnapshot(initialCash, initialCash, 0.0);
    }

    public synchronized void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be strictly positive");
        }
        this.cashBalance += amount;
    }

    public synchronized void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be strictly positive");
        }
        if (amount > this.cashBalance) {
            throw new IllegalStateException("Insufficient funds for withdrawal. Available cash: $" + String.format("%.2f", cashBalance));
        }
        this.cashBalance -= amount;
    }

    public synchronized void deductCash(double amount) {
        if (amount > this.cashBalance) {
            throw new IllegalStateException("Insufficient cash. Required: $" + String.format("%.2f", amount) + ", Available: $" + String.format("%.2f", cashBalance));
        }
        this.cashBalance -= amount;
    }

    public synchronized void addCash(double amount) {
        this.cashBalance += amount;
    }

    public Holding getHolding(String symbol) {
        return holdings.get(symbol.toUpperCase());
    }

    public Holding getOrCreateHolding(String symbol) {
        return holdings.computeIfAbsent(symbol.toUpperCase(), Holding::new);
    }

    public void addHolding(Holding holding) {
        holdings.put(holding.getSymbol().toUpperCase(), holding);
    }

    public void removeHoldingIfEmpty(String symbol) {
        Holding h = holdings.get(symbol.toUpperCase());
        if (h != null && h.getQuantity() == 0 && h.getRealizedPnL() == 0 && h.getTotalDividendsReceived() == 0) {
            holdings.remove(symbol.toUpperCase());
        }
    }

    public synchronized double getTotalHoldingsValue(Map<String, Stock> marketStocks) {
        double total = 0.0;
        for (Holding holding : holdings.values()) {
            if (holding.getQuantity() > 0) {
                Stock stock = marketStocks.get(holding.getSymbol());
                double price = (stock != null) ? stock.getCurrentPrice() : 0.0;
                total += holding.getMarketValue(price);
            }
        }
        return Math.round(total * 100.0) / 100.0;
    }

    public synchronized double getTotalEquity(Map<String, Stock> marketStocks) {
        return Math.round((cashBalance + getTotalHoldingsValue(marketStocks)) * 100.0) / 100.0;
    }

    public synchronized double getTotalRealizedPnL() {
        double total = 0.0;
        for (Holding holding : holdings.values()) {
            total += holding.getRealizedPnL();
        }
        return Math.round(total * 100.0) / 100.0;
    }

    public synchronized double getTotalUnrealizedPnL(Map<String, Stock> marketStocks) {
        double total = 0.0;
        for (Holding holding : holdings.values()) {
            if (holding.getQuantity() > 0) {
                Stock stock = marketStocks.get(holding.getSymbol());
                double price = (stock != null) ? stock.getCurrentPrice() : 0.0;
                total += holding.getUnrealizedPnL(price);
            }
        }
        return Math.round(total * 100.0) / 100.0;
    }

    public synchronized double getTotalDividends() {
        double total = 0.0;
        for (Holding holding : holdings.values()) {
            total += holding.getTotalDividendsReceived();
        }
        return Math.round(total * 100.0) / 100.0;
    }

    public synchronized double getTotalReturnPercent(Map<String, Stock> marketStocks) {
        if (initialCash <= 0) return 0.0;
        double currentEquity = getTotalEquity(marketStocks);
        return Math.round(((currentEquity - initialCash) / initialCash * 100.0) * 100.0) / 100.0;
    }

    public synchronized void recordEquitySnapshot(double equity, double cash, double holdingsVal) {
        if (equityHistory.size() >= 500) {
            equityHistory.remove(0);
        }
        equityHistory.add(new EquityPoint(LocalDateTime.now(), equity, cash, holdingsVal));
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public synchronized double getCashBalance() { return Math.round(cashBalance * 100.0) / 100.0; }
    public double getInitialCash() { return initialCash; }
    public Map<String, Holding> getHoldings() { return Collections.unmodifiableMap(holdings); }
    public List<EquityPoint> getEquityHistory() { return Collections.unmodifiableList(new ArrayList<>(equityHistory)); }
}
