import java.util.ArrayList;
import java.nio.file.Paths;
import java.util.Scanner;
import objects.Employee;
import objects.Patient;
import services.BannerService;
import services.DataStoreService;
import services.PersonCreationService;

public class Main {
    static ArrayList<Patient> patients = new ArrayList<>();
    static ArrayList<Employee> employees = new ArrayList<>();
    static Scanner scanner = new Scanner(System.in);
    static final DataStoreService dataStoreService = new DataStoreService(Paths.get("data"));

    public static void main(String[] args) {
        initializeData();
        BannerService.showWelcomeBanner();
        showMainMenu();
    }

    private static void initializeData() {
        dataStoreService.initializeDataDirectory();
        dataStoreService.loadData(patients, employees);
    }

    private static void showMainMenu() {
        int termWidth = 80;
        int termHeight = 24;

        while (true) {
            System.out.print("\033[H\033[2J\033[3J");
            System.out.flush();

            String[] menuLines = {
                "================================",
                "   Hospital-362 Main Menu",
                "================================",
                "",
                "[1]  Log in as Employee",
                "[2]  View All Patients",
                "[3]  Create a New Person",
                "",
                "================================",
                "",
                "Select an option: "
            };

            int topPadding = (termHeight - menuLines.length) / 2;
            for (int i = 0; i < topPadding; i++) {
                System.out.println();
            }

            for (int i = 0; i < menuLines.length; i++) {
                int leftPadding = (termWidth - menuLines[i].length()) / 2;
                if (leftPadding < 0) leftPadding = 0;
                String pad = " ".repeat(leftPadding);
                if (i == menuLines.length - 1) {
                    System.out.print(pad + menuLines[i]);
                } else {
                    System.out.println(pad + menuLines[i]);
                }
            }

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    employeeLogin();
                    break;
                case "2":
                    viewAllPatients();
                    break;
                case "3":
                    PersonCreationService.createNewPerson(
                            scanner,
                            patients,
                            employees,
                            () -> dataStoreService.saveData(patients, employees)
                    );
                    break;
                default:
                    break;
            }
        }
    }

    private static void employeeLogin() {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Employee Login ===\n");

        if (employees.isEmpty()) {
            System.out.println("  No employees in the system.");
            System.out.println("\n  Press Enter to return to menu...");
            scanner.nextLine();
            return;
        }

        for (int i = 0; i < employees.size(); i++) {
            Employee e = employees.get(i);
            System.out.println("  " + (i + 1) + ". " + e.getName()
                    + " | ID: " + e.getEmployeeId()
                    + " | Department: " + e.getDepartment()
                    + " | Role: " + e.getRole());
        }

        System.out.print("\n  Select employee number: ");
        String selection = scanner.nextLine().trim();

        try {
            int index = Integer.parseInt(selection) - 1;
            if (index >= 0 && index < employees.size()) {
                Employee selected = employees.get(index);
                System.out.println("\n  Welcome, " + selected.getName() + "!");
                System.out.println("  Department: " + selected.getDepartment());
                System.out.println("  Role: " + selected.getRole());
                System.out.println("\n  Press Enter to return to menu...");
                scanner.nextLine();
                return;
            }
        } catch (NumberFormatException ignored) {
            // Fall through to invalid selection message.
        }

        System.out.println("\n  Invalid employee selection.");
        System.out.println("  Press Enter to return to menu...");
        scanner.nextLine();
    }

    private static void viewAllPatients() {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === All Patients ===\n");

        if (patients.isEmpty()) {
            System.out.println("  No patients in the system.");
        } else {
            for (int i = 0; i < patients.size(); i++) {
                Patient p = patients.get(i);
                System.out.println("  " + (i + 1) + ". " + p.getName()
                        + " | ID: " + p.getPatientId()
                        + " | Room: " + p.getRoomNumber()
                        + " | Diagnosis: " + p.getDiagnosis());
            }
        }

        System.out.println("\n  Press Enter to return to menu...");
        scanner.nextLine();
    }

}
