package com.trading.ui.cli;

import com.trading.domain.*;
import com.trading.engine.MarketSimulationEngine;
import com.trading.service.*;
import java.io.File;
import java.util.*;

/**
 * Interactive ANSI Terminal Console Interface.
 */
public class TerminalDashboard {
    private final AuthService authService;
    private final MarketDataService marketDataService;
    private final PortfolioService portfolioService;
    private final TradingService tradingService;
    private final ExportReportService exportReportService;
    private final MarketSimulationEngine simulationEngine;
    private final Scanner scanner;

    public TerminalDashboard(AuthService authService,
                             MarketDataService marketDataService,
                             PortfolioService portfolioService,
                             TradingService tradingService,
                             ExportReportService exportReportService,
                             MarketSimulationEngine simulationEngine) {
        this.authService = authService;
        this.marketDataService = marketDataService;
        this.portfolioService = portfolioService;
        this.tradingService = tradingService;
        this.exportReportService = exportReportService;
        this.simulationEngine = simulationEngine;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        if (System.console() == null && !scanner.hasNextLine()) {
            System.out.println(">> Running in background server mode. Web Dashboard is active on port.");
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        while (true) {
            try {
                User user = authService.getCurrentUser();
                Portfolio portfolio = portfolioService.getPortfolioForUser(user.getId());

                printMainMenu(user, portfolio);
                System.out.print(AnsiConsole.BRIGHT_CYAN + "Select Option [0-10]: " + AnsiConsole.RESET);
                if (!scanner.hasNextLine()) {
                    try {
                        Thread.currentThread().join();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return;
                }
                String input = scanner.nextLine().trim();

            switch (input) {
                case "1" -> showMarketWatchlist();
                case "2" -> showStockDetailAndChart();
                case "3" -> handlePlaceOrder(portfolio);
                case "4" -> showPortfolioHoldings(portfolio);
                case "5" -> showPerformanceAnalytics(portfolio);
                case "6" -> showPendingOrdersAndCancel(user);
                case "7" -> showMarketNewsAndShock();
                case "8" -> handleCashManagement(portfolio);
                case "9" -> handleExportReports(portfolio);
                case "10" -> handleSimulationSpeed();
                case "0" -> {
                    System.out.println(AnsiConsole.yellow("Shutting down Trading Platform. Goodbye!"));
                    simulationEngine.stop();
                    return;
                }
                default -> System.out.println(AnsiConsole.red("Invalid option! Please enter 0-10."));
            }

            System.out.println("\nPress Enter to continue...");
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
            } catch (Exception e) {
                try {
                    Thread.currentThread().join();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
        }
    }

    private void printMainMenu(User user, Portfolio portfolio) {
        Map<String, Stock> marketStocks = simulationEngine.getAllStocks();
        double equity = portfolio.getTotalEquity(marketStocks);
        double returnPct = portfolio.getTotalReturnPercent(marketStocks);

        System.out.println(AnsiConsole.banner());
        System.out.printf("  Trader: %s | Cash: $%s | Total Equity: $%s | Return: %s\n",
                AnsiConsole.bold(user.getFullName()),
                AnsiConsole.yellow(String.format("%,.2f", portfolio.getCashBalance())),
                AnsiConsole.bold(String.format("%,.2f", equity)),
                AnsiConsole.colorPriceChange(returnPct, String.format("%+.2f%%", returnPct)));
        System.out.println("----------------------------------------------------------------------------------------");
        System.out.println("  1.  [Market]    Live Market Quotes & Ticker Watchlist");
        System.out.println("  2.  [Chart]     Stock Deep-Dive & ASCII Candlestick/Line Chart");
        System.out.println("  3.  [Trade]     Place Order (Market, Limit, Stop-Loss, Trailing-Stop)");
        System.out.println("  4.  [Portfolio] Holdings, Asset Allocation & Real-time P&L");
        System.out.println("  5.  [Analytics] Institutional Risk/Return Metrics (Sharpe, Drawdown, Win Rate)");
        System.out.println("  6.  [Orders]    Pending Order Book & Order Cancellation");
        System.out.println("  7.  [News]      Live Macro/Corporate News Feed & Inject Shock");
        System.out.println("  8.  [Funds]     Cash Management (Deposit / Withdraw)");
        System.out.println("  9.  [Export]    Export Portfolio Statement to CSV & JSON");
        System.out.println("  10. [Engine]    Adjust Simulation Speed / Pause");
        System.out.println("  0.  [Exit]      Exit Platform");
        System.out.println("----------------------------------------------------------------------------------------");
    }

    private void showMarketWatchlist() {
        System.out.println("\n" + AnsiConsole.BRIGHT_CYAN + AnsiConsole.BOLD + "=== REAL-TIME MARKET WATCHLIST ===" + AnsiConsole.RESET);
        System.out.printf("%-8s %-20s %-12s %-10s %-12s %-12s %-12s %-14s %-10s\n",
                "SYMBOL", "NAME", "SECTOR", "PRICE", "CHANGE", "BID", "ASK", "VOLUME", "SPARKLINE");
        System.out.println("─".repeat(110));

        for (Stock s : marketDataService.getAllStocks()) {
            List<Double> hist = s.getPriceHistory().stream().map(Stock.PricePoint::getPrice).toList();
            String spark = AsciiChart.renderSparkline(hist);
            String changeStr = String.format("%+.2f (%+.2f%%)", s.getChange(), s.getChangePercent());

            System.out.printf("%-8s %-20s %-12s $%-9.2f %-12s $%-11.2f $%-11.2f %-14s %s\n",
                    AnsiConsole.bold(s.getSymbol()),
                    truncate(s.getCompanyName(), 20),
                    s.getSector(),
                    s.getCurrentPrice(),
                    AnsiConsole.colorPriceChange(s.getChange(), changeStr),
                    s.getBidPrice(),
                    s.getAskPrice(),
                    String.format("%,d", s.getVolume()),
                    AnsiConsole.cyan(spark));
        }

        // Sector summary
        System.out.println("\n" + AnsiConsole.bold("Sector Performance:"));
        marketDataService.getSectorPerformance().forEach((sector, avgChg) -> {
            System.out.printf("  • %-15s : %s\n", sector, AnsiConsole.colorPriceChange(avgChg, String.format("%+.2f%%", avgChg)));
        });
    }

    private void showStockDetailAndChart() {
        System.out.print("\nEnter Stock Symbol (e.g. AAPL, NVDA, TSLA, BTC-ETF): ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        Stock stock = marketDataService.getStock(symbol);

        if (stock == null) {
            System.out.println(AnsiConsole.red("Error: Stock '" + symbol + "' not found!"));
            return;
        }

        System.out.println("\n" + AnsiConsole.BRIGHT_CYAN + AnsiConsole.BOLD + "=== STOCK ANALYSIS: " + stock.getSymbol() + " (" + stock.getCompanyName() + ") ===" + AnsiConsole.RESET);
        System.out.printf("Sector: %-15s | Market Cap: $%.1fB | Div Yield: %.2f%%\n",
                stock.getSector(), stock.getMarketCap(), stock.getDividendYield());
        System.out.printf("Current Price: $%.2f | Open: $%.2f | High: $%.2f | Low: $%.2f\n",
                stock.getCurrentPrice(), stock.getOpenPrice(), stock.getDayHigh(), stock.getDayLow());
        System.out.printf("Bid: $%.2f | Ask: $%.2f | Volume: %,d shares | Volatility: %.1f%%\n\n",
                stock.getBidPrice(), stock.getAskPrice(), stock.getVolume(), stock.getVolatility() * 100.0);

        List<Double> prices = stock.getPriceHistory().stream().map(Stock.PricePoint::getPrice).toList();
        System.out.println(AsciiChart.renderLineChart(prices, 12, 50, stock.getSymbol() + " Price History"));
    }

    private void handlePlaceOrder(Portfolio portfolio) {
        System.out.println("\n" + AnsiConsole.BRIGHT_CYAN + AnsiConsole.BOLD + "=== PLACE TRADE ORDER ===" + AnsiConsole.RESET);
        System.out.print("Enter Symbol (e.g. AAPL, NVDA, TSLA): ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        Stock stock = marketDataService.getStock(symbol);
        if (stock == null) {
            System.out.println(AnsiConsole.red("Invalid symbol!"));
            return;
        }

        System.out.printf("Current %s Market Price: $%.2f (Bid: $%.2f | Ask: $%.2f)\n",
                symbol, stock.getCurrentPrice(), stock.getBidPrice(), stock.getAskPrice());

        System.out.print("Side [1. BUY, 2. SELL]: ");
        String sideStr = scanner.nextLine().trim();
        OrderSide side = sideStr.equals("2") ? OrderSide.SELL : OrderSide.BUY;

        if (side == OrderSide.SELL) {
            Holding h = portfolio.getHolding(symbol);
            int owned = (h != null) ? h.getQuantity() : 0;
            System.out.printf("You currently own %d shares of %s.\n", owned, symbol);
            if (owned <= 0) {
                System.out.println(AnsiConsole.red("You do not own any shares of " + symbol + " to sell!"));
                return;
            }
        }

        System.out.println("Order Types:");
        System.out.println("  1. MARKET        (Executes immediately at best available market price)");
        System.out.println("  2. LIMIT         (Executes when price reaches limit or better)");
        System.out.println("  3. STOP-LOSS     (Triggers market order when price drops/rises to stop)");
        System.out.println("  4. TRAILING-STOP (Dynamic stop tracking highest price by %)");
        System.out.print("Select Order Type [1-4]: ");
        String typeChoice = scanner.nextLine().trim();

        System.out.print("Quantity (Shares): ");
        int qty;
        try {
            qty = Integer.parseInt(scanner.nextLine().trim());
            if (qty <= 0) throw new Exception();
        } catch (Exception e) {
            System.out.println(AnsiConsole.red("Invalid quantity!"));
            return;
        }

        Double limitPrice = null;
        Double stopPrice = null;
        Double trailingPct = null;

        try {
            switch (typeChoice) {
                case "2" -> {
                    System.out.print("Enter Limit Price: $");
                    limitPrice = Double.parseDouble(scanner.nextLine().trim());
                    tradingService.placeLimitOrder(portfolio, symbol, side, qty, limitPrice);
                }
                case "3" -> {
                    System.out.print("Enter Stop Price: $");
                    stopPrice = Double.parseDouble(scanner.nextLine().trim());
                    tradingService.placeStopLossOrder(portfolio, symbol, side, qty, stopPrice);
                }
                case "4" -> {
                    System.out.print("Enter Trailing Percent (e.g. 5 for 5%): ");
                    trailingPct = Double.parseDouble(scanner.nextLine().trim());
                    tradingService.placeTrailingStopOrder(portfolio, symbol, side, qty, trailingPct);
                }
                default -> tradingService.placeMarketOrder(portfolio, symbol, side, qty);
            }
            System.out.println(AnsiConsole.green("✔ Order placed successfully!"));
        } catch (Exception e) {
            System.out.println(AnsiConsole.red("Order Placement Failed: " + e.getMessage()));
        }
    }

    private void showPortfolioHoldings(Portfolio portfolio) {
        Map<String, Stock> marketStocks = simulationEngine.getAllStocks();
        System.out.println("\n" + AnsiConsole.BRIGHT_CYAN + AnsiConsole.BOLD + "=== PORTFOLIO HOLDINGS & REAL-TIME P&L ===" + AnsiConsole.RESET);
        System.out.printf("Portfolio: %s | Cash: $%,.2f | Total Holdings: $%,.2f | Total Equity: $%,.2f\n",
                portfolio.getName(),
                portfolio.getCashBalance(),
                portfolio.getTotalHoldingsValue(marketStocks),
                portfolio.getTotalEquity(marketStocks));
        System.out.println("─".repeat(105));

        System.out.printf("%-8s %-8s %-12s %-12s %-14s %-18s %-14s %-12s\n",
                "SYMBOL", "SHARES", "AVG COST", "CURR PRICE", "MARKET VALUE", "UNREALIZED P&L", "REALIZED P&L", "DIVIDENDS");
        System.out.println("─".repeat(105));

        boolean hasHoldings = false;
        for (Holding h : portfolio.getHoldings().values()) {
            if (h.getQuantity() > 0 || h.getRealizedPnL() != 0) {
                hasHoldings = true;
                Stock s = marketStocks.get(h.getSymbol());
                double price = (s != null) ? s.getCurrentPrice() : 0.0;
                double mktVal = h.getMarketValue(price);
                double unPnL = h.getUnrealizedPnL(price);
                double unPnLPct = h.getUnrealizedPnLPercent(price);
                String unPnLStr = String.format("%+,.2f (%+.2f%%)", unPnL, unPnLPct);

                System.out.printf("%-8s %-8d $%-11.2f $%-11.2f $%-13.2f %-18s %-14s $%-11.2f\n",
                        AnsiConsole.bold(h.getSymbol()),
                        h.getQuantity(),
                        h.getAverageCostBasis(),
                        price,
                        mktVal,
                        AnsiConsole.colorPriceChange(unPnL, unPnLStr),
                        AnsiConsole.colorPriceChange(h.getRealizedPnL(), String.format("%+,.2f", h.getRealizedPnL())),
                        h.getTotalDividendsReceived());
            }
        }

        if (!hasHoldings) {
            System.out.println("  (No active holdings in portfolio. Place a BUY order to start trading.)");
        }

        System.out.println("─".repeat(105));
        double totUnrealized = portfolio.getTotalUnrealizedPnL(marketStocks);
        double totRealized = portfolio.getTotalRealizedPnL();
        double totDivs = portfolio.getTotalDividends();
        System.out.printf("TOTALS: Unrealized P&L: %s | Realized P&L: %s | Total Dividends: $%,.2f\n",
                AnsiConsole.colorPriceChange(totUnrealized, String.format("%+,.2f", totUnrealized)),
                AnsiConsole.colorPriceChange(totRealized, String.format("%+,.2f", totRealized)),
                totDivs);
    }

    private void showPerformanceAnalytics(Portfolio portfolio) {
        PerformanceMetrics metrics = portfolioService.calculatePerformanceMetrics(portfolio);

        System.out.println("\n" + AnsiConsole.BRIGHT_CYAN + AnsiConsole.BOLD + "=== INSTITUTIONAL PERFORMANCE & RISK METRICS ===" + AnsiConsole.RESET);
        System.out.printf("  • Total Return:           %s ($%,.2f)\n",
                AnsiConsole.colorPriceChange(metrics.getTotalReturnPercent(), String.format("%+.2f%%", metrics.getTotalReturnPercent())),
                metrics.getTotalReturnAmount());
        System.out.printf("  • Sharpe Ratio:           %s\n", AnsiConsole.bold(String.format("%.2f", metrics.getSharpeRatio())));
        System.out.printf("  • Maximum Drawdown:       %s\n", AnsiConsole.red(String.format("-%.2f%%", metrics.getMaxDrawdownPercent())));
        System.out.printf("  • Win Rate:               %s (%d wins / %d losses out of %d closed trades)\n",
                AnsiConsole.green(String.format("%.1f%%", metrics.getWinRate())),
                metrics.getWinningTrades(), metrics.getLosingTrades(), metrics.getTotalTrades());
        System.out.printf("  • Profit Factor:          %s\n", AnsiConsole.bold(String.format("%.2f", metrics.getProfitFactor())));
        System.out.printf("  • Realized P&L:           %s\n", AnsiConsole.colorPriceChange(metrics.getRealizedPnL(), String.format("$%,.2f", metrics.getRealizedPnL())));
        System.out.printf("  • Unrealized P&L:         %s\n", AnsiConsole.colorPriceChange(metrics.getUnrealizedPnL(), String.format("$%,.2f", metrics.getUnrealizedPnL())));
        System.out.printf("  • Cumulative Dividends:   $%s\n", String.format("%,.2f", metrics.getTotalDividends()));

        List<Portfolio.EquityPoint> eqHist = portfolio.getEquityHistory();
        if (eqHist.size() > 1) {
            List<Double> eqValues = eqHist.stream().map(Portfolio.EquityPoint::getEquity).toList();
            System.out.println("\n" + AsciiChart.renderLineChart(eqValues, 10, 50, "Portfolio Equity Growth Curve"));
        }
    }

    private void showPendingOrdersAndCancel(User user) {
        List<Order> pending = tradingService.getPendingOrders(user.getId());
        System.out.println("\n" + AnsiConsole.BRIGHT_CYAN + AnsiConsole.BOLD + "=== PENDING ORDER BOOK ===" + AnsiConsole.RESET);

        if (pending.isEmpty()) {
            System.out.println("  No active pending orders.");
        } else {
            System.out.printf("%-10s %-8s %-6s %-12s %-8s %-12s %-14s\n",
                    "ORDER ID", "SYMBOL", "SIDE", "TYPE", "QTY", "TARGET PRICE", "STATUS");
            System.out.println("─".repeat(80));
            for (Order o : pending) {
                String targetPrice = (o.getType() == OrderType.LIMIT) ? "$" + o.getLimitPrice() :
                        (o.getType() == OrderType.STOP_LOSS ? "$" + o.getStopPrice() : o.getTrailingPercent() + "% Trail");
                System.out.printf("%-10s %-8s %-6s %-12s %-8d %-12s %-14s\n",
                        o.getId(), o.getSymbol(), o.getSide(), o.getType(), o.getQuantity(), targetPrice, AnsiConsole.yellow(o.getStatus().name()));
            }

            System.out.print("\nEnter Order ID to cancel (or press Enter to skip): ");
            String cancelId = scanner.nextLine().trim();
            if (!cancelId.isBlank()) {
                boolean ok = tradingService.cancelOrder(cancelId, "Cancelled via CLI");
                if (ok) {
                    System.out.println(AnsiConsole.green("✔ Order " + cancelId + " cancelled."));
                } else {
                    System.out.println(AnsiConsole.red("Order not found or already filled."));
                }
            }
        }
    }

    private void showMarketNewsAndShock() {
        System.out.println("\n" + AnsiConsole.BRIGHT_CYAN + AnsiConsole.BOLD + "=== REAL-TIME MARKET NEWS FEED ===" + AnsiConsole.RESET);
        List<MarketNews> newsList = marketDataService.getLatestNews();
        if (newsList.isEmpty()) {
            System.out.println("  No market news events yet.");
        } else {
            for (MarketNews n : newsList) {
                String shockStr = String.format("%+.1f%%", n.getPriceShockPct());
                System.out.printf("  [%s] %s (%s | Sector: %s | Impact: %s)\n",
                        n.getSentiment() == MarketNews.Sentiment.BULLISH ? AnsiConsole.green("BULLISH") :
                                (n.getSentiment() == MarketNews.Sentiment.BEARISH ? AnsiConsole.red("BEARISH") : AnsiConsole.yellow("NEUTRAL")),
                        AnsiConsole.bold(n.getHeadline()),
                        n.getAffectedSymbol(),
                        n.getAffectedSector(),
                        AnsiConsole.colorPriceChange(n.getPriceShockPct(), shockStr));
                System.out.printf("      \"%s\"\n\n", n.getContent());
            }
        }

        System.out.print("Trigger dynamic macroeconomic shock news now? [y/N]: ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            marketDataService.triggerRandomNews();
            System.out.println(AnsiConsole.green("✔ News event injected into live market!"));
        }
    }

    private void handleCashManagement(Portfolio portfolio) {
        System.out.println("\n" + AnsiConsole.BRIGHT_CYAN + AnsiConsole.BOLD + "=== CASH MANAGEMENT ===" + AnsiConsole.RESET);
        System.out.printf("Current Cash Balance: $%,.2f\n", portfolio.getCashBalance());
        System.out.println("  1. Deposit Funds");
        System.out.println("  2. Withdraw Funds");
        System.out.print("Select [1-2]: ");
        String choice = scanner.nextLine().trim();

        System.out.print("Enter Amount: $");
        try {
            double amt = Double.parseDouble(scanner.nextLine().trim());
            if (choice.equals("1")) {
                portfolioService.depositFunds(portfolio, amt, "User Cash Deposit");
                System.out.println(AnsiConsole.green("✔ Successfully deposited $" + String.format("%.2f", amt)));
            } else if (choice.equals("2")) {
                portfolioService.withdrawFunds(portfolio, amt, "User Cash Withdrawal");
                System.out.println(AnsiConsole.green("✔ Successfully withdrew $" + String.format("%.2f", amt)));
            }
        } catch (Exception e) {
            System.out.println(AnsiConsole.red("Operation failed: " + e.getMessage()));
        }
    }

    private void handleExportReports(Portfolio portfolio) {
        System.out.println("\n" + AnsiConsole.BRIGHT_CYAN + AnsiConsole.BOLD + "=== EXPORT PORTFOLIO REPORTS ===" + AnsiConsole.RESET);
        Map<String, Stock> marketStocks = simulationEngine.getAllStocks();
        PerformanceMetrics metrics = portfolioService.calculatePerformanceMetrics(portfolio);
        List<Transaction> txs = portfolioService.getTransactionHistory(portfolio.getId());

        try {
            File csv = exportReportService.exportTransactionsToCSV(txs, "transactions_statement.csv");
            File json = exportReportService.exportPortfolioStatementToJSON(portfolio, marketStocks, metrics, txs, "portfolio_report.json");

            System.out.println(AnsiConsole.green("✔ Transactions exported to CSV: " + csv.getAbsolutePath()));
            System.out.println(AnsiConsole.green("✔ Portfolio Statement exported to JSON: " + json.getAbsolutePath()));
        } catch (Exception e) {
            System.out.println(AnsiConsole.red("Export failed: " + e.getMessage()));
        }
    }

    private void handleSimulationSpeed() {
        System.out.println("\n" + AnsiConsole.BRIGHT_CYAN + AnsiConsole.BOLD + "=== SIMULATION ENGINE SPEED ===" + AnsiConsole.RESET);
        System.out.printf("Current Tick Rate: %d ms (%s)\n",
                simulationEngine.getSpeed(),
                simulationEngine.isRunning() ? "RUNNING" : "PAUSED");
        System.out.println("  1. Normal Speed (1,000 ms / tick)");
        System.out.println("  2. Fast (500 ms / tick)");
        System.out.println("  3. Turbo (200 ms / tick)");
        System.out.println("  4. High-Frequency (100 ms / tick)");
        System.out.println("  5. Toggle Pause / Resume");
        System.out.print("Select [1-5]: ");
        String c = scanner.nextLine().trim();

        switch (c) {
            case "1" -> simulationEngine.setSpeed(1000);
            case "2" -> simulationEngine.setSpeed(500);
            case "3" -> simulationEngine.setSpeed(200);
            case "4" -> simulationEngine.setSpeed(100);
            case "5" -> {
                if (simulationEngine.isRunning()) {
                    simulationEngine.stop();
                } else {
                    simulationEngine.start();
                }
            }
        }
        System.out.println(AnsiConsole.green("✔ Engine updated!"));
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max - 3) + "..." : text;
    }
}
