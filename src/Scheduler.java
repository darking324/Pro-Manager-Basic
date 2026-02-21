import java.sql.*;
import java.util.*;

public class Scheduler {

    // Internal lightweight model for scheduling
    static class Project {
        int id;
        String title;
        int deadline;   // original deadline
        int revenue;
        double score;

        Project(int id, String title, int deadline, int revenue) {
            this.id = id;
            this.title = title;
            this.deadline = deadline;
            this.revenue = revenue;
        }
    }

    public static void generateSchedule() {

        List<Project> projects = new ArrayList<>();
        double expectedRevenue = predictNextWeekRevenue();

        // Fetch only required fields
        String fetchSql =
                "SELECT id, title, deadline, revenue " +
                        "FROM projects WHERE status = 'PENDING'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(fetchSql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                int deadline = rs.getInt("deadline");
                if (deadline <= 0) continue;

                projects.add(new Project(
                        rs.getInt("id"),
                        rs.getString("title"),
                        deadline,
                        rs.getInt("revenue")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (projects.isEmpty()) {
            System.out.println("No schedulable projects.");
            return;
        }

        // =============================
        // Predictive Scoring Model
        // =============================
        for (Project p : projects) {

            double urgency = 1.0 / Math.max(p.deadline, 1);

            int futurePenalty = 0;
            if (p.deadline > 5 && p.revenue < expectedRevenue) {
                futurePenalty = 1;
            }

            // Balanced scoring formula
            p.score =
                    (0.5 * p.revenue)
                            + (0.3 * urgency * 5000)
                            - (0.2 * futurePenalty * p.revenue);
        }

        // Sort by score descending
        projects.sort((a, b) -> Double.compare(b.score, a.score));

        // =============================
        // Greedy Deadline Allocation
        // =============================
        Project[] week = new Project[5];
        int totalRevenue = 0;

        List<Integer> scheduledIds = new ArrayList<>();

        for (Project p : projects) {

            int lastDay = Math.min(p.deadline, 5) - 1;

            for (int d = lastDay; d >= 0; d--) {
                if (week[d] == null) {
                    week[d] = p;
                    totalRevenue += p.revenue;
                    scheduledIds.add(p.id);
                    break;
                }
            }
        }

        // Batch update scheduled projects
        updateProjectStatusBatch(scheduledIds);

        // Store revenue history
        storeWeeklyRevenue(totalRevenue);

        // =============================
        // Display Schedule
        // =============================
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

        System.out.println("\n=========================================");
        System.out.println("Expected Next Week Revenue: " + expectedRevenue);
        System.out.println("=========================================");

        System.out.println("\nWeekly Schedule:");

        for (int i = 0; i < 5; i++) {
            System.out.println(days[i] + " : " +
                    (week[i] != null ? week[i].title : "No Project"));
        }

        System.out.println("\nTotal Revenue: " + totalRevenue);
        System.out.println("=========================================");
    }

    // =============================
    // Revenue Prediction (WMA)
    // =============================
    private static double predictNextWeekRevenue() {

        List<Integer> revenues = new ArrayList<>();

        String sql =
                "SELECT total_revenue FROM weekly_revenue_history " +
                        "ORDER BY week_no DESC LIMIT 3";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                revenues.add(rs.getInt("total_revenue"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (revenues.isEmpty())
            return 0;

        if (revenues.size() == 1)
            return revenues.get(0);

        if (revenues.size() == 2)
            return (revenues.get(0) + revenues.get(1)) / 2.0;

        return 0.5 * revenues.get(0)
                + 0.3 * revenues.get(1)
                + 0.2 * revenues.get(2);
    }

    // =============================
    // Batch Status Update
    // =============================
    private static void updateProjectStatusBatch(List<Integer> ids) {

        if (ids.isEmpty()) return;

        String sql =
                "UPDATE projects SET status = 'SCHEDULED' WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (int id : ids) {
                ps.setInt(1, id);
                ps.addBatch();
            }

            ps.executeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =============================
    // Store Weekly Revenue
    // =============================
    private static void storeWeeklyRevenue(int revenue) {

        String sql =
                "INSERT INTO weekly_revenue_history(total_revenue) VALUES (?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, revenue);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}