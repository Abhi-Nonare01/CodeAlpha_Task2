package com.trading;

import com.trading.domain.*;
import com.trading.repository.*;
import com.trading.service.ExportReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PersistenceTest {

    private String testDbPath;
    private DatabaseManager dbManager;
    private UserRepository userRepository;
    private PortfolioRepository portfolioRepository;
    private TransactionRepository transactionRepository;
    private OrderRepository orderRepository;

    @BeforeEach
    public void setUp() {
        testDbPath = "test_trading_" + System.currentTimeMillis() + ".db";
        dbManager = new DatabaseManager(testDbPath);
        userRepository = new UserRepository(dbManager);
        portfolioRepository = new PortfolioRepository(dbManager);
        transactionRepository = new TransactionRepository(dbManager);
        orderRepository = new OrderRepository(dbManager);
    }

    @AfterEach
    public void tearDown() {
        try {
            File f = new File(testDbPath);
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception ignored) {}
    }

    @Test
    public void testUserAndPortfolioPersistence() {
        User user = new User("U100", "quant_guru", "Dr. Quant Guru", "quant@firm.com", "hash123", User.RiskProfile.AGGRESSIVE);
        Portfolio portfolio = new Portfolio("P100", user.getId(), "Alpha Fund", 250000.0);
        user.setDefaultPortfolioId(portfolio.getId());

        userRepository.save(user);
        portfolioRepository.save(portfolio);

        // Fetch back user
        Optional<User> fetchedUser = userRepository.findByUsername("quant_guru");
        assertTrue(fetchedUser.isPresent());
        assertEquals("Dr. Quant Guru", fetchedUser.get().getFullName());
        assertEquals(User.RiskProfile.AGGRESSIVE, fetchedUser.get().getRiskProfile());

        // Fetch back portfolio
        Optional<Portfolio> fetchedPort = portfolioRepository.findById("P100");
        assertTrue(fetchedPort.isPresent());
        assertEquals(250000.0, fetchedPort.get().getCashBalance(), 0.01);
    }

    @Test
    public void testHoldingsPersistence() {
        Portfolio portfolio = new Portfolio("P200", "U200", "Main Portfolio", 100000.0);
        Holding h = portfolio.getOrCreateHolding("NVDA");
        h.addShares(50, 120.00, 2.50);

        portfolioRepository.save(portfolio);

        // Fetch back and assert holdings
        Optional<Portfolio> fetched = portfolioRepository.findById("P200");
        assertTrue(fetched.isPresent());
        Holding loadedH = fetched.get().getHolding("NVDA");
        assertNotNull(loadedH);
        assertEquals(50, loadedH.getQuantity());
        assertEquals(120.05, loadedH.getAverageCostBasis(), 0.05);
    }

    @Test
    public void testTransactionPersistenceAndHistory() {
        Transaction tx1 = Transaction.createTrade("U300", "P300", "O300", "TSLA",
                Transaction.Type.BUY, 20, 250.00, 2.50, "Test buy order");
        transactionRepository.save(tx1);

        List<Transaction> list = transactionRepository.findByPortfolioId("P300");
        assertEquals(1, list.size());
        assertEquals("TSLA", list.get(0).getSymbol());
        assertEquals(20, list.get(0).getQuantity());
        assertEquals(5002.50, list.get(0).getTotalAmount(), 0.01);
    }

    @Test
    public void testOrderPersistence() {
        Order order = Order.createLimitOrder("U400", "P400", "MSFT", OrderSide.BUY, 15, 420.00);
        orderRepository.save(order);

        List<Order> orders = orderRepository.findByUserId("U400");
        assertEquals(1, orders.size());
        assertEquals("MSFT", orders.get(0).getSymbol());
        assertEquals(OrderStatus.PENDING, orders.get(0).getStatus());

        // Update status to FILLED
        order.fill(419.50, 15);
        orderRepository.save(order);

        List<Order> updated = orderRepository.findByUserId("U400");
        assertEquals(OrderStatus.FILLED, updated.get(0).getStatus());
        assertEquals(419.50, updated.get(0).getExecutionPrice(), 0.01);
    }

    @Test
    public void testExportReportsToCSVAndJSON() throws IOException {
        ExportReportService exportService = new ExportReportService();

        Portfolio portfolio = new Portfolio("P500", "U500", "Export Portfolio", 100000.0);
        Holding h = portfolio.getOrCreateHolding("AAPL");
        h.addShares(10, 150.0, 1.0);

        List<Transaction> txs = List.of(
                Transaction.createTrade("U500", "P500", "O1", "AAPL", Transaction.Type.BUY, 10, 150.0, 1.0, "Test")
        );

        Stock stock = new Stock("AAPL", "Apple Inc.", "Tech", 155.0, 0.2, 0.08, 0.5, 2500.0);
        Map<String, Stock> market = Map.of("AAPL", stock);

        PerformanceMetrics metrics = PerformanceMetrics.calculate(portfolio, market, txs);

        String csvPath = "test_export_" + System.currentTimeMillis() + ".csv";
        String jsonPath = "test_export_" + System.currentTimeMillis() + ".json";

        File csvFile = exportService.exportTransactionsToCSV(txs, csvPath);
        File jsonFile = exportService.exportPortfolioStatementToJSON(portfolio, market, metrics, txs, jsonPath);

        assertTrue(csvFile.exists() && csvFile.length() > 0);
        assertTrue(jsonFile.exists() && jsonFile.length() > 0);

        String jsonContent = Files.readString(jsonFile.toPath());
        assertTrue(jsonContent.contains("AAPL"));
        assertTrue(jsonContent.contains("performanceMetrics"));

        csvFile.delete();
        jsonFile.delete();
    }
}
