package me.minseok.shopsystem.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseManager {

    private final Logger logger;
    private HikariDataSource dataSource;

    public DatabaseManager(String host, int port, String database,
            String username, String password, Logger logger) {
        this.logger = logger;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);

        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // Performance settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        this.dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void initialize() {
        try (Connection conn = getConnection()) {
            // Create player_balances table
            executeUpdate(conn, """
                        CREATE TABLE IF NOT EXISTS player_balances (
                            uuid VARCHAR(36) PRIMARY KEY,
                            balance DECIMAL(15,2) DEFAULT 0.00,
                            last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                    """);

            // Create transactions table
            executeUpdate(conn, """
                        CREATE TABLE IF NOT EXISTS transactions (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            uuid VARCHAR(36) NOT NULL,
                            type ENUM('DEPOSIT', 'WITHDRAW', 'PURCHASE', 'SALE', 'TRANSFER') NOT NULL,
                            amount DECIMAL(15,2) NOT NULL,
                            description VARCHAR(255),
                            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            INDEX idx_uuid (uuid),
                            INDEX idx_timestamp (timestamp)
                        )
                    """);

            // Create price_data table for dynamic pricing
            executeUpdate(conn, """
                        CREATE TABLE IF NOT EXISTS price_data (
                            item_id VARCHAR(64) PRIMARY KEY,
                            base_price DECIMAL(15,2) NOT NULL,
                            current_price DECIMAL(15,2) NOT NULL,
                            sell_price DECIMAL(15,2) NOT NULL,
                            transaction_count INT DEFAULT 0,
                            last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                    """);

            // Create price_history table
            executeUpdate(conn, """
                        CREATE TABLE IF NOT EXISTS price_history (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            item_id VARCHAR(64) NOT NULL,
                            price DECIMAL(15,2) NOT NULL,
                            changed_by VARCHAR(36),
                            reason VARCHAR(100),
                            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            INDEX idx_item (item_id),
                            INDEX idx_timestamp (timestamp)
                        )
                    """);

            // Create player_purchases table for one-time items
            executeUpdate(conn, """
                        CREATE TABLE IF NOT EXISTS player_purchases (
                            uuid VARCHAR(36) NOT NULL,
                            item_id VARCHAR(64) NOT NULL,
                            purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (uuid, item_id)
                        )
                    """);

            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void executeUpdate(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection closed");
        }
    }

    public boolean hasPurchased(java.util.UUID uuid, String itemId) {
        String sql = "SELECT 1 FROM player_purchases WHERE uuid = ? AND item_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, itemId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.severe("Failed to check purchase status: " + e.getMessage());
            return false;
        }
    }

    public void recordPurchase(java.util.UUID uuid, String itemId) {
        String sql = "INSERT IGNORE INTO player_purchases (uuid, item_id) VALUES (?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to record purchase: " + e.getMessage());
        }
    }
}
