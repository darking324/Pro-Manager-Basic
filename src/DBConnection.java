import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/promanage";

    private static final String USER = "postgres";
    private static final String PASSWORD = "password";

    public static Connection getConnection() {

        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Database connection failed: " + e.getMessage(), e);
        }
    }
}