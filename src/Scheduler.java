import java.sql.*;
import java.util.*;

public class Scheduler {

    static class DPProject {
        int id;
        String title;
        int deadline;   // effective WORKING deadline
        int revenue;

        DPProject(int id, String title, int deadline, int revenue) {
            this.id = id;
            this.title = title;
            this.deadline = deadline;
            this.revenue = revenue;
        }
    }

    public static void generateSchedule() {

        List<DPProject> projects = new ArrayList<>();

        // 1️⃣ Read projects from database
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM projects")) {

            while (rs.next()) {

                int calendarDeadline = rs.getInt("deadline");

                // 🔑 Convert CALENDAR deadline → WORKING deadline
                int workingDays = calendarDeadline - (calendarDeadline / 7) * 2;
                workingDays = Math.min(workingDays, 5); // max 5 slots/week

                if (workingDays <= 0) continue; // cannot be scheduled

                projects.add(new DPProject(
                        rs.getInt("id"),
                        rs.getString("title"),
                        workingDays,
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

        // 2️⃣ Sort by effective deadline (important for DP)
        projects.sort(Comparator.comparingInt(p -> p.deadline));

        int n = projects.size();
        int maxDays = 5;

        // 3️⃣ DP table
        int[][] dp = new int[n + 1][maxDays + 1];
        boolean[][] take = new boolean[n + 1][maxDays + 1];

        // 4️⃣ Fill DP table
        for (int i = 1; i <= n; i++) {
            DPProject p = projects.get(i - 1);

            for (int d = 0; d <= maxDays; d++) {
                dp[i][d] = dp[i - 1][d]; // skip project

                if (d > 0 && d <= p.deadline) {
                    int candidate = dp[i - 1][d - 1] + p.revenue;
                    if (candidate > dp[i][d]) {
                        dp[i][d] = candidate;
                        take[i][d] = true;
                    }
                }
            }
        }

        // 5️⃣ Backtrack to find selected projects
        List<DPProject> selected = new ArrayList<>();
        int d = maxDays;

        for (int i = n; i > 0; i--) {
            if (take[i][d]) {
                DPProject p = projects.get(i - 1);
                selected.add(p);
                d--;
            }
        }

        Collections.reverse(selected);

        // 6️⃣ Assign selected projects to days (earliest available)
        String[] schedule = new String[5];
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

        int dayIndex = 0;
        for (DPProject p : selected) {
            if (dayIndex < 5) {
                schedule[dayIndex++] = p.title;
            }
        }

        // 7️⃣ Output
        System.out.println("\nWeekly Schedule:");
        for (int i = 0; i < 5; i++) {
            System.out.println(days[i] + " : " +
                    (schedule[i] != null ? schedule[i] : "No Project"));
        }

        System.out.println("\nTotal Revenue: " + dp[n][maxDays]);
    }
}