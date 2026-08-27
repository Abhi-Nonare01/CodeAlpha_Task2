package com.trading;

import com.trading.engine.MarketSimulationEngine;
import com.trading.engine.OrderMatchingEngine;
import com.trading.repository.*;
import com.trading.service.*;
import com.trading.ui.cli.AnsiConsole;
import com.trading.ui.cli.TerminalDashboard;
import com.trading.ui.web.TradingWebServer;

/**
 * Main entry point for the Enterprise Stock Trading Platform.
 * Bootstraps the database, simulation engine, matching engine,
 * services, web dashboard server, and interactive ANSI terminal UI.
 */
public class App {
    private static final int DEFAULT_WEB_PORT = 8080;

    public static void main(String[] args) {
        System.out.println(AnsiConsole.banner());
        System.out.println(">> Initializing SQLite Persistence Layer...");

        DatabaseManager databaseManager = new DatabaseManager("trading_platform.db");
        UserRepository userRepository = new UserRepository(databaseManager);
        PortfolioRepository portfolioRepository = new PortfolioRepository(databaseManager);
        TransactionRepository transactionRepository = new TransactionRepository(databaseManager);
        OrderRepository orderRepository = new OrderRepository(databaseManager);

        System.out.println(">> Initializing Market Simulation & Order Matching Engines...");
        OrderMatchingEngine matchingEngine = new OrderMatchingEngine();
        MarketSimulationEngine simulationEngine = new MarketSimulationEngine(matchingEngine);

        System.out.println(">> Initializing Business & Analytics Services...");
        AuthService authService = new AuthService(userRepository, portfolioRepository);
        MarketDataService marketDataService = new MarketDataService(simulationEngine);
        PortfolioService portfolioService = new PortfolioService(portfolioRepository, transactionRepository, simulationEngine);
        TradingService tradingService = new TradingService(matchingEngine, simulationEngine, portfolioRepository, transactionRepository, orderRepository);
        ExportReportService exportReportService = new ExportReportService();

        // Start background simulation engine
        simulationEngine.start();

        // Start embedded web trading dashboard
        int webPort = DEFAULT_WEB_PORT;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            try {
                webPort = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        } else if (args.length > 0) {
            try {
                webPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        TradingWebServer webServer = new TradingWebServer(
                webPort,
                authService,
                marketDataService,
                portfolioService,
                tradingService,
                exportReportService,
                simulationEngine
        );
        webServer.start();

        System.out.println("========================================================================================");
        System.out.println(AnsiConsole.green("✔ QuantumTrade Platform is RUNNING!"));
        System.out.println("  • Modern Web Trading UI: " + AnsiConsole.cyan("http://localhost:" + webPort));
        System.out.println("  • REST API Docs:         " + AnsiConsole.cyan("http://localhost:" + webPort + "/api/market"));
        System.out.println("  • Embedded Database:     " + AnsiConsole.yellow("trading_platform.db (SQLite)"));
        System.out.println("========================================================================================\n");

        // Automatically open browser for seamless 1-click user experience
        try {
            String url = "http://localhost:" + webPort;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            }
        } catch (Exception ignored) {}

        // Start Interactive Terminal Dashboard
        TerminalDashboard terminalDashboard = new TerminalDashboard(
                authService,
                marketDataService,
                portfolioService,
                tradingService,
                exportReportService,
                simulationEngine
        );

        // Shutdown hook to cleanly stop web server and engine
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            simulationEngine.stop();
            webServer.stop();
        }));

        terminalDashboard.start();
    }
}
