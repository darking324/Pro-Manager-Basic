import java.sql.*;
import java.util.Locale;
import java.util.Scanner;

/*
 * ProjectDAO
 *
 * Responsibilities:
 * - Add new projects
 * - View all projects
 * - View projects by status (PENDING, SCHEDULED, COMPLETED, EXPIRED)
 * - View revenue history
 * - Convert SCHEDULED → COMPLETED (used when new week starts)
 */

public class ProjectDAO {

    // =====================================
    // ADD NEW PROJECT
    // =====================================
    public static void addProject(Scanner sc) {

        String title = promptTitle(sc);
        int deadline = promptPositiveInt(sc, "Enter deadline (calendar days): ");
        int revenue = promptPositiveInt(sc, "Enter revenue: ");

        String insertSql =
                "INSERT INTO projects(project_code, title, deadline, revenue, status) " +
                        "VALUES ('Proj' || LPAD(nextval('projects_id_seq')::text, 3, '0'), ?, ?, ?, ?) " +
                        "RETURNING project_code";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(insertSql)) {

            ps.setString(1, title);
            ps.setInt(2, deadline);
            ps.setInt(3, revenue);
            ps.setString(4, ProjectStatus.PENDING.name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Project added successfully.");
                    System.out.println("Assigned Project Code: " + rs.getString("project_code"));
                }
            }

        } catch (SQLException e) {
            System.out.println("Failed to add project: " + e.getMessage());
        }
    }

    // =====================================
    // VIEW ALL PROJECTS
    // =====================================
    public static void viewAllProjects() {

        String sql =
                "SELECT id, project_code, title, deadline, revenue, status " +
                        "FROM projects ORDER BY id";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            printProjectTableHeader();

            boolean empty = true;

            while (rs.next()) {
                empty = false;
                printProjectRow(rs);
            }

            if (empty) {
                System.out.println("No projects found.");
            }

            printProjectTableFooter();

        } catch (SQLException e) {
            System.out.println("Failed to fetch projects: " + e.getMessage());
        }
    }

    // =====================================
    // VIEW PROJECTS BY STATUS
    // =====================================
    public static void viewProjectsByStatus(String status) {

        if (!ProjectStatus.isValid(status)) {
            System.out.println("Invalid status filter: " + status);
            return;
        }

        String normalizedStatus = status.toUpperCase(Locale.ROOT);

        String sql =
                "SELECT id, project_code, title, deadline, revenue, status " +
                        "FROM projects WHERE status = ? ORDER BY id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ) {

            ps.setString(1, normalizedStatus);

            System.out.println("\nProjects with status: " + normalizedStatus);

            try (ResultSet rs = ps.executeQuery()) {
                printProjectTableHeader();

                boolean empty = true;

                while (rs.next()) {
                    empty = false;
                    printProjectRow(rs);
                }

                if (empty) {
                    System.out.println("No projects found.");
                }

                printProjectTableFooter();
            }

        } catch (SQLException e) {
            System.out.println("Failed to filter projects: " + e.getMessage());
        }
    }

    // =====================================
    // VIEW WEEKLY REVENUE HISTORY
    // =====================================
    public static void viewRevenueHistory() {

        String sql =
                "SELECT week_no, total_revenue, created_at " +
                        "FROM weekly_revenue_history ORDER BY week_no DESC";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\nWeekly Revenue History:");
            System.out.println("----------------------------------------------------");

            boolean empty = true;

            while (rs.next()) {
                empty = false;
                System.out.println(
                        "Week " + rs.getInt("week_no") +
                                " | Revenue: " + rs.getInt("total_revenue") +
                                " | Date: " + rs.getTimestamp("created_at")
                );
            }

            if (empty) {
                System.out.println("No revenue history found.");
            }

            System.out.println("----------------------------------------------------");

        } catch (SQLException e) {
            System.out.println("Failed to load revenue history: " + e.getMessage());
        }
    }

    // =====================================
    // CONVERT SCHEDULED → COMPLETED
    // =====================================
    public static void markScheduledAsCompleted() {

        String sql =
                "UPDATE projects SET status = 'COMPLETED' WHERE status = 'SCHEDULED'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println(rows + " project(s) marked as COMPLETED.");
            }

        } catch (SQLException e) {
            System.out.println("Failed to mark scheduled projects as completed: " + e.getMessage());
        }
    }

    // =====================================
    // HELPER METHODS (TABLE FORMATTING)
    // =====================================
    private static void printProjectTableHeader() {

        System.out.println("----------------------------------------------------------------------------");
        System.out.printf("%-5s %-10s %-20s %-10s %-10s %-12s%n",
                "ID", "ProjID", "Title", "Deadline", "Revenue", "Status");
        System.out.println("----------------------------------------------------------------------------");
    }

    private static void printProjectRow(ResultSet rs) throws SQLException {

        System.out.printf("%-5d %-10s %-20s %-10d %-10d %-12s%n",
                rs.getInt("id"),
                rs.getString("project_code"),
                rs.getString("title"),
                rs.getInt("deadline"),
                rs.getInt("revenue"),
                rs.getString("status"));
    }

    private static void printProjectTableFooter() {
        System.out.println("----------------------------------------------------------------------------");
    }

    private static String promptTitle(Scanner sc) {
        while (true) {
            System.out.print("Enter project title: ");
            String title = sc.nextLine();
            try {
                return InputValidator.requireNonBlank("title", title);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static int promptPositiveInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = sc.nextLine().trim();
            try {
                int value = Integer.parseInt(raw);
                return InputValidator.requirePositive("value", value);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}