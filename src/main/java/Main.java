import java.util.Scanner;

/*
 * Main class - Entry point of ProManage Predictive Scheduler
 *
 * Responsibilities:
 * - Display menu options
 * - Handle user input
 * - Call appropriate service methods
 *
 * Updated Features:
 * - Added "View Completed Projects"
 * - Exit shifted to option 9
 */

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        while (true) {

            System.out.println("\n===== ProManage Predictive Scheduler =====");
            System.out.println("1. Add Project");
            System.out.println("2. View All Projects");
            System.out.println("3. Generate Weekly Schedule");
            System.out.println("4. View Scheduled Projects");
            System.out.println("5. View Revenue History");
            System.out.println("6. View Pending Projects");
            System.out.println("7. View Completed Projects");   // NEW
            System.out.println("8. View Expired Projects");
            System.out.println("9. Exit");
            System.out.print("Enter choice: ");

            int choice;

            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            switch (choice) {

                case 1 -> ProjectDAO.addProject(sc);

                case 2 -> ProjectDAO.viewAllProjects();

                case 3 -> {
                    if (confirmScheduleGeneration(sc)) {
                        Scheduler.generateSchedule();
                    } else {
                        System.out.println("Schedule generation cancelled.");
                    }
                }

                case 4 -> ProjectDAO.viewProjectsByStatus("SCHEDULED");

                case 5 -> ProjectDAO.viewRevenueHistory();

                case 6 -> ProjectDAO.viewProjectsByStatus("PENDING");

                case 7 -> ProjectDAO.viewProjectsByStatus("COMPLETED");  // NEW

                case 8 -> ProjectDAO.viewProjectsByStatus("EXPIRED");

                case 9 -> {
                    System.out.println("Exiting ProManage...");
                    sc.close();
                    return;
                }

                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private static boolean confirmScheduleGeneration(Scanner sc) {
        System.out.println("\nThis will:");
        System.out.println("- Mark last week's scheduled projects as COMPLETED");
        System.out.println("- Decrease pending project deadlines by 7 days");
        System.out.println("- Generate and save a new weekly schedule");
        System.out.print("Continue? (y/n): ");

        String answer = sc.nextLine().trim().toLowerCase();
        return "y".equals(answer) || "yes".equals(answer);
    }
}