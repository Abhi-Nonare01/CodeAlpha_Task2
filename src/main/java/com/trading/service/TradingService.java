package com.trading.service;

import com.trading.domain.*;
import com.trading.engine.MarketSimulationEngine;
import com.trading.engine.OrderMatchingEngine;
import com.trading.repository.OrderRepository;
import com.trading.repository.PortfolioRepository;
import com.trading.repository.TransactionRepository;
import java.util.List;
import java.util.Optional;

/**
 * Service orchestrating order placement, validation, execution callbacks,
 * and transactional balance/holding updates.
 */
public class TradingService {
    private final OrderMatchingEngine matchingEngine;
    private final MarketSimulationEngine simulationEngine;
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;

    public TradingService(OrderMatchingEngine matchingEngine,
                          MarketSimulationEngine simulationEngine,
                          PortfolioRepository portfolioRepository,
                          TransactionRepository transactionRepository,
                          OrderRepository orderRepository) {
        this.matchingEngine = matchingEngine;
        this.simulationEngine = simulationEngine;
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;

        // Register execution callback with matching engine
        this.matchingEngine.addListener(this::handleOrderExecution);
    }

    public synchronized Order placeOrder(Portfolio portfolio, String symbol, OrderSide side, OrderType type,
                                         int quantity, Double limitPrice, Double stopPrice, Double trailingPercent) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive. Provided: " + quantity);
        }

        Stock stock = simulationEngine.getStock(symbol);
        if (stock == null) {
            throw new IllegalArgumentException("Unknown stock symbol: " + symbol);
        }

        // Validate funds/shares availability before submitting order
        double estimatedPrice = stock.getCurrentPrice();
        if (limitPrice != null && limitPrice > 0) {
            estimatedPrice = limitPrice;
        }

        if (side == OrderSide.BUY) {
            double estimatedCost = (quantity * estimatedPrice) * 1.002; // buffer for fee/spread
            if (portfolio.getCashBalance() < estimatedCost && type == OrderType.MARKET) {
                throw new IllegalStateException(String.format("Insufficient cash balance! Required: ~$%.2f, Available: $%.2f",
                        estimatedCost, portfolio.getCashBalance()));
            }
        } else if (side == OrderSide.SELL) {
            Holding holding = portfolio.getHolding(symbol);
            int owned = (holding != null) ? holding.getQuantity() : 0;
            if (owned < quantity) {
                throw new IllegalStateException(String.format("Insufficient shares to sell! Owned: %d, Requested: %d",
                        owned, quantity));
            }
        }

        Order order = new Order(null, portfolio.getUserId(), portfolio.getId(), symbol, side, type, quantity, limitPrice, stopPrice, trailingPercent);
        orderRepository.save(order);

        // Submit to matching engine
        matchingEngine.submitOrder(order, stock);

        return order;
    }

    public synchronized Order placeMarketOrder(Portfolio portfolio, String symbol, OrderSide side, int quantity) {
        return placeOrder(portfolio, symbol, side, OrderType.MARKET, quantity, null, null, null);
    }

    public synchronized Order placeLimitOrder(Portfolio portfolio, String symbol, OrderSide side, int quantity, double limitPrice) {
        return placeOrder(portfolio, symbol, side, OrderType.LIMIT, quantity, limitPrice, null, null);
    }

    public synchronized Order placeStopLossOrder(Portfolio portfolio, String symbol, OrderSide side, int quantity, double stopPrice) {
        return placeOrder(portfolio, symbol, side, OrderType.STOP_LOSS, quantity, null, stopPrice, null);
    }

    public synchronized Order placeTrailingStopOrder(Portfolio portfolio, String symbol, OrderSide side, int quantity, double trailingPct) {
        return placeOrder(portfolio, symbol, side, OrderType.TRAILING_STOP, quantity, null, null, trailingPct);
    }

    public synchronized boolean cancelOrder(String orderId, String reason) {
        boolean cancelled = matchingEngine.cancelOrder(orderId, reason);
        if (cancelled) {
            // update in repository
            for (Order o : matchingEngine.getOrderHistory()) {
                if (o.getId().equalsIgnoreCase(orderId)) {
                    orderRepository.save(o);
                    break;
                }
            }
        }
        return cancelled;
    }

    private synchronized void handleOrderExecution(Order order, double execPrice, double fee) {
        Optional<Portfolio> optPort = portfolioRepository.findById(order.getPortfolioId());
        if (optPort.isEmpty()) {
            System.err.println("Execution failed: Portfolio not found: " + order.getPortfolioId());
            return;
        }

        Portfolio portfolio = optPort.get();
        double totalTradeVal = order.getQuantity() * execPrice;

        if (order.getSide() == OrderSide.BUY) {
            double totalCost = totalTradeVal + fee;
            try {
                portfolio.deductCash(totalCost);
                Holding holding = portfolio.getOrCreateHolding(order.getSymbol());
                holding.addShares(order.getQuantity(), execPrice, fee);
            } catch (Exception e) {
                System.err.println("Buy execution settlement error: " + e.getMessage());
                return;
            }
        } else if (order.getSide() == OrderSide.SELL) {
            double netProceeds = totalTradeVal - fee;
            try {
                Holding holding = portfolio.getHolding(order.getSymbol());
                if (holding != null) {
                    holding.removeShares(order.getQuantity(), execPrice, fee);
                    portfolio.addCash(netProceeds);
                }
            } catch (Exception e) {
                System.err.println("Sell execution settlement error: " + e.getMessage());
                return;
            }
        }

        // Persist updated portfolio & holdings
        portfolioRepository.save(portfolio);

        // Create and persist immutable transaction
        Transaction tx = Transaction.createTrade(
                portfolio.getUserId(),
                portfolio.getId(),
                order.getId(),
                order.getSymbol(),
                order.getSide() == OrderSide.BUY ? Transaction.Type.BUY : Transaction.Type.SELL,
                order.getQuantity(),
                execPrice,
                fee,
                String.format("Executed %s order #%s", order.getType(), order.getId())
        );
        transactionRepository.save(tx);

        // Update order status in order repository
        orderRepository.save(order);
    }

    public List<Order> getPendingOrders(String userId) {
        return matchingEngine.getPendingOrdersForUser(userId);
    }

    public List<Order> getOrderHistory(String userId) {
        return orderRepository.findByUserId(userId);
    }
}
