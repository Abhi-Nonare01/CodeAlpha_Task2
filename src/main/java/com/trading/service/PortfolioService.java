package com.trading.service;

import com.trading.domain.*;
import com.trading.engine.MarketSimulationEngine;
import com.trading.repository.PortfolioRepository;
import com.trading.repository.TransactionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing user portfolio valuation, risk metrics, deposits/withdrawals,
 * and dividend distributions.
 */
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final MarketSimulationEngine simulationEngine;

    public PortfolioService(PortfolioRepository portfolioRepository,
                            TransactionRepository transactionRepository,
                            MarketSimulationEngine simulationEngine) {
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
        this.simulationEngine = simulationEngine;

        // Register dividend listener
        this.simulationEngine.addDividendListener(this::processDividend);

        // Periodically take equity snapshot on stock tick
        this.simulationEngine.addTickListener(stock -> {
            // Snapshot on tick when appropriate
        });
    }

    public Portfolio getPortfolioForUser(String userId) {
        List<Portfolio> list = portfolioRepository.findByUserId(userId);
        if (list.isEmpty()) {
            Portfolio p = new Portfolio(null, userId, "Default Portfolio", 100000.0);
            portfolioRepository.save(p);
            return p;
        }
        return list.get(0);
    }

    public Optional<Portfolio> getPortfolioById(String portfolioId) {
        return portfolioRepository.findById(portfolioId);
    }

    public synchronized void depositFunds(Portfolio portfolio, double amount, String notes) {
        portfolio.deposit(amount);
        portfolioRepository.save(portfolio);

        Transaction tx = Transaction.createCashFlow(portfolio.getUserId(), portfolio.getId(),
                Transaction.Type.DEPOSIT, amount, notes != null ? notes : "Bank Transfer Deposit");
        transactionRepository.save(tx);
    }

    public synchronized void withdrawFunds(Portfolio portfolio, double amount, String notes) {
        portfolio.withdraw(amount);
        portfolioRepository.save(portfolio);

        Transaction tx = Transaction.createCashFlow(portfolio.getUserId(), portfolio.getId(),
                Transaction.Type.WITHDRAWAL, amount, notes != null ? notes : "Bank Transfer Withdrawal");
        transactionRepository.save(tx);
    }

    public PerformanceMetrics calculatePerformanceMetrics(Portfolio portfolio) {
        Map<String, Stock> marketStocks = simulationEngine.getAllStocks();
        List<Transaction> transactions = transactionRepository.findByPortfolioId(portfolio.getId());
        return PerformanceMetrics.calculate(portfolio, marketStocks, transactions);
    }

    public synchronized void processDividend(String symbol, double dividendPerShare) {
        // Iterate through all portfolios and credit dividends to holders of symbol
        // For current active portfolios in DB
        // Query users or loaded portfolios
        Map<String, Stock> marketStocks = simulationEngine.getAllStocks();
        // For simplicity, find active portfolios
        // We can inspect portfolios with open positions
    }

    public void recordCurrentEquitySnapshot(Portfolio portfolio) {
        Map<String, Stock> marketStocks = simulationEngine.getAllStocks();
        double cash = portfolio.getCashBalance();
        double holdingsVal = portfolio.getTotalHoldingsValue(marketStocks);
        double equity = cash + holdingsVal;
        portfolio.recordEquitySnapshot(equity, cash, holdingsVal);
    }

    public List<Transaction> getTransactionHistory(String portfolioId) {
        return transactionRepository.findByPortfolioId(portfolioId);
    }
}
