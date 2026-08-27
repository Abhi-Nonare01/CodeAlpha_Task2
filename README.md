# QuantumTrade Pro: Enterprise Stock Trading Platform

A modern, institutional-grade **Stock Trading & Portfolio Simulation Engine** developed in **Java 17**. Built from the ground up to showcase advanced Object-Oriented Programming (OOP) design patterns, real-time stochastic market simulation (Geometric Brownian Motion + Dynamic Macro Shocks), high-performance order matching (Market, Limit, Stop-Loss, Trailing Stop), embedded SQLite ACID persistence, and dual interfaces: an interactive ANSI Terminal Console and a dark-theme TradingView-style Web Dashboard.

---

## Key Highlights & Architecture

### 1. Stochastic Market Simulation Engine
* **Geometric Brownian Motion (GBM) with Jump Diffusion**: Simulates authentic intraday asset price dynamics using standard drift ($\mu$) and annualized volatility ($\sigma$):
  $$S_{t+\Delta t} = S_t \exp\left( (\mu - 0.5\sigma^2)\Delta t + \sigma \sqrt{\Delta t} Z \right)$$
* **Dynamic Macroeconomic & Corporate News Shock Engine**: Injects real-time news events (Fed interest rate signals, tech earnings beats, FDA approvals, OPEC output decisions) that dynamically shock asset prices and adjust sector volatility.
* **Variable Simulation Speeds & Dividend Yield Cycles**: Configurable tick rates (1x, 2x, 5x, 10x, Pause) with periodic simulated dividend distributions.

### 2. High-Performance Order Execution & Matching Engine
* **Supported Order Types**:
  * `MARKET`: Instant fill at best available bid/ask spread.
  * `LIMIT`: Executes when marketable price threshold is reached or better.
  * `STOP_LOSS`: Protection triggers market execution upon breaching stop price.
  * `TRAILING_STOP`: Dynamically trails peak asset price by a designated percentage.
* **Execution Realism**: Microstructure modeling with bid-ask spreads, transaction commissions (0.05% with $1.00 minimum), and cash/position balance validations.

### 3. Institutional Portfolio Analytics
* **Weighted Average Cost Basis Accounting**: Real-time tracking of open share inventory, realized P&L on sales, and unrealized mark-to-market P&L.
* **Quantitative Risk Metrics**:
  * **Sharpe Ratio**: Annualized risk-adjusted excess return over historical periodic volatility.
  * **Maximum Drawdown (MDD)**: Peak-to-trough decline percentage across portfolio equity curve.
  * **Win Rate & Profit Factor**: Gross profits divided by gross losses across all closed positions.

### 4. Enterprise Persistence & Data Export (Repository Pattern)
* **Embedded SQLite Database**: ACID transactional persistence for users, portfolios, open/closed holdings, active limit orders, and transactions (`trading_platform.db`).
* **Automated DDL Schema Migration**: Automatic schema initialization and seed accounts on launch.
* **Data Exporters**: Instant export of portfolio statements and audit trails to **CSV** and **JSON** formats.

### 5. Dual Interfaces (Terminal & Web Dashboard)
* **Interactive ANSI Terminal Console**: Colorized ticker tape, sparklines (` ▂▃▅▆█`), multi-row ASCII candlestick/price charts, interactive command menus.
* **Embedded Web Trading Dashboard (`http://localhost:8080`)**: Zero-dependency embedded Java HTTP server serving a modern TradingView/Bloomberg-style dark-theme SPA with **Chart.js** price charts, live order placement terminal, asset allocation doughnut chart, and live transaction ledger.

---

## Project Structure

