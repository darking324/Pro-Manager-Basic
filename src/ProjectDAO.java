import java.sql.*;
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
    public static void addProject() {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter project title: ");
        String title = sc.nextLine();

        System.out.print("Enter deadline (calendar days): ");
        int deadline = sc.nextInt();

        System.out.print("Enter revenue: ");
        int revenue = sc.nextInt();

        String getSeqSql = "SELECT nextval('projects_id_seq')";
        String insertSql =
                "INSERT INTO projects(project_code, title, deadline, revenue, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             Statement seqStmt = con.createStatement();
             ResultSet rs = seqStmt.executeQuery(getSeqSql)) {

            rs.next();
            int seqValue = rs.getInt(1);

            String projectCode = String.format("Proj%03d", seqValue);

            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                ps.setString(1, projectCode);
                ps.setString(2, title);
                ps.setInt(3, deadline);
                ps.setInt(4, revenue);
                ps.setString(5, "PENDING");
                ps.executeUpdate();
            }

            System.out.println("Project added successfully.");
            System.out.println("Assigned Project Code: " + projectCode);

        } catch (Exception e) {
            e.printStackTrace();
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =====================================
    // VIEW PROJECTS BY STATUS
    // =====================================
    public static void viewProjectsByStatus(String status) {

        String sql =
                "SELECT id, project_code, title, deadline, revenue, status " +
                        "FROM projects WHERE status = ? ORDER BY id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();

            System.out.println("\nProjects with status: " + status);

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

        } catch (Exception e) {
            e.printStackTrace();
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

        } catch (Exception e) {
            e.printStackTrace();
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

        } catch (Exception e) {
            e.printStackTrace();
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
}