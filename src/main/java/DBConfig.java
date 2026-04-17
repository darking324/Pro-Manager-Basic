import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/*
 * DBConfig resolves database configuration from:
 * 1) Environment variables
 * 2) application.properties
 * 3) Safe defaults
 */
public final class DBConfig {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/promanage";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "";
    private static final Properties PROPERTIES = loadProperties();

    private DBConfig() {
    }

    public static String getJdbcUrl() {
        return resolveValue("db.url", "PROMANAGE_DB_URL", DEFAULT_URL);
    }

    public static String getUsername() {
        return resolveValue("db.user", "PROMANAGE_DB_USER", DEFAULT_USER);
    }

    public static String getPassword() {
        return resolveValue("db.password", "PROMANAGE_DB_PASSWORD", DEFAULT_PASSWORD);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream in = DBConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException ignored) {
            // Fall back to defaults and environment variables.
        }

        return properties;
    }

    private static String resolveValue(String propertyKey, String envKey, String fallback) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        String property = PROPERTIES.getProperty(propertyKey);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }

        return fallback;
    }
}
