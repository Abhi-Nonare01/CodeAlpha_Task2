package com.trading.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite embedded database connections, connection pooling,
 * and automated schema initializations.
 */
public class DatabaseManager {
    private static final String DEFAULT_DB_PATH = "trading_platform.db";
    private final String dbUrl;

    public DatabaseManager() {
        this(DEFAULT_DB_PATH);
    }

    public DatabaseManager(String dbFilePath) {
        this.dbUrl = "jdbc:sqlite:" + dbFilePath;
        initializeSchema();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private void initializeSchema() {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                full_name TEXT NOT NULL,
                email TEXT,
                password_hash TEXT NOT NULL,
                default_portfolio_id TEXT,
                risk_profile TEXT,
                created_at TEXT NOT NULL
            );
        """;

        String createPortfoliosTable = """
            CREATE TABLE IF NOT EXISTS portfolios (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                name TEXT NOT NULL,
                cash_balance REAL NOT NULL,
                initial_cash REAL NOT NULL,
                created_at TEXT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );
        """;

        String createHoldingsTable = """
            CREATE TABLE IF NOT EXISTS holdings (
                portfolio_id TEXT NOT NULL,
                symbol TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                total_cost_basis REAL NOT NULL,
                realized_pnl REAL NOT NULL,
                dividends_received REAL NOT NULL,
                PRIMARY KEY (portfolio_id, symbol),
                FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE
            );
        """;

        String createTransactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                portfolio_id TEXT NOT NULL,
                order_id TEXT,
                symbol TEXT NOT NULL,
                type TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                price REAL NOT NULL,
                total_amount REAL NOT NULL,
                fee REAL NOT NULL,
                timestamp TEXT NOT NULL,
                notes TEXT
            );
        """;

        String createOrdersTable = """
            CREATE TABLE IF NOT EXISTS orders (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                portfolio_id TEXT NOT NULL,
                symbol TEXT NOT NULL,
                side TEXT NOT NULL,
                type TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                filled_quantity INTEGER NOT NULL,
                limit_price REAL,
                stop_price REAL,
                trailing_percent REAL,
                status TEXT NOT NULL,
                created_at TEXT NOT NULL,
                filled_at TEXT,
                execution_price REAL,
                notes TEXT
            );
        """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute(createUsersTable);
            stmt.execute(createPortfoliosTable);
            stmt.execute(createHoldingsTable);
            stmt.execute(createTransactionsTable);
            stmt.execute(createOrdersTable);
        } catch (SQLException e) {
            System.err.println("Fatal: Failed to initialize SQLite schema: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }
}
