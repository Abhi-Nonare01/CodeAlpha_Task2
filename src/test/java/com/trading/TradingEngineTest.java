package com.trading;

import com.trading.domain.*;
import com.trading.engine.MarketSimulationEngine;
import com.trading.engine.OrderMatchingEngine;
import com.trading.engine.PricingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class TradingEngineTest {

    private Stock appleStock;
    private Portfolio portfolio;
    private OrderMatchingEngine matchingEngine;

    @BeforeEach
    public void setUp() {
        appleStock = new Stock("AAPL", "Apple Inc.", "Technology", 150.00, 0.20, 0.08, 0.50, 2500.0);
        portfolio = new Portfolio("PORT-1", "USER-1", "Test Portfolio", 50000.0);
        matchingEngine = new OrderMatchingEngine();
    }

    @Test
    public void testStockPriceEvolutionAndSpread() {
        assertEquals("AAPL", appleStock.getSymbol());
        assertEquals(150.00, appleStock.getCurrentPrice(), 0.001);
        assertTrue(appleStock.getBidPrice() < appleStock.getCurrentPrice());
        assertTrue(appleStock.getAskPrice() > appleStock.getCurrentPrice());

        // Update price
        appleStock.updatePrice(160.00, 500);
        assertEquals(160.00, appleStock.getCurrentPrice(), 0.001);
        assertEquals(10.00, appleStock.getChange(), 0.001);
        assertEquals(6.67, appleStock.getChangePercent(), 0.01);
        assertEquals(160.00, appleStock.getDayHigh(), 0.001);
        assertEquals(150.00, appleStock.getDayLow(), 0.001);
    }

    @Test
    public void testHoldingCostBasisAndRealizedPnL() {
        Holding holding = new Holding("AAPL");
        assertEquals(0, holding.getQuantity());

        // Buy 10 shares @ $150 with $1 fee -> total cost $1501
        holding.addShares(10, 150.00, 1.00);
        assertEquals(10, holding.getQuantity());
        assertEquals(150.10, holding.getAverageCostBasis(), 0.01);

        // Buy 10 more shares @ $200 with $1 fee -> total cost $1501 + $2001 = $3502
        holding.addShares(10, 200.00, 1.00);
        assertEquals(20, holding.getQuantity());
        assertEquals(175.10, holding.getAverageCostBasis(), 0.01);

        // Sell 10 shares @ $220 with $1 fee
        // Cost of 10 shares sold = 175.10 * 10 = $1751
        // Proceeds = 220 * 10 - 1 = $2199
        // Profit = 2199 - 1751 = $448
        double profit = holding.removeShares(10, 220.00, 1.00);
        assertEquals(10, holding.getQuantity());
        assertEquals(448.00, profit, 0.1);
        assertEquals(448.00, holding.getRealizedPnL(), 0.1);
    }

    @Test
    public void testPortfolioValuationAndEquity() {
        Map<String, Stock> market = new HashMap<>();
        market.put("AAPL", appleStock);

        Holding h = portfolio.getOrCreateHolding("AAPL");
        h.addShares(100, 150.00, 5.00);
        portfolio.deductCash(15005.00);

        assertEquals(34995.00, portfolio.getCashBalance(), 0.01);
        assertEquals(15000.00, portfolio.getTotalHoldingsValue(market), 0.01);
        assertEquals(49995.00, portfolio.getTotalEquity(market), 0.01);

        // Price rises to $170
        appleStock.updatePrice(170.00, 1000);
        assertEquals(17000.00, portfolio.getTotalHoldingsValue(market), 0.01);
        assertEquals(51995.00, portfolio.getTotalEquity(market), 0.01);
        assertEquals(1995.00, portfolio.getTotalUnrealizedPnL(market), 0.01);
    }

    @Test
    public void testMarketOrderExecution() {
        AtomicBoolean executed = new AtomicBoolean(false);
        matchingEngine.addListener((order, execPrice, fee) -> {
            executed.set(true);
            assertEquals(OrderStatus.FILLED, order.getStatus());
            assertTrue(execPrice > 0);
            assertTrue(fee >= 1.00);
        });

        Order marketBuy = Order.createMarketOrder("U1", "P1", "AAPL", OrderSide.BUY, 25);
        matchingEngine.submitOrder(marketBuy, appleStock);

        assertTrue(executed.get());
        assertEquals(OrderStatus.FILLED, marketBuy.getStatus());
    }

    @Test
    public void testLimitOrderExecution() {
        AtomicBoolean executed = new AtomicBoolean(false);
        matchingEngine.addListener((order, execPrice, fee) -> {
            executed.set(true);
        });

        // Current price is $150. Place limit buy order at $140 (not marketable yet)
        Order limitBuy = Order.createLimitOrder("U1", "P1", "AAPL", OrderSide.BUY, 10, 140.00);
        matchingEngine.submitOrder(limitBuy, appleStock);

        assertEquals(OrderStatus.PENDING, limitBuy.getStatus());
        assertFalse(executed.get());
        assertEquals(1, matchingEngine.getPendingOrders().size());

        // Simulate market price dropping to $139
        appleStock.updatePrice(139.00, 100);
        matchingEngine.processStockTick(appleStock);

        assertTrue(executed.get());
        assertEquals(OrderStatus.FILLED, limitBuy.getStatus());
        assertEquals(0, matchingEngine.getPendingOrders().size());
    }

    @Test
    public void testStopLossOrderTrigger() {
        AtomicBoolean executed = new AtomicBoolean(false);
        matchingEngine.addListener((order, execPrice, fee) -> {
            executed.set(true);
        });

        // Place Stop-Loss SELL at $145 while price is $150
        Order stopLoss = Order.createStopLossOrder("U1", "P1", "AAPL", OrderSide.SELL, 10, 145.00);
        matchingEngine.submitOrder(stopLoss, appleStock);

        assertEquals(OrderStatus.PENDING, stopLoss.getStatus());

        // Price drops below $145
        appleStock.updatePrice(144.50, 100);
        matchingEngine.processStockTick(appleStock);

        assertTrue(executed.get());
        assertEquals(OrderStatus.FILLED, stopLoss.getStatus());
    }

    @Test
    public void testPerformanceMetricsCalculation() {
        Map<String, Stock> market = new HashMap<>();
        market.put("AAPL", appleStock);

        portfolio.recordEquitySnapshot(50000.0, 50000.0, 0.0);
        portfolio.recordEquitySnapshot(52000.0, 30000.0, 22000.0);
        portfolio.recordEquitySnapshot(51000.0, 30000.0, 21000.0);

        List<Transaction> txs = List.of(
                new Transaction("T1", "U1", "P1", "O1", "AAPL", Transaction.Type.BUY, 10, 150.0, 1500.0, 1.0, null, "Buy"),
                new Transaction("T2", "U1", "P1", "O2", "AAPL", Transaction.Type.SELL, 10, 170.0, 1700.0, 1.0, null, "Sell win")
        );

        PerformanceMetrics metrics = PerformanceMetrics.calculate(portfolio, market, txs);
        assertNotNull(metrics);
        assertTrue(metrics.getWinRate() >= 0.0);
    }
}
