package com.trading.service;

import com.trading.domain.MarketNews;
import com.trading.domain.Stock;
import com.trading.engine.MarketSimulationEngine;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for querying market quotes, gainers, losers, sector performance, and news feed.
 */
public class MarketDataService {
    private final MarketSimulationEngine simulationEngine;

    public MarketDataService(MarketSimulationEngine simulationEngine) {
        this.simulationEngine = simulationEngine;
    }

    public Stock getStock(String symbol) {
        return simulationEngine.getStock(symbol);
    }

    public List<Stock> getAllStocks() {
        return new ArrayList<>(simulationEngine.getAllStocks().values());
    }

    public List<Stock> getTopGainers(int limit) {
        return simulationEngine.getAllStocks().values().stream()
                .sorted(Comparator.comparingDouble(Stock::getChangePercent).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Stock> getTopLosers(int limit) {
        return simulationEngine.getAllStocks().values().stream()
                .sorted(Comparator.comparingDouble(Stock::getChangePercent))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Stock> getMostActive(int limit) {
        return simulationEngine.getAllStocks().values().stream()
                .sorted(Comparator.comparingLong(Stock::getVolume).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Map<String, Double> getSectorPerformance() {
        Map<String, List<Double>> sectorReturns = new HashMap<>();
        for (Stock s : simulationEngine.getAllStocks().values()) {
            sectorReturns.computeIfAbsent(s.getSector(), k -> new ArrayList<>()).add(s.getChangePercent());
        }

        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : sectorReturns.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            result.put(entry.getKey(), Math.round(avg * 100.0) / 100.0);
        }
        return result;
    }

    public List<MarketNews> getLatestNews() {
        return simulationEngine.getRecentNews();
    }

    public void triggerNewsEvent(MarketNews news) {
        simulationEngine.publishNews(news);
    }

    public void triggerRandomNews() {
        simulationEngine.publishRandomNews();
    }
}
