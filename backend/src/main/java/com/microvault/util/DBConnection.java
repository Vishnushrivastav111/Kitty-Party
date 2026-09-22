package com.microvault.util;

import com.microvault.config.DatabaseConfig;
import com.microvault.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Hands out PostgreSQL connections. Every DAO implementation calls
 * DBConnection.getConnection() so the credentials stay in one place
 * (see {@link DatabaseConfig}).
 */
public class DBConnection {

    private DBConnection() {
    }

    /**
     * Opens a new JDBC connection. The caller closes it, normally with
     * try-with-resources.
     */
    public static Connection getConnection() {
        try {
            Class.forName(DatabaseConfig.getDriverClass());
            DriverManager.setLoginTimeout(5);
            return DriverManager.getConnection(
                    DatabaseConfig.getUrl(),
                    DatabaseConfig.getUsername(),
                    DatabaseConfig.getPassword());
        } catch (ClassNotFoundException exception) {
            throw new DatabaseException(
                    "PostgreSQL JDBC driver not found: " + DatabaseConfig.getDriverClass(), exception);
        } catch (SQLException exception) {
            throw new DatabaseException(
                    "Unable to connect to PostgreSQL at " + DatabaseConfig.getUrl(), exception);
        }
    }

    /**
     * Returns true when the database can be reached. Tests use this so they can
     * be skipped instead of failing when PostgreSQL is not available.
     */
    public static boolean isAvailable() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (Exception exception) {
            return false;
        }
    }
}
