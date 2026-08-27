package com.trading.engine;

import com.trading.domain.MarketNews;
import com.trading.domain.Stock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real-time stochastic market simulation engine.
 * Evolves prices using Geometric Brownian Motion, emits market news,
 * and distributes simulated dividends.
 */
public class MarketSimulationEngine {

    @FunctionalInterface
    public interface StockTickListener {
        void onStockTick(Stock stock);
    }

    @FunctionalInterface
    public interface NewsListener {
        void onNewsPublished(MarketNews news);
    }

    @FunctionalInterface
    public interface DividendListener {
        void onDividendPaid(String symbol, double dividendPerShare);
    }

    private final Map<String, Stock> stocks = new ConcurrentHashMap<>();
    private final List<MarketNews> recentNews = new CopyOnWriteArrayList<>();
    private final PricingStrategy pricingStrategy;
    private final NewsEventGenerator newsGenerator;
    private final OrderMatchingEngine matchingEngine;

    private final List<StockTickListener> tickListeners = new CopyOnWriteArrayList<>();
    private final List<NewsListener> newsListeners = new CopyOnWriteArrayList<>();
    private final List<DividendListener> dividendListeners = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService executorService;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger tickIntervalMs = new AtomicInteger(1000);
    private int tickCount = 0;
    private final Random random = new Random();

    public MarketSimulationEngine(OrderMatchingEngine matchingEngine) {
        this.pricingStrategy = new PricingStrategy.GeometricBrownianMotion();
        this.newsGenerator = new NewsEventGenerator();
        this.matchingEngine = matchingEngine;
        initializeDefaultMarketUniverse();
    }

    private void initializeDefaultMarketUniverse() {
        // Diverse stock universe across sectors
        registerStock(new Stock("AAPL", "Apple Inc.", "Technology", 185.50, 0.22, 0.08, 0.50, 2850.0));
        registerStock(new Stock("NVDA", "NVIDIA Corp.", "Technology", 125.20, 0.45, 0.18, 0.08, 3100.0));
        registerStock(new Stock("MSFT", "Microsoft Corp.", "Technology", 445.80, 0.20, 0.09, 0.70, 3300.0));
        registerStock(new Stock("TSLA", "Tesla Inc.", "Automotive", 248.00, 0.55, 0.12, 0.00, 780.0));
        registerStock(new Stock("AMZN", "Amazon.com Inc.", "Consumer", 192.30, 0.28, 0.11, 0.00, 2000.0));
        registerStock(new Stock("GOOGL", "Alphabet Inc.", "Technology", 178.60, 0.24, 0.10, 0.45, 2200.0));
        registerStock(new Stock("JPM", "JPMorgan Chase & Co.", "Finance", 215.40, 0.18, 0.07, 2.20, 610.0));
        registerStock(new Stock("XOM", "Exxon Mobil Corp.", "Energy", 118.90, 0.21, 0.05, 3.20, 470.0));
        registerStock(new Stock("JNJ", "Johnson & Johnson", "Healthcare", 162.10, 0.15, 0.04, 3.05, 390.0));
        registerStock(new Stock("BTC-ETF", "iShares Bitcoin Trust", "Crypto Asset", 38.40, 0.65, 0.25, 0.00, 22.0));
    }

    public void registerStock(Stock stock) {
        stocks.put(stock.getSymbol().toUpperCase(), stock);
    }

    public Stock getStock(String symbol) {
        return stocks.get(symbol.toUpperCase());
    }

    public Map<String, Stock> getAllStocks() {
        return Collections.unmodifiableMap(stocks);
    }

    public void addTickListener(StockTickListener listener) {
        tickListeners.add(listener);
    }

    public void addNewsListener(NewsListener listener) {
        newsListeners.add(listener);
    }

    public void addDividendListener(DividendListener listener) {
        dividendListeners.add(listener);
    }

