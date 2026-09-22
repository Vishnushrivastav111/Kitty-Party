package com.microvault.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * The single place where the PostgreSQL connection details live.
 *
 * Values are read from src/main/resources/database.properties.
 * Environment variables (DB_URL, DB_USERNAME, DB_PASSWORD) win over the file,
 * so a developer can point the backend at another database without editing code.
 */
public class DatabaseConfig {

    private static final String PROPERTIES_FILE = "database.properties";

    private static final String url;
    private static final String username;
    private static final String password;
    private static final String driverClass;

    static {
        Properties properties = loadProperties();

        url = readValue("DB_URL", properties, "db.url",
                "jdbc:postgresql://10.23.240.38:5432/indr_aug13_smartsavingsandinvestment_dev?connectTimeout=5");
        username = readValue("DB_USERNAME", properties, "db.username",
                "indr_aug13_smartsavingsandinvestment_dev");
        password = readValue("DB_PASSWORD", properties, "db.password", "TCS@123");
        driverClass = readValue("DB_DRIVER", properties, "db.driver", "org.postgresql.Driver");
    }

    /** Utility style class: no instances. */
    private DatabaseConfig() {
    }

    public static String getUrl() {
        return url;
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        return password;
    }

    public static String getDriverClass() {
        return driverClass;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream stream = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (Exception exception) {
            // The built-in defaults below are used when the file cannot be read.
            System.out.println("Could not read " + PROPERTIES_FILE + ", using default settings.");
        }
        return properties;
    }

    private static String readValue(String environmentKey, Properties properties,
                                    String propertyKey, String defaultValue) {

        String fromEnvironment = System.getenv(environmentKey);
        if (fromEnvironment != null && !fromEnvironment.trim().isEmpty()) {
            return fromEnvironment.trim();
        }

        String fromFile = properties.getProperty(propertyKey);
        if (fromFile != null && !fromFile.trim().isEmpty()) {
            return fromFile.trim();
        }

        return defaultValue;
    }
}
