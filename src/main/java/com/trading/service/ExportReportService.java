package com.trading.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trading.domain.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for exporting portfolio statements, transaction history, and tax reports
 * to CSV and JSON formats.
 */
public class ExportReportService {
    private final ObjectMapper objectMapper;

    public ExportReportService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public File exportTransactionsToCSV(List<Transaction> transactions, String targetFilePath) throws IOException {
        File file = new File(targetFilePath);
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Transaction ID,Timestamp,Type,Symbol,Quantity,Price,Total Amount,Fee,Notes");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Transaction tx : transactions) {
                writer.printf("%s,%s,%s,%s,%d,%.2f,%.2f,%.2f,\"%s\"%n",
                        tx.getId(),
                        tx.getTimestamp().format(fmt),
                        tx.getType(),
                        tx.getSymbol(),
                        tx.getQuantity(),
                        tx.getPrice(),
                        tx.getTotalAmount(),
                        tx.getFee(),
                        tx.getNotes().replace("\"", "\"\""));
            }
        }
        return file;
    }

    public File exportPortfolioStatementToJSON(Portfolio portfolio, Map<String, Stock> marketStocks,
                                               PerformanceMetrics metrics, List<Transaction> transactions,
                                               String targetFilePath) throws IOException {
        Map<String, Object> statement = new LinkedHashMap<>();
        statement.put("reportGeneratedAt", LocalDateTime.now().toString());
        statement.put("portfolioId", portfolio.getId());
        statement.put("portfolioName", portfolio.getName());
        statement.put("userId", portfolio.getUserId());
        statement.put("cashBalance", portfolio.getCashBalance());
        statement.put("initialCash", portfolio.getInitialCash());
        statement.put("totalHoldingsValue", portfolio.getTotalHoldingsValue(marketStocks));
        statement.put("totalEquity", portfolio.getTotalEquity(marketStocks));

        // Holdings Breakdown
        List<Map<String, Object>> holdingsList = new ArrayList<>();
        for (Holding h : portfolio.getHoldings().values()) {
            if (h.getQuantity() > 0) {
                Stock s = marketStocks.get(h.getSymbol());
                double curPrice = s != null ? s.getCurrentPrice() : 0.0;
                Map<String, Object> hMap = new LinkedHashMap<>();
                hMap.put("symbol", h.getSymbol());
                hMap.put("quantity", h.getQuantity());
                hMap.put("averageCostBasis", Math.round(h.getAverageCostBasis() * 100.0) / 100.0);
                hMap.put("currentPrice", curPrice);
                hMap.put("marketValue", Math.round(h.getMarketValue(curPrice) * 100.0) / 100.0);
                hMap.put("unrealizedPnL", Math.round(h.getUnrealizedPnL(curPrice) * 100.0) / 100.0);
                hMap.put("unrealizedPnLPercent", Math.round(h.getUnrealizedPnLPercent(curPrice) * 100.0) / 100.0);
                hMap.put("realizedPnL", Math.round(h.getRealizedPnL() * 100.0) / 100.0);
                holdingsList.add(hMap);
            }
        }
        statement.put("holdings", holdingsList);

        // Performance Metrics
        Map<String, Object> perfMap = new LinkedHashMap<>();
        perfMap.put("totalReturnAmount", metrics.getTotalReturnAmount());
        perfMap.put("totalReturnPercent", metrics.getTotalReturnPercent());
        perfMap.put("realizedPnL", metrics.getRealizedPnL());
        perfMap.put("unrealizedPnL", metrics.getUnrealizedPnL());
        perfMap.put("totalDividends", metrics.getTotalDividends());
        perfMap.put("sharpeRatio", metrics.getSharpeRatio());
        perfMap.put("maxDrawdownPercent", metrics.getMaxDrawdownPercent());
        perfMap.put("winRatePercent", metrics.getWinRate());
        perfMap.put("profitFactor", metrics.getProfitFactor());
        perfMap.put("totalTrades", metrics.getTotalTrades());
        perfMap.put("winningTrades", metrics.getWinningTrades());
        perfMap.put("losingTrades", metrics.getLosingTrades());
        statement.put("performanceMetrics", perfMap);

        statement.put("recentTransactions", transactions);

        File file = new File(targetFilePath);
        objectMapper.writeValue(file, statement);
        return file;
    }
}