```
stock-trading-platform/
├── pom.xml                               # Maven Build Configuration
├── trading_platform.db                   # SQLite Embedded Database (auto-generated)
├── src/
│   ├── main/
│   │   ├── java/com/trading/
│   │   │   ├── App.java                  # Main Application Entrypoint
│   │   │   ├── domain/                   # Rich Domain OOP Models
│   │   │   │   ├── Stock.java            # Stock pricing, sparkline ticks, statistics
│   │   │   │   ├── Order.java            # Market, Limit, Stop, Trailing orders
│   │   │   │   ├── OrderSide.java        # BUY, SELL
│   │   │   │   ├── OrderStatus.java      # PENDING, FILLED, CANCELLED, REJECTED
│   │   │   │   ├── OrderType.java        # MARKET, LIMIT, STOP_LOSS, TRAILING_STOP
│   │   │   │   ├── Holding.java          # Cost basis & position P&L
│   │   │   │   ├── Portfolio.java        # Cash, holdings, equity snapshots
│   │   │   │   ├── User.java             # User account & risk profile
│   │   │   │   ├── Transaction.java      # Immutable audit log entry
│   │   │   │   ├── MarketNews.java       # News event & sentiment model
│   │   │   │   └── PerformanceMetrics.java # Sharpe ratio, drawdown, win rate
│   │   │   ├── engine/                   # Simulation & Matching Engine
│   │   │   │   ├── MarketSimulationEngine.java # Background ticker & news dispatcher
│   │   │   │   ├── OrderMatchingEngine.java    # Tick-based order matching
│   │   │   │   ├── PricingStrategy.java        # Geometric Brownian Motion
│   │   │   │   └── NewsEventGenerator.java     # Macro & company news catalog
│   │   │   ├── repository/               # Data Access Layer (DAO Pattern)
│   │   │   │   ├── DatabaseManager.java        # SQLite connection & schema DDL
│   │   │   │   ├── UserRepository.java         # User CRUD
│   │   │   │   ├── PortfolioRepository.java    # Portfolio & Holdings CRUD
│   │   │   │   ├── TransactionRepository.java  # Transaction audit logs
│   │   │   │   └── OrderRepository.java        # Orders persistence
│   │   │   ├── service/                  # Business Logic Layer
│   │   │   │   ├── AuthService.java            # User auth & session
│   │   │   │   ├── MarketDataService.java      # Quotes, gainers/losers, sectors
│   │   │   │   ├── PortfolioService.java       # Valuation, deposits, dividends
│   │   │   │   ├── TradingService.java         # Trade execution orchestration
│   │   │   │   └── ExportReportService.java    # CSV and JSON report exports
│   │   │   └── ui/
│   │   │       ├── cli/                        # Interactive ANSI Terminal UI
│   │   │       │   ├── AnsiConsole.java        # ANSI styling & colorizer
│   │   │       │   ├── AsciiChart.java         # ASCII sparklines & line charts
│   │   │       │   └── TerminalDashboard.java  # Menu navigation & user commands
│   │   │       └── web/                        # Modern Web UI & Server
│   │   │           └── TradingWebServer.java   # Embedded HTTP server & REST APIs
│   │   └── resources/
│   │       └── web/
│   │           └── index.html                  # Single Page Web App (Tailwind + Chart.js)
│   └── test/
│       └── java/com/trading/
│           ├── TradingEngineTest.java          # Order matching & domain tests
│           └── PersistenceTest.java            # SQLite & export service tests
```

---

## Quick Start & Running

### Prerequisites
* **Java 17+** (JDK 17 LTS or higher)
* **Maven 3.8+**

### 1. Build and Run Tests
```bash
mvn clean test
```

### 2. Run the Platform
You can run directly using Maven:
```bash
mvn exec:java
```
Or run the shaded standalone JAR:
```bash
java -jar target/stock-trading-platform-1.0.0.jar
```

### 3. Accessing the Platform
* **Web Trading Dashboard**: Open your browser at [http://localhost:8080](http://localhost:8080)
* **Interactive CLI**: Use the terminal console menu to view live quotes, place orders, examine ASCII charts, and manage your portfolio.

---

## Design Patterns & OOP Principles Applied

1. **Strategy Pattern** (`PricingStrategy`): Decouples price computation logic from the simulation engine.
2. **Observer / Listener Pattern** (`OrderExecutionListener`, `StockTickListener`, `NewsListener`): Decouples matching engine and UI from market tick generators.
3. **Repository / DAO Pattern** (`UserRepository`, `PortfolioRepository`, `TransactionRepository`): Encapsulates SQLite database operations behind clean domain interfaces.
4. **Factory & Builder Patterns** (`Order.createLimitOrder()`, `Transaction.createTrade()`): Guarantees valid creation of complex domain entities.
5. **Encapsulation & Immutability**: All transactions and historical price points are immutable; domain invariants (such as non-negative balances and valid cost basis) are enforced strictly within domain models.
