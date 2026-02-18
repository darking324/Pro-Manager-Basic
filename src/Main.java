import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n--- ProManage Scheduler ---");
            System.out.println("1. Add Project");
            System.out.println("2. View All Projects");
            System.out.println("3. Generate Weekly Schedule");
            System.out.println("4. Exit");
            System.out.print("Enter choice: ");

            int choice = sc.nextInt();
            sc.nextLine(); // consume newline

            switch (choice) {
                case 1 -> ProjectDAO.addProject();
                case 2 -> ProjectDAO.viewAllProjects();
                case 3 -> Scheduler.generateSchedule();
                case 4 -> {
                    System.out.println("Exiting...");
                    System.exit(0);
                }
                default -> System.out.println("Invalid choice");
            }
        }
    }
}