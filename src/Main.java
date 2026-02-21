import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n===== ProManage Predictive Scheduler =====");
            System.out.println("1. Add Project");
            System.out.println("2. View All Projects");
            System.out.println("3. View Pending Projects");
            System.out.println("4. View Scheduled Projects");
            System.out.println("5. Generate Weekly Schedule");
            System.out.println("6. View Revenue History");
            System.out.println("7. Exit");
            System.out.print("Enter choice: ");

            int choice;

            try {
                choice = sc.nextInt();
                sc.nextLine();
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine();
                continue;
            }

            switch (choice) {

                case 1 -> ProjectDAO.addProject();
                case 2 -> ProjectDAO.viewAllProjects();
                case 3 -> ProjectDAO.viewProjectsByStatus("PENDING");
                case 4 -> ProjectDAO.viewProjectsByStatus("SCHEDULED");
                case 5 -> Scheduler.generateSchedule();
                case 6 -> ProjectDAO.viewRevenueHistory();

                case 7 -> {
                    System.out.println("Exiting ProManage...");
                    sc.close();
                    return;
                }

                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }
}