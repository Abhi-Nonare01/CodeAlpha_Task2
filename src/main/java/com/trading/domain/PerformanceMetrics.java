package com.trading.domain;

import java.io.Serializable;
import java.util.List;

/**
 * Calculates and encapsulates institutional-grade portfolio risk and return metrics:
 * Total Return %, Realized/Unrealized P&L, Sharpe Ratio, Max Drawdown, Win Rate, and Profit Factor.
 */
public class PerformanceMetrics implements Serializable {
    private static final long serialVersionUID = 1L;

    private final double totalEquity;
    private final double cashBalance;
    private final double holdingsValue;
    private final double totalReturnAmount;
    private final double totalReturnPercent;
    private final double realizedPnL;
    private final double unrealizedPnL;
    private final double totalDividends;
    private final double sharpeRatio;
    private final double maxDrawdownPercent;
    private final double winRate;
    private final int totalTrades;
    private final int winningTrades;
    private final int losingTrades;
    private final double profitFactor;

    public PerformanceMetrics(double totalEquity, double cashBalance, double holdingsValue,
                              double totalReturnAmount, double totalReturnPercent,
                              double realizedPnL, double unrealizedPnL, double totalDividends,
                              double sharpeRatio, double maxDrawdownPercent,
                              double winRate, int totalTrades, int winningTrades, int losingTrades, double profitFactor) {
        this.totalEquity = Math.round(totalEquity * 100.0) / 100.0;
        this.cashBalance = Math.round(cashBalance * 100.0) / 100.0;
        this.holdingsValue = Math.round(holdingsValue * 100.0) / 100.0;
        this.totalReturnAmount = Math.round(totalReturnAmount * 100.0) / 100.0;
        this.totalReturnPercent = Math.round(totalReturnPercent * 100.0) / 100.0;
        this.realizedPnL = Math.round(realizedPnL * 100.0) / 100.0;
        this.unrealizedPnL = Math.round(unrealizedPnL * 100.0) / 100.0;
        this.totalDividends = Math.round(totalDividends * 100.0) / 100.0;
        this.sharpeRatio = Math.round(sharpeRatio * 100.0) / 100.0;
        this.maxDrawdownPercent = Math.round(maxDrawdownPercent * 100.0) / 100.0;
        this.winRate = Math.round(winRate * 100.0) / 100.0;
        this.totalTrades = totalTrades;
        this.winningTrades = winningTrades;
        this.losingTrades = losingTrades;
        this.profitFactor = Math.round(profitFactor * 100.0) / 100.0;
    }

    public static PerformanceMetrics calculate(Portfolio portfolio, java.util.Map<String, Stock> marketStocks,
                                              List<Transaction> transactions) {
        double cash = portfolio.getCashBalance();
        double holdingsVal = portfolio.getTotalHoldingsValue(marketStocks);
        double currentEquity = cash + holdingsVal;
        double initialCash = portfolio.getInitialCash();
        double totalReturnAmt = currentEquity - initialCash;
        double totalReturnPct = initialCash > 0 ? (totalReturnAmt / initialCash) * 100.0 : 0.0;
        double realized = portfolio.getTotalRealizedPnL();
        double unrealized = portfolio.getTotalUnrealizedPnL(marketStocks);
        double dividends = portfolio.getTotalDividends();

        // Calculate Win Rate, Profitable Trades, and Profit Factor from transactions
        int trades = 0;
        int wins = 0;
        int losses = 0;
        double grossProfit = 0.0;
        double grossLoss = 0.0;

        for (Transaction tx : transactions) {
            if (tx.getType() == Transaction.Type.SELL) {
                trades++;
                // Check if trade had positive realized value
                Holding h = portfolio.getHolding(tx.getSymbol());
                double avgCost = (h != null && h.getAverageCostBasis() > 0) ? h.getAverageCostBasis() : tx.getPrice();
                double profit = (tx.getPrice() - avgCost) * tx.getQuantity() - tx.getFee();
                if (profit >= 0) {
                    wins++;
                    grossProfit += profit;
                } else {
                    losses++;
                    grossLoss += Math.abs(profit);
                }
            }
        }

        double winRatePct = trades > 0 ? ((double) wins / trades) * 100.0 : 0.0;
        double profitFactor = (grossLoss > 0) ? (grossProfit / grossLoss) : (grossProfit > 0 ? 10.0 : 1.0);

        // Calculate Maximum Drawdown & Sharpe Ratio from equity history
        List<Portfolio.EquityPoint> history = portfolio.getEquityHistory();
        double maxEquity = 0.0;
        double maxDrawdownPct = 0.0;
        List<Double> periodicReturns = new java.util.ArrayList<>();

        for (int i = 0; i < history.size(); i++) {
            double eq = history.get(i).getEquity();
            if (eq > maxEquity) {
                maxEquity = eq;
            }
            if (maxEquity > 0) {
                double dd = ((maxEquity - eq) / maxEquity) * 100.0;
                if (dd > maxDrawdownPct) {
                    maxDrawdownPct = dd;
                }
            }
            if (i > 0) {
                double prevEq = history.get(i - 1).getEquity();
                if (prevEq > 0) {
                    periodicReturns.add((eq - prevEq) / prevEq);
                }
            }
        }

        // Sharpe Ratio (assuming risk free rate = 0 for intraday intervals)
        double sharpe = 0.0;
        if (periodicReturns.size() >= 3) {
            double sum = 0.0;
            for (double r : periodicReturns) sum += r;
            double mean = sum / periodicReturns.size();

            double varianceSum = 0.0;
            for (double r : periodicReturns) {
                varianceSum += Math.pow(r - mean, 2);
            }
            double stdDev = Math.sqrt(varianceSum / (periodicReturns.size() - 1));
            if (stdDev > 0.00001) {
                // Annualized Sharpe approximation
                sharpe = (mean / stdDev) * Math.sqrt(252);
            }
        }

        return new PerformanceMetrics(currentEquity, cash, holdingsVal, totalReturnAmt, totalReturnPct,
                realized, unrealized, dividends, sharpe, maxDrawdownPct, winRatePct, trades, wins, losses, profitFactor);
    }

    // Getters
    public double getTotalEquity() { return totalEquity; }
    public double getCashBalance() { return cashBalance; }
    public double getHoldingsValue() { return holdingsValue; }
    public double getTotalReturnAmount() { return totalReturnAmount; }
    public double getTotalReturnPercent() { return totalReturnPercent; }
    public double getRealizedPnL() { return realizedPnL; }
    public double getUnrealizedPnL() { return unrealizedPnL; }
    public double getTotalDividends() { return totalDividends; }
    public double getSharpeRatio() { return sharpeRatio; }
    public double getMaxDrawdownPercent() { return maxDrawdownPercent; }
    public double getWinRate() { return winRate; }
    public int getTotalTrades() { return totalTrades; }
    public int getWinningTrades() { return winningTrades; }
    public int getLosingTrades() { return losingTrades; }
    public double getProfitFactor() { return profitFactor; }
}
