import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/*
 * Scheduler Class
 *
 * Responsibilities:
 * - Convert last week's SCHEDULED -> COMPLETED
 * - Reduce deadlines weekly
 * - Apply predictive scoring
 * - Perform greedy scheduling
 * - Expire rejected and overdue projects
 * - Store weekly revenue
 */

public class Scheduler {

    public static void generateSchedule() {
        Connection con = null;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            int completedCount = markScheduledAsCompleted(con);
            int reducedCount = reduceDeadlinesByOneWeek(con);
            int overdueExpiredCount = expireOverduePendingProjects(con);
            double expectedRevenue = predictNextWeekRevenue(con);

            List<Project> projects = fetchSchedulablePendingProjects(con);

            if (projects.isEmpty()) {
                con.commit();
                System.out.println("No schedulable projects this week.");
                printWeekSummary(0, expectedRevenue, completedCount, reducedCount, 0, overdueExpiredCount, 0);
                return;
            }

            scoreProjects(projects, expectedRevenue);
            projects.sort(Comparator.comparingDouble(Project::getScore).reversed());

            Project[] week = new Project[SchedulingConfig.DAYS_PER_WEEK];
            List<Integer> scheduledIds = new ArrayList<>();
            List<Project> rejectedProjects = new ArrayList<>();
            int totalRevenue = 0;

            for (Project p : projects) {
                int lastDay = Math.min(p.getDeadline(), SchedulingConfig.DAYS_PER_WEEK) - 1;
                boolean assigned = false;

                for (int day = lastDay; day >= 0; day--) {
                    if (week[day] == null) {
                        week[day] = p;
                        scheduledIds.add(p.getId());
                        totalRevenue += p.getRevenue();
                        assigned = true;
                        break;
                    }
                }

                if (!assigned) {
                    rejectedProjects.add(p);
                }
            }

            int rejectedExpiredCount = expireRejectedUrgentProjects(con, rejectedProjects);
            int scheduledCount = updateProjectStatusBatch(con, scheduledIds);
            storeWeeklyRevenue(con, totalRevenue);

            con.commit();

            displaySchedule(week, totalRevenue, expectedRevenue);
            printWeekSummary(totalRevenue, expectedRevenue, completedCount, reducedCount, scheduledCount,
                    overdueExpiredCount, rejectedExpiredCount);

        } catch (SQLException e) {
            rollbackQuietly(con);
            System.out.println("Schedule generation failed: " + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ignored) {
                    // Ignore cleanup errors.
                }
            }
        }
    }

    private static void scoreProjects(List<Project> projects, double expectedRevenue) {
        for (Project p : projects) {
            double urgency = 1.0 / Math.max(p.getDeadline(), 1);
            int futurePenalty = 0;

            if (p.getDeadline() > SchedulingConfig.LONG_DEADLINE_THRESHOLD_DAYS
                    && p.getRevenue() < expectedRevenue * SchedulingConfig.LOW_VALUE_REVENUE_MULTIPLIER) {
                futurePenalty = 1;
            }

            double score =
                    (SchedulingConfig.REVENUE_WEIGHT * p.getRevenue())
                            + (SchedulingConfig.URGENCY_WEIGHT * urgency * SchedulingConfig.URGENCY_MULTIPLIER)
                            - (SchedulingConfig.PENALTY_WEIGHT * futurePenalty * p.getRevenue());

            p.setScore(score);
        }
    }

    private static List<Project> fetchSchedulablePendingProjects(Connection con) throws SQLException {
        List<Project> projects = new ArrayList<>();

        String sql = "SELECT id, project_code, title, deadline, revenue, status "
                + "FROM projects WHERE status = ? AND deadline > 0";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ProjectStatus.PENDING.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    projects.add(new Project(
                            rs.getInt("id"),
                            rs.getString("project_code"),
                            rs.getString("title"),
                            rs.getInt("deadline"),
                            rs.getInt("revenue"),
                            rs.getString("status")
                    ));
                }
            }
        }

        return projects;
    }

    private static int markScheduledAsCompleted(Connection con) throws SQLException {
        String sql = "UPDATE projects SET status = ? WHERE status = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ProjectStatus.COMPLETED.name());
            ps.setString(2, ProjectStatus.SCHEDULED.name());
            return ps.executeUpdate();
        }
    }

    private static int reduceDeadlinesByOneWeek(Connection con) throws SQLException {
        String sql = "UPDATE projects SET deadline = deadline - ? WHERE status = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, SchedulingConfig.DAYS_DECREASE_PER_WEEK);
            ps.setString(2, ProjectStatus.PENDING.name());
            return ps.executeUpdate();
        }
    }

    private static int expireOverduePendingProjects(Connection con) throws SQLException {
        String sql = "UPDATE projects SET status = ? WHERE status = ? AND deadline <= 0";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ProjectStatus.EXPIRED.name());
            ps.setString(2, ProjectStatus.PENDING.name());
            return ps.executeUpdate();
        }
    }

    private static int expireRejectedUrgentProjects(Connection con, List<Project> rejectedProjects) throws SQLException {
        String sql = "UPDATE projects SET status = ? WHERE id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (Project p : rejectedProjects) {
                if (p.getDeadline() <= SchedulingConfig.DAYS_PER_WEEK) {
                    ps.setString(1, ProjectStatus.EXPIRED.name());
                    ps.setInt(2, p.getId());
                    ps.addBatch();
                }
            }

            int[] batch = ps.executeBatch();
            return successfulBatchCount(batch);
        }
    }

    private static int updateProjectStatusBatch(Connection con, List<Integer> ids) throws SQLException {
        if (ids.isEmpty()) {
            return 0;
        }

        String sql = "UPDATE projects SET status = ? WHERE id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int id : ids) {
                ps.setString(1, ProjectStatus.SCHEDULED.name());
                ps.setInt(2, id);
                ps.addBatch();
            }

            int[] batch = ps.executeBatch();
            return successfulBatchCount(batch);
        }
    }

    private static int successfulBatchCount(int[] batch) {
        int count = 0;
        for (int result : batch) {
            if (result >= 0 || result == Statement.SUCCESS_NO_INFO) {
                count++;
            }
        }
        return count;
    }

    private static void storeWeeklyRevenue(Connection con, int revenue) throws SQLException {
        String sql = "INSERT INTO weekly_revenue_history(total_revenue) VALUES (?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, revenue);
            ps.executeUpdate();
        }
    }

    private static double predictNextWeekRevenue(Connection con) throws SQLException {
        List<Integer> revenues = new ArrayList<>();

        String sql = "SELECT total_revenue FROM weekly_revenue_history ORDER BY week_no DESC LIMIT 3";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                revenues.add(rs.getInt("total_revenue"));
            }
        }

        if (revenues.isEmpty()) {
            return 0;
        }

        if (revenues.size() == 1) {
            return revenues.get(0);
        }

        if (revenues.size() == 2) {
            return (revenues.get(0) + revenues.get(1)) / 2.0;
        }

        return (SchedulingConfig.MOST_RECENT_REVENUE_WEIGHT * revenues.get(0))
                + (SchedulingConfig.SECOND_RECENT_REVENUE_WEIGHT * revenues.get(1))
                + (SchedulingConfig.THIRD_RECENT_REVENUE_WEIGHT * revenues.get(2));
    }

    private static void displaySchedule(Project[] week, int totalRevenue, double expectedRevenue) {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

        System.out.println("\n=========================================");
        System.out.println("Expected Next Week Revenue: " + String.format("%.2f", expectedRevenue));
        System.out.println("=========================================");

        System.out.println("\nWeekly Schedule:");
        for (int i = 0; i < SchedulingConfig.DAYS_PER_WEEK; i++) {
            Project p = week[i];
            String label = (p != null)
                    ? p.getTitle() + " (" + p.getProjectCode() + ")"
                    : "No Project";
            System.out.println(days[i] + " : " + label);
        }

        System.out.println("\nTotal Revenue: " + totalRevenue);
        System.out.println("=========================================");
    }

    private static void printWeekSummary(int totalRevenue,
                                         double expectedRevenue,
                                         int completedCount,
                                         int reducedCount,
                                         int scheduledCount,
                                         int overdueExpiredCount,
                                         int rejectedExpiredCount) {
        System.out.println("\nWeek Summary");
        System.out.println("- Completed from previous week: " + completedCount);
        System.out.println("- Pending deadlines reduced: " + reducedCount);
        System.out.println("- Newly scheduled: " + scheduledCount);
        System.out.println("- Expired due to overdue deadline: " + overdueExpiredCount);
        System.out.println("- Expired after rejection this week: " + rejectedExpiredCount);
        System.out.println("- Expected revenue baseline: " + String.format("%.2f", expectedRevenue));
        System.out.println("- Realized weekly revenue: " + totalRevenue);
    }

    private static void rollbackQuietly(Connection con) {
        if (con == null) {
            return;
        }

        try {
            con.rollback();
        } catch (SQLException ignored) {
            // Ignore rollback errors.
        }
    }
}
