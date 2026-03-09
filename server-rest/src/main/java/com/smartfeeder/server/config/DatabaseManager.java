package com.smartfeeder.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manager per il pool di connessioni MySQL tramite HikariCP.
 */
public class DatabaseManager {

    private static HikariDataSource dataSource;

    public static void initialize() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getEnvOrDefault("DB_URL", "jdbc:mysql://localhost:3306/SmartFeederDB"));
        config.setUsername(getEnvOrDefault("DB_USER", "smartfeeder"));
        config.setPassword(getEnvOrDefault("DB_PASSWORD", "zLfVSEAz8rKaUi"));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");

        dataSource = new HikariDataSource(config);
        System.out.println("[DatabaseManager] Pool connessioni MySQL inizializzato.");
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isEmpty()) ? val : defaultValue;
    }
}
