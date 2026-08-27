package com.trading.engine;

import com.trading.domain.MarketNews;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates realistic macroeconomic, sector-wide, and company-specific market news events
 * that dynamically shock asset prices and adjust market volatility.
 */
public class NewsEventGenerator {
    private final Random random = new Random();

    private static final List<NewsTemplate> TEMPLATES = new ArrayList<>();

    private static class NewsTemplate {
        final String headlineFormat;
        final String contentFormat;
        final String sector;
        final String symbol;
        final MarketNews.Sentiment sentiment;
        final double minShock;
        final double maxShock;

        NewsTemplate(String headlineFormat, String contentFormat, String sector, String symbol,
                     MarketNews.Sentiment sentiment, double minShock, double maxShock) {
            this.headlineFormat = headlineFormat;
            this.contentFormat = contentFormat;
            this.sector = sector;
            this.symbol = symbol;
            this.sentiment = sentiment;
            this.minShock = minShock;
            this.maxShock = maxShock;
        }
    }

    static {
        // Macroeconomic Events
        TEMPLATES.add(new NewsTemplate(
                "Federal Reserve holds interest rates steady; signals potential rate cuts",
                "The Federal Open Market Committee announced it is maintaining the benchmark interest rate, citing cooling inflation.",
                "ALL", "ALL", MarketNews.Sentiment.BULLISH, 1.0, 3.5));

        TEMPLATES.add(new NewsTemplate(
                "Inflation data prints hotter than expected; bond yields spike",
                "Consumer Price Index rose 0.4% month-over-month, raising concerns about prolonged high borrowing costs.",
                "ALL", "ALL", MarketNews.Sentiment.BEARISH, -3.5, -1.2));

        TEMPLATES.add(new NewsTemplate(
                "Global GDP growth forecast revised upward by IMF",
                "Resilient consumer spending and trade volumes prompt the IMF to boost economic expansion projections.",
                "ALL", "ALL", MarketNews.Sentiment.BULLISH, 0.8, 2.2));

        // Tech Sector Events
        TEMPLATES.add(new NewsTemplate(
                "Breakthrough next-generation AI chip architecture announced by {SYMBOL}",
                "{SYMBOL} revealed a flagship AI accelerator demonstrating 3x power efficiency and record compute benchmarks.",
                "Technology", "{SYMBOL}", MarketNews.Sentiment.BULLISH, 3.0, 7.5));

        TEMPLATES.add(new NewsTemplate(
                "Cloud infrastructure spending hits all-time high amid enterprise AI surge",
                "Hyperscalers report accelerating cloud contract bookings, boosting software and hardware ecosystems.",
                "Technology", "ALL", MarketNews.Sentiment.BULLISH, 1.5, 4.0));

        TEMPLATES.add(new NewsTemplate(
                "Antitrust regulatory probe opened into Big Tech cloud software licensing",
                "Regulators announced an inquiry into restrictive ecosystem licensing practices across tech majors.",
                "Technology", "{SYMBOL}", MarketNews.Sentiment.BEARISH, -5.0, -1.8));

        // Energy & EV Sector Events
        TEMPLATES.add(new NewsTemplate(
                "OPEC+ agrees to unexpected oil output cuts; energy sector rallies",
                "Crude oil futures jumped following unanimous agreement to constrain global production quotas.",
                "Energy", "XOM", MarketNews.Sentiment.BULLISH, 2.5, 6.0));

        TEMPLATES.add(new NewsTemplate(
                "Next-gen solid state battery achieves 1,000-mile range in laboratory trials",
                "Breakthrough in cathode chemistry promises faster charging and dramatically lower production costs for EV leaders.",
                "Automotive", "TSLA", MarketNews.Sentiment.BULLISH, 3.5, 8.0));

        // Healthcare / Bio Events
        TEMPLATES.add(new NewsTemplate(
                "FDA grants fast-track priority review for blockbuster cancer therapy",
                "Phase 3 clinical trial data demonstrated statistically significant improvement in overall survival rates.",
                "Healthcare", "JNJ", MarketNews.Sentiment.BULLISH, 2.0, 5.5));

        // Financials
        TEMPLATES.add(new NewsTemplate(
                "Major investment bank reports record net interest income and trading profits",
                "Earnings beat Wall Street consensus by 18% driven by robust advisory fees and fixed income volume.",
                "Finance", "JPM", MarketNews.Sentiment.BULLISH, 1.8, 4.2));

        TEMPLATES.add(new NewsTemplate(
                "Credit rating agency downgrades regional banking credit outlook",
                "Commercial real estate exposure concerns lead to elevated provisions for loan losses.",
                "Finance", "JPM", MarketNews.Sentiment.BEARISH, -4.0, -1.5));
    }

    public MarketNews generateRandomNews(List<String> availableSymbols) {
        NewsTemplate t = TEMPLATES.get(random.nextInt(TEMPLATES.size()));
        String symbol = t.symbol;
        if (symbol.equals("{SYMBOL}")) {
            if (!availableSymbols.isEmpty()) {
                symbol = availableSymbols.get(random.nextInt(availableSymbols.size()));
            } else {
                symbol = "AAPL";
            }
        }

        String headline = t.headlineFormat.replace("{SYMBOL}", symbol);
        String content = t.contentFormat.replace("{SYMBOL}", symbol);

        double shockRange = t.maxShock - t.minShock;
        double shock = t.minShock + (random.nextDouble() * shockRange);
        shock = Math.round(shock * 10.0) / 10.0;

        return new MarketNews(null, headline, content, t.sector, symbol, t.sentiment, shock);
    }
}
