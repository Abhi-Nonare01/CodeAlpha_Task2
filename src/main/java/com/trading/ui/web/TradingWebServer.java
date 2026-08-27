package com.trading.ui.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.trading.domain.*;
import com.trading.engine.MarketSimulationEngine;
import com.trading.service.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Embedded lightweight HTTP server serving a modern real-time Web Trading Dashboard
 * and JSON REST APIs.
 */
public class TradingWebServer {
    private final int port;
    private final AuthService authService;
    private final MarketDataService marketDataService;
    private final PortfolioService portfolioService;
    private final TradingService tradingService;
    private final ExportReportService exportReportService;
    private final MarketSimulationEngine simulationEngine;
    private final ObjectMapper objectMapper;
    private HttpServer server;

    public TradingWebServer(int port,
                            AuthService authService,
                            MarketDataService marketDataService,
                            PortfolioService portfolioService,
                            TradingService tradingService,
                            ExportReportService exportReportService,
                            MarketSimulationEngine simulationEngine) {
        this.port = port;
        this.authService = authService;
        this.marketDataService = marketDataService;
        this.portfolioService = portfolioService;
        this.tradingService = tradingService;
        this.exportReportService = exportReportService;
        this.simulationEngine = simulationEngine;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public synchronized void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newCachedThreadPool());

            // Static Dashboard handler
            server.createContext("/", new StaticDashboardHandler());

            // API Endpoints
            server.createContext("/api/auth/login", this::handleAuthLogin);
            server.createContext("/api/auth/register", this::handleAuthRegister);
            server.createContext("/api/auth/me", this::handleAuthMe);
            server.createContext("/api/auth/profile", this::handleAuthProfile);
            server.createContext("/api/auth/logout", this::handleAuthLogout);
            server.createContext("/api/market", this::handleMarket);
            server.createContext("/api/market/stock", this::handleStockDetail);
            server.createContext("/api/portfolio", this::handlePortfolio);
            server.createContext("/api/metrics", this::handleMetrics);
            server.createContext("/api/news", this::handleNews);
            server.createContext("/api/orders", this::handleOrders);
            server.createContext("/api/transactions", this::handleTransactions);
            server.createContext("/api/orders/place", this::handlePlaceOrder);
            server.createContext("/api/orders/cancel", this::handleCancelOrder);
            server.createContext("/api/news/trigger", this::handleTriggerNews);
            server.createContext("/api/funds", this::handleFunds);
            server.createContext("/api/engine/speed", this::handleEngineSpeed);

