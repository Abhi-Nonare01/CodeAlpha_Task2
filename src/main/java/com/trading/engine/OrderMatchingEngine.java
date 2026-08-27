package com.trading.engine;

import com.trading.domain.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * High-performance matching engine that monitors live market ticks and executes
 * Market, Limit, Stop-Loss, and Trailing-Stop orders when market conditions are satisfied.
 */
public class OrderMatchingEngine {

    @FunctionalInterface
    public interface OrderExecutionListener {
        void onOrderExecuted(Order order, double executionPrice, double fee);
    }

    private final List<Order> pendingOrders = new CopyOnWriteArrayList<>();
    private final List<Order> orderHistory = new CopyOnWriteArrayList<>();
    private final List<OrderExecutionListener> listeners = new CopyOnWriteArrayList<>();

    // Fee structure: 0.05% commission with a minimum of $1.00
    private static final double COMMISSION_RATE = 0.0005;
    private static final double MIN_COMMISSION = 1.00;

    public void addListener(OrderExecutionListener listener) {
        listeners.add(listener);
    }

    public synchronized void submitOrder(Order order, Stock currentStock) {
        if (order.getType() == OrderType.MARKET) {
            executeMarketOrder(order, currentStock);
        } else {
            pendingOrders.add(order);
            // Check immediately in case limit price is already marketable
            evaluateOrderAgainstStock(order, currentStock);
        }
    }

    public synchronized boolean cancelOrder(String orderId, String reason) {
        for (Order order : pendingOrders) {
            if (order.getId().equalsIgnoreCase(orderId)) {
                order.cancel(reason != null ? reason : "Cancelled by user");
                pendingOrders.remove(order);
                orderHistory.add(order);
                return true;
            }
        }
        return false;
    }

    public synchronized void processStockTick(Stock stock) {
        List<Order> toExecute = new ArrayList<>();

        for (Order order : pendingOrders) {
            if (!order.getSymbol().equalsIgnoreCase(stock.getSymbol())) {
                continue;
            }

            if (evaluateOrderAgainstStock(order, stock)) {
                toExecute.add(order);
            }
        }

        for (Order order : toExecute) {
            pendingOrders.remove(order);
            double execPrice = (order.getSide() == OrderSide.BUY) ? stock.getAskPrice() : stock.getBidPrice();
            executeOrder(order, execPrice);
        }
    }

    private boolean evaluateOrderAgainstStock(Order order, Stock stock) {
        double currentPrice = stock.getCurrentPrice();
        double bid = stock.getBidPrice();
        double ask = stock.getAskPrice();

        switch (order.getType()) {
            case LIMIT:
                if (order.getSide() == OrderSide.BUY && ask <= order.getLimitPrice()) {
                    return true;
                } else if (order.getSide() == OrderSide.SELL && bid >= order.getLimitPrice()) {
                    return true;
                }
                break;

            case STOP_LOSS:
                if (order.getSide() == OrderSide.SELL && bid <= order.getStopPrice()) {
                    return true;
                } else if (order.getSide() == OrderSide.BUY && ask >= order.getStopPrice()) {
                    return true;
                }
                break;

            case TRAILING_STOP:
                order.updateTrailingHighWaterMark(currentPrice);
                double effectiveStop = order.getEffectiveTrailingStopPrice();
                if (order.getSide() == OrderSide.SELL && bid <= effectiveStop) {
                    return true;
                } else if (order.getSide() == OrderSide.BUY && ask >= effectiveStop) {
                    return true;
                }
                break;

            default:
                break;
        }
        return false;
    }

    private void executeMarketOrder(Order order, Stock stock) {
        double execPrice = (order.getSide() == OrderSide.BUY) ? stock.getAskPrice() : stock.getBidPrice();
        executeOrder(order, execPrice);
    }

    private void executeOrder(Order order, double execPrice) {
        double notional = order.getQuantity() * execPrice;
        double fee = Math.max(MIN_COMMISSION, notional * COMMISSION_RATE);
        fee = Math.round(fee * 100.0) / 100.0;

        order.fill(execPrice, order.getQuantity());
        orderHistory.add(order);

        for (OrderExecutionListener listener : listeners) {
            try {
                listener.onOrderExecuted(order, execPrice, fee);
            } catch (Exception e) {
                System.err.println("Error notifying order listener: " + e.getMessage());
            }
        }
    }

    public List<Order> getPendingOrders() {
        return Collections.unmodifiableList(new ArrayList<>(pendingOrders));
    }

    public List<Order> getPendingOrdersForUser(String userId) {
        List<Order> list = new ArrayList<>();
        for (Order o : pendingOrders) {
            if (o.getUserId().equals(userId)) {
                list.add(o);
            }
        }
        return list;
    }

    public List<Order> getOrderHistory() {
        return Collections.unmodifiableList(new ArrayList<>(orderHistory));
    }

    public List<Order> getOrderHistoryForUser(String userId) {
        List<Order> list = new ArrayList<>();
        for (Order o : orderHistory) {
            if (o.getUserId().equals(userId)) {
                list.add(o);
            }
        }
        return list;
    }
}
