package com.trading.domain;

import java.io.Serializable;

/**
 * Represents a user's holding / position in a specific stock.
 * Uses weighted average cost basis accounting for precision and P&L tracking.
 */
public class Holding implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String symbol;
    private int quantity;
    private double totalCostBasis;
    private double realizedPnL;
    private double totalDividendsReceived;

    public Holding(String symbol) {
        this.symbol = symbol.toUpperCase();
        this.quantity = 0;
        this.totalCostBasis = 0.0;
        this.realizedPnL = 0.0;
        this.totalDividendsReceived = 0.0;
    }

    public Holding(String symbol, int quantity, double totalCostBasis, double realizedPnL, double totalDividendsReceived) {
        this.symbol = symbol.toUpperCase();
        this.quantity = quantity;
        this.totalCostBasis = totalCostBasis;
        this.realizedPnL = realizedPnL;
        this.totalDividendsReceived = totalDividendsReceived;
    }

    public synchronized void addShares(int additionalQuantity, double pricePerShare, double fee) {
        if (additionalQuantity <= 0) return;
        double cost = (additionalQuantity * pricePerShare) + fee;
        this.totalCostBasis += cost;
        this.quantity += additionalQuantity;
    }

    public synchronized double removeShares(int removeQuantity, double pricePerShare, double fee) {
        if (removeQuantity <= 0 || removeQuantity > this.quantity) {
            throw new IllegalArgumentException("Cannot sell more shares than currently owned. Owned: " + quantity + ", Requested: " + removeQuantity);
        }

        double avgCostPerShare = getAverageCostBasis();
        double costBasisOfSoldShares = avgCostPerShare * removeQuantity;
        double proceeds = (removeQuantity * pricePerShare) - fee;
        double profitFromSale = proceeds - costBasisOfSoldShares;

        this.realizedPnL += profitFromSale;
        this.totalCostBasis -= costBasisOfSoldShares;
        this.quantity -= removeQuantity;

        if (this.quantity == 0) {
            this.totalCostBasis = 0.0;
        }

        return profitFromSale;
    }

    public synchronized void addDividend(double amount) {
        if (amount > 0) {
            this.totalDividendsReceived += amount;
        }
    }

    public synchronized double getAverageCostBasis() {
        if (quantity == 0) return 0.0;
        return totalCostBasis / quantity;
    }

    public synchronized double getMarketValue(double currentPrice) {
        return quantity * currentPrice;
    }

    public synchronized double getUnrealizedPnL(double currentPrice) {
        if (quantity == 0) return 0.0;
        return getMarketValue(currentPrice) - totalCostBasis;
    }

    public synchronized double getUnrealizedPnLPercent(double currentPrice) {
        if (totalCostBasis <= 0 || quantity == 0) return 0.0;
        return (getUnrealizedPnL(currentPrice) / totalCostBasis) * 100.0;
    }

    public synchronized double getTotalPnL(double currentPrice) {
        return getUnrealizedPnL(currentPrice) + realizedPnL + totalDividendsReceived;
    }

    // Getters
    public String getSymbol() { return symbol; }
    public synchronized int getQuantity() { return quantity; }
    public synchronized double getTotalCostBasis() { return totalCostBasis; }
    public synchronized double getRealizedPnL() { return realizedPnL; }
    public synchronized double getTotalDividendsReceived() { return totalDividendsReceived; }

    @Override
    public String toString() {
        return String.format("Holding[%s: %d shares @ avg $%.2f, Cost: $%.2f, Realized: $%.2f, Div: $%.2f]",
                symbol, quantity, getAverageCostBasis(), totalCostBasis, realizedPnL, totalDividendsReceived);
    }
}
