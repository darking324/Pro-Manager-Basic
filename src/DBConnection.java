import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/promanage",
                    "postgres",
                    "password"
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
