import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    public static Connection getConnection() {

        try {
            return DriverManager.getConnection(
                    DBConfig.getJdbcUrl(),
                    DBConfig.getUsername(),
                    DBConfig.getPassword()
            );
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Database connection failed: " + e.getMessage(), e);
        }
    }
}