            server.start();
            System.out.println(">> Web Trading Dashboard live at: http://localhost:" + port);
        } catch (IOException e) {
            System.err.println("Failed to start embedded web server: " + e.getMessage());
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println(">> Web Trading Dashboard stopped.");
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleAuthLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendCorsOptions(exchange);
            return;
        }
        try {
            Map<String, String> body = objectMapper.readValue(exchange.getRequestBody(), Map.class);
            String username = body.get("username");
            String password = body.get("password");

            Optional<User> opt = authService.login(username, password);
            if (opt.isPresent()) {
                User u = opt.get();
                sendJsonResponse(exchange, 200, Map.of(
                        "success", true,
                        "user", Map.of(
                                "id", u.getId(),
                                "username", u.getUsername(),
                                "fullName", u.getFullName(),
                                "email", u.getEmail() != null ? u.getEmail() : ""
                        )
                ));
            } else {
                sendJsonResponse(exchange, 401, Map.of("success", false, "error", "Invalid username or password"));
            }
        } catch (Exception e) {
            sendJsonResponse(exchange, 400, Map.of("success", false, "error", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleAuthRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendCorsOptions(exchange);
            return;
        }
        try {
            Map<String, Object> body = objectMapper.readValue(exchange.getRequestBody(), Map.class);
            String username = (String) body.get("username");
            String fullName = (String) body.get("fullName");
            String email = (String) body.get("email");
            String password = (String) body.get("password");
            double initialCash = body.get("initialCash") != null ? ((Number) body.get("initialCash")).doubleValue() : 100000.0;

            User u = authService.registerUser(username, fullName, email, password, User.RiskProfile.MODERATE, initialCash);
            authService.setCurrentUser(u);

            sendJsonResponse(exchange, 200, Map.of(
                    "success", true,
                    "user", Map.of(
                            "id", u.getId(),
                            "username", u.getUsername(),
                            "fullName", u.getFullName(),
                            "email", u.getEmail() != null ? u.getEmail() : ""
                    )
            ));
        } catch (Exception e) {
            sendJsonResponse(exchange, 400, Map.of("success", false, "error", e.getMessage()));
        }
    }

    private void handleAuthMe(HttpExchange exchange) throws IOException {
        User u = authService.getCurrentUser();
        if (u != null) {
            sendJsonResponse(exchange, 200, Map.of(
                    "loggedIn", true,
                    "user", Map.of(
                            "id", u.getId(),
                            "username", u.getUsername(),
                            "fullName", u.getFullName(),
                            "email", u.getEmail() != null ? u.getEmail() : ""
                    )
            ));
        } else {
            sendJsonResponse(exchange, 200, Map.of("loggedIn", false));
        }
    }

    private void handleAuthLogout(HttpExchange exchange) throws IOException {
        authService.logout();
        sendJsonResponse(exchange, 200, Map.of("success", true));
    }

    private void handleAuthProfile(HttpExchange exchange) throws IOException {
        User u = authService.getCurrentUser();
        if (u == null) {
            sendJsonResponse(exchange, 401, Map.of("loggedIn", false, "error", "User not logged in"));
            return;
        }

        Portfolio portfolio = portfolioService.getPortfolioForUser(u.getId());
        Map<String, Stock> marketStocks = simulationEngine.getAllStocks();
        PerformanceMetrics metrics = portfolioService.calculatePerformanceMetrics(portfolio);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", u.getId());
        resp.put("username", u.getUsername());
        resp.put("fullName", u.getFullName());
        resp.put("email", u.getEmail() != null ? u.getEmail() : "");
        resp.put("riskProfile", u.getRiskProfile() != null ? u.getRiskProfile().name() : "MODERATE");
        resp.put("kycStatus", "VERIFIED ✅");
        resp.put("segment", "Equity & Intraday Active");
        resp.put("accountType", "Paper Trading Pro");
        resp.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString().substring(0, 10) : "2026-08-27");
        resp.put("portfolioId", portfolio.getId());
        resp.put("portfolioName", portfolio.getName());
        resp.put("initialCash", portfolio.getInitialCash());
        resp.put("cashBalance", portfolio.getCashBalance());
        resp.put("totalEquity", portfolio.getTotalEquity(marketStocks));
        resp.put("totalHoldingsValue", portfolio.getTotalHoldingsValue(marketStocks));
        resp.put("totalReturnPercent", portfolio.getTotalReturnPercent(marketStocks));
        resp.put("realizedPnL", portfolio.getTotalRealizedPnL());
        resp.put("unrealizedPnL", portfolio.getTotalUnrealizedPnL(marketStocks));
        resp.put("totalDividends", portfolio.getTotalDividends());
        resp.put("winRate", metrics.getWinRate());
        resp.put("sharpeRatio", metrics.getSharpeRatio());
        resp.put("profitFactor", metrics.getProfitFactor());
        resp.put("totalTrades", metrics.getTotalTrades());

        sendJsonResponse(exchange, 200, resp);
    }

    private void handleMarket(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendCorsOptions(exchange);
            return;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Stock s : marketDataService.getAllStocks()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("symbol", s.getSymbol());
            map.put("name", s.getCompanyName());
            map.put("sector", s.getSector());
            map.put("price", s.getCurrentPrice());
            map.put("change", s.getChange());
            map.put("changePercent", s.getChangePercent());
            map.put("bid", s.getBidPrice());
            map.put("ask", s.getAskPrice());
            map.put("volume", s.getVolume());
            map.put("dayHigh", s.getDayHigh());
            map.put("dayLow", s.getDayLow());
            map.put("openPrice", s.getOpenPrice());
            map.put("volatility", s.getVolatility());
            map.put("dividendYield", s.getDividendYield());
            map.put("marketCap", s.getMarketCap());

            List<Double> sparkline = s.getPriceHistory().stream().map(Stock.PricePoint::getPrice).toList();
            map.put("sparkline", sparkline);
            list.add(map);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("stocks", list);
        resp.put("sectors", marketDataService.getSectorPerformance());
        resp.put("simSpeed", simulationEngine.getSpeed());
        resp.put("simRunning", simulationEngine.isRunning());
        sendJsonResponse(exchange, 200, resp);
    }

    private void handleStockDetail(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String symbol = "AAPL";
        if (query != null && query.contains("symbol=")) {
            symbol = query.split("symbol=")[1].split("&")[0].toUpperCase();
        }

        Stock stock = marketDataService.getStock(symbol);
        if (stock == null) {
            sendJsonResponse(exchange, 404, Map.of("error", "Stock not found"));
            return;
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("symbol", stock.getSymbol());
        resp.put("name", stock.getCompanyName());
        resp.put("sector", stock.getSector());
        resp.put("price", stock.getCurrentPrice());
        resp.put("change", stock.getChange());
        resp.put("changePercent", stock.getChangePercent());
        resp.put("bid", stock.getBidPrice());
        resp.put("ask", stock.getAskPrice());
        resp.put("volume", stock.getVolume());
        resp.put("dayHigh", stock.getDayHigh());
        resp.put("dayLow", stock.getDayLow());
        resp.put("history", stock.getPriceHistory());

        sendJsonResponse(exchange, 200, resp);
    }

    private void handlePortfolio(HttpExchange exchange) throws IOException {
        User user = authService.getCurrentUser();
        Portfolio portfolio = portfolioService.getPortfolioForUser(user.getId());
        Map<String, Stock> marketStocks = simulationEngine.getAllStocks();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("portfolioId", portfolio.getId());
        resp.put("portfolioName", portfolio.getName());
        resp.put("userFullName", user.getFullName());
        resp.put("username", user.getUsername());
        resp.put("cashBalance", portfolio.getCashBalance());
        resp.put("initialCash", portfolio.getInitialCash());
        resp.put("totalHoldingsValue", portfolio.getTotalHoldingsValue(marketStocks));
        resp.put("totalEquity", portfolio.getTotalEquity(marketStocks));
        resp.put("totalReturnPercent", portfolio.getTotalReturnPercent(marketStocks));
        resp.put("totalRealizedPnL", portfolio.getTotalRealizedPnL());
        resp.put("totalUnrealizedPnL", portfolio.getTotalUnrealizedPnL(marketStocks));
        resp.put("totalDividends", portfolio.getTotalDividends());

        List<Map<String, Object>> holdings = new ArrayList<>();
        for (Holding h : portfolio.getHoldings().values()) {
            if (h.getQuantity() > 0 || h.getRealizedPnL() != 0) {
                Stock s = marketStocks.get(h.getSymbol());
                double curPrice = s != null ? s.getCurrentPrice() : 0.0;
                Map<String, Object> hm = new LinkedHashMap<>();
                hm.put("symbol", h.getSymbol());
                hm.put("quantity", h.getQuantity());
                hm.put("avgCost", Math.round(h.getAverageCostBasis() * 100.0) / 100.0);
                hm.put("currentPrice", curPrice);
                hm.put("marketValue", Math.round(h.getMarketValue(curPrice) * 100.0) / 100.0);
                hm.put("unrealizedPnL", Math.round(h.getUnrealizedPnL(curPrice) * 100.0) / 100.0);
                hm.put("unrealizedPnLPercent", Math.round(h.getUnrealizedPnLPercent(curPrice) * 100.0) / 100.0);
                hm.put("realizedPnL", Math.round(h.getRealizedPnL() * 100.0) / 100.0);
                hm.put("dividends", Math.round(h.getTotalDividendsReceived() * 100.0) / 100.0);
                holdings.add(hm);
            }
        }
        resp.put("holdings", holdings);
        resp.put("equityHistory", portfolio.getEquityHistory());

        sendJsonResponse(exchange, 200, resp);
    }

    private void handleMetrics(HttpExchange exchange) throws IOException {
        User user = authService.getCurrentUser();
        Portfolio portfolio = portfolioService.getPortfolioForUser(user.getId());
        PerformanceMetrics metrics = portfolioService.calculatePerformanceMetrics(portfolio);
        sendJsonResponse(exchange, 200, metrics);
    }

    private void handleNews(HttpExchange exchange) throws IOException {
        sendJsonResponse(exchange, 200, marketDataService.getLatestNews());
    }

    private void handleOrders(HttpExchange exchange) throws IOException {
        User user = authService.getCurrentUser();
        Map<String, Object> resp = new HashMap<>();
        resp.put("pending", tradingService.getPendingOrders(user.getId()));
        resp.put("history", tradingService.getOrderHistory(user.getId()));
        sendJsonResponse(exchange, 200, resp);
    }

    private void handleTransactions(HttpExchange exchange) throws IOException {
        User user = authService.getCurrentUser();
        Portfolio portfolio = portfolioService.getPortfolioForUser(user.getId());
        List<Transaction> txs = portfolioService.getTransactionHistory(portfolio.getId());
        sendJsonResponse(exchange, 200, txs);
    }

    @SuppressWarnings("unchecked")
    private void handlePlaceOrder(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendCorsOptions(exchange);
            return;
        }

        try {
            Map<String, Object> body = objectMapper.readValue(exchange.getRequestBody(), Map.class);
            String symbol = (String) body.get("symbol");
            String sideStr = (String) body.get("side");
            String typeStr = (String) body.get("type");
            int quantity = ((Number) body.get("quantity")).intValue();
            Double limitPrice = body.get("limitPrice") != null ? ((Number) body.get("limitPrice")).doubleValue() : null;
            Double stopPrice = body.get("stopPrice") != null ? ((Number) body.get("stopPrice")).doubleValue() : null;
            Double trailingPercent = body.get("trailingPercent") != null ? ((Number) body.get("trailingPercent")).doubleValue() : null;

            OrderSide side = OrderSide.valueOf(sideStr.toUpperCase());
            OrderType type = OrderType.valueOf(typeStr.toUpperCase());

            User user = authService.getCurrentUser();
            Portfolio portfolio = portfolioService.getPortfolioForUser(user.getId());

            Order order = tradingService.placeOrder(portfolio, symbol, side, type, quantity, limitPrice, stopPrice, trailingPercent);
            sendJsonResponse(exchange, 200, Map.of("success", true, "order", order));
        } catch (Exception e) {
            sendJsonResponse(exchange, 400, Map.of("success", false, "error", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleCancelOrder(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendCorsOptions(exchange);
            return;
        }
        try {
            Map<String, Object> body = objectMapper.readValue(exchange.getRequestBody(), Map.class);
            String orderId = (String) body.get("orderId");
            boolean ok = tradingService.cancelOrder(orderId, "Cancelled via Web UI");
            sendJsonResponse(exchange, 200, Map.of("success", ok));
        } catch (Exception e) {
            sendJsonResponse(exchange, 400, Map.of("success", false, "error", e.getMessage()));
        }
    }

    private void handleTriggerNews(HttpExchange exchange) throws IOException {
        marketDataService.triggerRandomNews();
        sendJsonResponse(exchange, 200, Map.of("success", true, "message", "Macro news event injected into market."));
    }

    @SuppressWarnings("unchecked")
    private void handleFunds(HttpExchange exchange) throws IOException {
        try {
            Map<String, Object> body = objectMapper.readValue(exchange.getRequestBody(), Map.class);
            String action = (String) body.get("action");
            double amount = ((Number) body.get("amount")).doubleValue();
            User user = authService.getCurrentUser();
            Portfolio portfolio = portfolioService.getPortfolioForUser(user.getId());

            if ("deposit".equalsIgnoreCase(action)) {
                portfolioService.depositFunds(portfolio, amount, "Web Cash Deposit");
            } else {
                portfolioService.withdrawFunds(portfolio, amount, "Web Cash Withdrawal");
            }
            sendJsonResponse(exchange, 200, Map.of("success", true, "cashBalance", portfolio.getCashBalance()));
        } catch (Exception e) {
            sendJsonResponse(exchange, 400, Map.of("success", false, "error", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleEngineSpeed(HttpExchange exchange) throws IOException {
        try {
            Map<String, Object> body = objectMapper.readValue(exchange.getRequestBody(), Map.class);
            int speed = ((Number) body.get("speed")).intValue();
            simulationEngine.setSpeed(speed);
            sendJsonResponse(exchange, 200, Map.of("success", true, "speed", simulationEngine.getSpeed()));
        } catch (Exception e) {
            sendJsonResponse(exchange, 400, Map.of("success", false, "error", e.getMessage()));
        }
    }

    private void sendCorsOptions(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
    }

    /**
     * Serves the single-page web dashboard application.
     */
    private static class StaticDashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (!path.equals("/") && !path.equals("/index.html")) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            String htmlContent = WebStaticAssets.getIndexHtml();
            byte[] htmlBytes = htmlContent.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, htmlBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(htmlBytes);
            }
        }
    }
}
