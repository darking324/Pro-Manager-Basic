import java.sql.*;
import java.util.*;

/*
 * Scheduler Class
 *
 * Responsibilities:
 * - Convert last week's SCHEDULED → COMPLETED
 * - Reduce deadlines weekly
 * - Apply predictive scoring
 * - Perform greedy scheduling
 * - Expire rejected urgent projects
 * - Store weekly revenue
 */

public class Scheduler {

    // ===============================
    // Internal Lightweight Project Model
    // ===============================
    static class Project {
        int id;
        String title;
        int deadline;
        int revenue;
        double score;

        Project(int id, String title, int deadline, int revenue) {
            this.id = id;
            this.title = title;
            this.deadline = deadline;
            this.revenue = revenue;
        }
    }

    // ===============================
    // MAIN SCHEDULER METHOD
    // ===============================
    public static void generateSchedule() {

        // STEP 0: Convert last week SCHEDULED → COMPLETED
        markScheduledAsCompleted();

        List<Project> projects = new ArrayList<>();
        double expectedRevenue = predictNextWeekRevenue();

        // STEP 2: Fetch PENDING projects
        String fetchSql =
                "SELECT id, title, deadline, revenue FROM projects WHERE status = 'PENDING'";

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

        // STEP 3: Predictive Scoring
        for (Project p : projects) {

            double urgency = 1.0 / Math.max(p.deadline, 1);
            int futurePenalty = 0;

            // Penalize low-value long-deadline projects slightly
            if (p.deadline > 5 && p.revenue < expectedRevenue * 0.5) {
                futurePenalty = 1;
            }

            p.score =
                    (0.5 * p.revenue)
                            + (0.3 * urgency * 5000)
                            - (0.2 * futurePenalty * p.revenue);
        }

        // Sort by score descending
        projects.sort((a, b) -> Double.compare(b.score, a.score));

        // STEP 4: Greedy Scheduling
        Project[] week = new Project[5];
        int totalRevenue = 0;

        List<Integer> scheduledIds = new ArrayList<>();
        List<Project> rejectedProjects = new ArrayList<>();

        for (Project p : projects) {

            int lastDay = Math.min(p.deadline, 5) - 1;
            boolean assigned = false;

            for (int d = lastDay; d >= 0; d--) {
                if (week[d] == null) {
                    week[d] = p;
                    totalRevenue += p.revenue;
                    scheduledIds.add(p.id);
                    assigned = true;
                    break;
                }
            }

            if (!assigned) {
                rejectedProjects.add(p);
            }
        }

        // STEP 5: Expire urgent rejected projects
        expireRejectedUrgentProjects(rejectedProjects);

        // STEP 6: Mark selected projects as SCHEDULED
        updateProjectStatusBatch(scheduledIds);

        // STEP 7: Store weekly revenue
        storeWeeklyRevenue(totalRevenue);

        // STEP 8: Display schedule
        displaySchedule(week, totalRevenue, expectedRevenue);
    }

    // ==========================================
    // Convert SCHEDULED → COMPLETED
    // ==========================================
    private static void markScheduledAsCompleted() {

        String sql =
                "UPDATE projects SET status = 'COMPLETED' WHERE status = 'SCHEDULED'";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate(sql);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // Reduce deadlines by 7 days
    // ==========================================
    private static void reduceDeadlinesByOneWeek() {

        String sql =
                "UPDATE projects SET deadline = deadline - 7 WHERE status = 'PENDING'";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate(sql);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // Expire rejected urgent projects
    // ==========================================
    private static void expireRejectedUrgentProjects(List<Project> rejectedProjects) {

        String sql =
                "UPDATE projects SET status = 'EXPIRED' WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (Project p : rejectedProjects) {

                if (p.deadline <= 5) {
                    ps.setInt(1, p.id);
                    ps.addBatch();
                }
            }

            ps.executeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // Batch Update Status to SCHEDULED
    // ==========================================
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

    // ==========================================
    // Store Weekly Revenue
    // ==========================================
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

    // ==========================================
    // Weighted Moving Average Revenue Prediction
    // ==========================================
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

    // ==========================================
    // Display Weekly Schedule
    // ==========================================
    private static void displaySchedule(Project[] week,
                                        int totalRevenue,
                                        double expectedRevenue) {

        String[] days =
                {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

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
}