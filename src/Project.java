public class Project {

    private int id;
    private String projectCode;
    private String title;
    private int deadline;
    private int revenue;
    private String status;
    private double score;   // used for predictive scheduling

    public Project(int id, String projectCode, String title,
                   int deadline, int revenue, String status) {

        this.id = id;
        this.projectCode = projectCode;
        this.title = title;
        this.deadline = deadline;
        this.revenue = revenue;
        this.status = status;
    }

    // Getters
    public int getId() { return id; }

    public String getProjectCode() { return projectCode; }

    public String getTitle() { return title; }

    public int getDeadline() { return deadline; }

    public int getRevenue() { return revenue; }

    public String getStatus() { return status; }

    public double getScore() { return score; }

    // Setters
    public void setScore(double score) {
        this.score = score;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}