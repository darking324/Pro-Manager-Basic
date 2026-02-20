import java.sql.*;
import java.util.Scanner;

public class ProjectDAO {

    public static void addProject() {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter project title: ");
        String title = sc.nextLine();

        System.out.print("Enter deadline (in calendar days): ");
        int deadline = sc.nextInt();

        System.out.print("Enter revenue: ");
        int revenue = sc.nextInt();

        String getSeqSql = "SELECT nextval('project_id_seq')";
        String insertSql =
                "INSERT INTO projects(project_code, title, deadline, revenue) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             Statement seqStmt = con.createStatement();
             ResultSet rs = seqStmt.executeQuery(getSeqSql)) {

            // Generate next sequence value
            rs.next();
            int seqValue = rs.getInt(1);

            // Format: Proj001, Proj002, ...
            String projectCode = String.format("Proj%03d", seqValue);

            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                ps.setString(1, projectCode);
                ps.setString(2, title);
                ps.setInt(3, deadline);
                ps.setInt(4, revenue);
                ps.executeUpdate();
            }

            System.out.println("Project added successfully");
            System.out.println("Assigned Project ID: " + projectCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void viewAllProjects() {

        String sql =
                "SELECT id, project_code, title, deadline, revenue " +
                        "FROM projects ORDER BY id";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\nExisting Projects:");
            System.out.println("-------------------------------------------------------------------");
            System.out.printf("%-5s %-10s %-20s %-10s %-10s%n",
                    "ID", "ProjID", "Title", "Deadline", "Revenue");
            System.out.println("-------------------------------------------------------------------");

            boolean empty = true;

            while (rs.next()) {
                empty = false;
                System.out.printf("%-5d %-10s %-20s %-10d %-10d%n",
                        rs.getInt("id"),
                        rs.getString("project_code"),
                        rs.getString("title"),
                        rs.getInt("deadline"),
                        rs.getInt("revenue"));
            }

            if (empty) {
                System.out.println("No projects found.");
            }

            System.out.println("-------------------------------------------------------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}