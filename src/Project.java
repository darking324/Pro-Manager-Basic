/*
 * Project Model Class
 *
 * Represents a single project entity in the system.
 *
 * This class is used for:
 * - Database data transfer
 * - Scheduling logic
 * - Predictive scoring
 * - Status management
 */

public class Project {

    // ============================
    // Fields
    // ============================
    private int id;
    private String projectCode;
    private String title;
    private int deadline;          // Remaining deadline (in days)
    private int revenue;
    private String status;         // PENDING, SCHEDULED, COMPLETED, EXPIRED
    private double score;          // Used in predictive scheduling

    // Optional enhancement fields
    private int weeksPending;      // Useful for backlog tracking (future fairness logic)

    // ============================
    // Constructor
    // ============================
    public Project(int id,
                   String projectCode,
                   String title,
                   int deadline,
                   int revenue,
                   String status) {

        this.id = id;
        this.projectCode = projectCode;
        this.title = title;
        this.deadline = deadline;
        this.revenue = revenue;
        this.status = status;
        this.weeksPending = 0;  // default
    }

    // ============================
    // Getters
    // ============================
    public int getId() {
        return id;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public String getTitle() {
        return title;
    }

    public int getDeadline() {
        return deadline;
    }

    public int getRevenue() {
        return revenue;
    }

    public String getStatus() {
        return status;
    }

    public double getScore() {
        return score;
    }

    public int getWeeksPending() {
        return weeksPending;
    }

    // ============================
    // Setters
    // ============================
    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setWeeksPending(int weeksPending) {
        this.weeksPending = weeksPending;
    }

    // ============================
    // Utility Method (Optional)
    // ============================
    @Override
    public String toString() {
        return "Project{" +
                "ID=" + id +
                ", Code='" + projectCode + '\'' +
                ", Title='" + title + '\'' +
                ", Deadline=" + deadline +
                ", Revenue=" + revenue +
                ", Status='" + status + '\'' +
                '}';
    }
}