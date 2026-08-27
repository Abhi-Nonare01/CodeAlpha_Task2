package com.trading.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an equity instrument / stock traded on the exchange.
 * Maintains real-time price state, daily statistics, and historical tick data.
 */
public class Stock implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class PricePoint implements Serializable {
        private static final long serialVersionUID = 1L;
        private final LocalDateTime timestamp;
        private final double price;
        private final long volume;

        public PricePoint(LocalDateTime timestamp, double price, long volume) {
            this.timestamp = timestamp;
            this.price = price;
            this.volume = volume;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public double getPrice() { return price; }
        public long getVolume() { return volume; }
    }

    private final String symbol;
    private final String companyName;
    private final String sector;
    private double currentPrice;
    private double openPrice;
    private double previousClose;
    private double dayHigh;
    private double dayLow;
    private long volume;
    private double volatility; // Annualized volatility (e.g., 0.25 = 25%)
    private double drift;      // Expected annual return / trend
    private double dividendYield; // Annual dividend yield %
    private double marketCap;  // In billions USD
    private final int maxHistorySize;
    private final List<PricePoint> priceHistory;

    public Stock(String symbol, String companyName, String sector, double initialPrice,
                 double volatility, double drift, double dividendYield, double marketCap) {
        this.symbol = symbol.toUpperCase();
        this.companyName = companyName;
        this.sector = sector;
        this.currentPrice = initialPrice;
        this.openPrice = initialPrice;
        this.previousClose = initialPrice;
        this.dayHigh = initialPrice;
        this.dayLow = initialPrice;
        this.volume = 0;
        this.volatility = volatility;
        this.drift = drift;
        this.dividendYield = dividendYield;
        this.marketCap = marketCap;
        this.maxHistorySize = 300;
        this.priceHistory = new ArrayList<>();
        addHistoricalPoint(initialPrice, 0);
    }

    public synchronized void updatePrice(double newPrice, long tradeVolume) {
        if (newPrice <= 0.01) {
            newPrice = 0.01;
        }
        this.currentPrice = Math.round(newPrice * 100.0) / 100.0;
        if (this.currentPrice > this.dayHigh) {
            this.dayHigh = this.currentPrice;
        }
        if (this.currentPrice < this.dayLow) {
            this.dayLow = this.currentPrice;
        }
        this.volume += tradeVolume;
        addHistoricalPoint(this.currentPrice, tradeVolume);
    }

    private synchronized void addHistoricalPoint(double price, long vol) {
        if (priceHistory.size() >= maxHistorySize) {
            priceHistory.remove(0);
        }
        priceHistory.add(new PricePoint(LocalDateTime.now(), price, vol));
    }

    public synchronized double getChange() {
        return Math.round((currentPrice - previousClose) * 100.0) / 100.0;
    }

    public synchronized double getChangePercent() {
        if (previousClose == 0) return 0.0;
        return Math.round(((currentPrice - previousClose) / previousClose * 100.0) * 100.0) / 100.0;
    }

    public synchronized double getBidPrice() {
        // Dynamic simulated spread based on volatility
        double spreadPercent = Math.max(0.0005, volatility * 0.005);
        return Math.round((currentPrice * (1.0 - spreadPercent / 2.0)) * 100.0) / 100.0;
    }

    public synchronized double getAskPrice() {
        double spreadPercent = Math.max(0.0005, volatility * 0.005);
        return Math.round((currentPrice * (1.0 + spreadPercent / 2.0)) * 100.0) / 100.0;
    }

    // Getters and Setters
    public String getSymbol() { return symbol; }
    public String getCompanyName() { return companyName; }
    public String getSector() { return sector; }
    public synchronized double getCurrentPrice() { return currentPrice; }
    public synchronized double getOpenPrice() { return openPrice; }
    public synchronized double getPreviousClose() { return previousClose; }
    public synchronized void setPreviousClose(double previousClose) { this.previousClose = previousClose; }
    public synchronized double getDayHigh() { return dayHigh; }
    public synchronized double getDayLow() { return dayLow; }
    public synchronized long getVolume() { return volume; }
    public synchronized double getVolatility() { return volatility; }
    public synchronized void setVolatility(double volatility) { this.volatility = volatility; }
    public synchronized double getDrift() { return drift; }
    public synchronized void setDrift(double drift) { this.drift = drift; }
    public double getDividendYield() { return dividendYield; }
    public double getMarketCap() { return marketCap; }
    public synchronized List<PricePoint> getPriceHistory() {
        return Collections.unmodifiableList(new ArrayList<>(priceHistory));
    }

    @Override
    public String toString() {
        return String.format("%s (%s): $%.2f [%+.2f (%.2f%%)] Vol: %,d",
                symbol, companyName, currentPrice, getChange(), getChangePercent(), volume);
    }
}