    public synchronized void start() {
        if (isRunning.get()) return;
        isRunning.set(true);
        executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MarketSimulator-Worker");
            t.setDaemon(true);
            return t;
        });

        scheduleNextTick();
        System.out.println(">> Market Simulation Engine started at " + tickIntervalMs.get() + "ms tick rate.");
    }

    private void scheduleNextTick() {
        if (!isRunning.get() || executorService == null || executorService.isShutdown()) return;
        executorService.schedule(this::onSimulationTick, tickIntervalMs.get(), TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (!isRunning.get()) return;
        isRunning.set(false);
        if (executorService != null) {
            executorService.shutdownNow();
        }
        System.out.println(">> Market Simulation Engine stopped.");
    }

    public void setSpeed(int intervalMs) {
        this.tickIntervalMs.set(Math.max(50, intervalMs));
    }

    public int getSpeed() {
        return tickIntervalMs.get();
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    private void onSimulationTick() {
        try {
            tickCount++;

            // 1. Periodically generate random market news (every ~15 ticks on avg)
            if (tickCount % 15 == 0 && random.nextDouble() < 0.8) {
                publishRandomNews();
            }

            // 2. Periodically distribute simulated quarterly dividends (every ~120 ticks)
            if (tickCount % 120 == 0) {
                simulateDividendDistribution();
            }

            // 3. Evolve prices for all stocks
            double dt = 1.0 / 252.0 / 390.0; // Scaled intraday delta
            for (Stock stock : stocks.values()) {
                double shock = getActiveShockForStock(stock);
                double newPrice = pricingStrategy.calculateNextPrice(stock, dt, shock);
                long tradeVol = (long) (100 + random.nextInt(1500) * (stock.getVolatility() * 5));
                stock.updatePrice(newPrice, tradeVol);

                // Notify order matching engine of new price tick
                matchingEngine.processStockTick(stock);

                // Notify UI / Websocket listeners
                for (StockTickListener listener : tickListeners) {
                    try {
                        listener.onStockTick(stock);
                    } catch (Exception e) {
                        // ignore listener exception
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error in simulation tick: " + e.getMessage());
        } finally {
            if (isRunning.get()) {
                scheduleNextTick();
            }
        }
    }

    private double getActiveShockForStock(Stock stock) {
        // Return shock from most recent news if occurred in the last 3 ticks
        if (!recentNews.isEmpty()) {
            MarketNews latest = recentNews.get(0);
            if (latest.getAffectedSymbol().equalsIgnoreCase(stock.getSymbol()) ||
                latest.getAffectedSector().equalsIgnoreCase(stock.getSector()) ||
                (latest.getAffectedSymbol().equalsIgnoreCase("ALL") && latest.getAffectedSector().equalsIgnoreCase("ALL"))) {
                return latest.getPriceShockPct() * 0.15; // smooth shock distribution over ticks
            }
        }
        return 0.0;
    }

    public void publishNews(MarketNews news) {
        if (recentNews.size() >= 50) {
            recentNews.remove(recentNews.size() - 1);
        }
        recentNews.add(0, news);

        for (NewsListener listener : newsListeners) {
            try {
                listener.onNewsPublished(news);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public void publishRandomNews() {
        List<String> symbols = new ArrayList<>(stocks.keySet());
        MarketNews news = newsGenerator.generateRandomNews(symbols);
        publishNews(news);
    }

    private void simulateDividendDistribution() {
        for (Stock stock : stocks.values()) {
            if (stock.getDividendYield() > 0) {
                // Quarterly simulated dividend = (Price * Yield% / 4)
                double dividend = (stock.getCurrentPrice() * (stock.getDividendYield() / 100.0)) / 4.0;
                dividend = Math.round(dividend * 100.0) / 100.0;
                if (dividend > 0) {
                    for (DividendListener listener : dividendListeners) {
                        try {
                            listener.onDividendPaid(stock.getSymbol(), dividend);
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
        }
    }

    public List<MarketNews> getRecentNews() {
        return Collections.unmodifiableList(new ArrayList<>(recentNews));
    }
}
