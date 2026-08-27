package com.trading.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents dynamic macro-economic or company-specific news events that shock prices and drive market sentiment.
 */
public class MarketNews implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Sentiment {
        BULLISH,
        BEARISH,
        NEUTRAL
    }

    private final String id;
    private final String headline;
    private final String content;
    private final String affectedSector; // e.g. "Technology", "All", "Energy"
    private final String affectedSymbol; // e.g. "AAPL" or "ALL"
    private final Sentiment sentiment;
    private final double priceShockPct;  // e.g. +3.5 for +3.5%, -5.0 for -5.0%
    private final LocalDateTime timestamp;

    public MarketNews(String id, String headline, String content, String affectedSector,
                      String affectedSymbol, Sentiment sentiment, double priceShockPct) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString().substring(0, 8).toUpperCase() : id;
        this.headline = headline;
        this.content = content;
        this.affectedSector = affectedSector;
        this.affectedSymbol = affectedSymbol;
        this.sentiment = sentiment;
        this.priceShockPct = priceShockPct;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getHeadline() { return headline; }
    public String getContent() { return content; }
    public String getAffectedSector() { return affectedSector; }
    public String getAffectedSymbol() { return affectedSymbol; }
    public Sentiment getSentiment() { return sentiment; }
    public double getPriceShockPct() { return priceShockPct; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] NEWS: %s (%s, Shock: %+.1f%%)",
                sentiment, headline, affectedSymbol.equals("ALL") ? affectedSector : affectedSymbol, priceShockPct);
    }
}
