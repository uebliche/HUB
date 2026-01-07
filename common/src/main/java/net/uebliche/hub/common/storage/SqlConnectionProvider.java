package net.uebliche.hub.common.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

final class SqlConnectionProvider {
    private final StorageSettings.SqlSettings settings;

    SqlConnectionProvider(StorageSettings.SqlSettings settings) {
        this.settings = settings;
        String driver = settings.driver();
        if (driver != null && !driver.isBlank()) {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException ignored) {
                // handled on connect
            }
        }
    }

    Connection open() throws SQLException {
        String url = settings.url();
        if (url == null || url.isBlank()) {
            throw new SQLException("SQL url is not configured");
        }
        String user = settings.user();
        if (user == null || user.isBlank()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, user, settings.password());
    }
}
