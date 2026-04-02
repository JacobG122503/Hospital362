import java.util.ArrayList;
import java.nio.file.Paths;
import java.util.Scanner;
import objects.Employee;
import objects.Patient;
import services.*;

public class Main {
    static ArrayList<Patient> patients = new ArrayList<>();
    static ArrayList<Employee> employees = new ArrayList<>();
    static Scanner scanner = new Scanner(System.in);
    static final DataStoreService dataStoreService = new DataStoreService(Paths.get("data"));
    static final PharmacyService pharmacyService = new PharmacyService(Paths.get("data"));

    public static void main(String[] args) {
        initializeData();
        BannerService.showWelcomeBanner();
        showMainMenu();
    }

    private static void initializeData() {
        dataStoreService.initializeDataDirectory();
        dataStoreService.loadData(patients, employees);
        pharmacyService.initializeFiles();
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
                "[2]  Patients",
                "[3]  Create New Person",
                "[q]  Quit",
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
                    PatientsService.createService(
                            scanner,
                            patients,
                            () -> dataStoreService.saveData(patients, employees)
                    );
                    break;
                case "3":
                    PersonCreationService.createNewPerson(
                            scanner,
                            patients,
                            employees,
                            () -> dataStoreService.saveData(patients, employees)
                    );
                    break;
                case "q":
                    System.out.println();
                    System.exit(0);
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

                if (isPharmacist(selected)) {
                    System.out.println("\n  [1] Dispense prescribed medication");
                    System.out.println("  [2] Return to main menu");
                    System.out.print("\n  Select option: ");
                    String pharmacistChoice = scanner.nextLine().trim();
                    if ("1".equals(pharmacistChoice)) {
                        pharmacyService.dispensePrescribedMedication(scanner, patients);
                    }
                } else {
                    System.out.println("\n  Press Enter to return to menu...");
                    scanner.nextLine();
                }
                return;
            }
        } catch (NumberFormatException ignored) {
            // Fall through to invalid selection message.
        }

        System.out.println("\n  Invalid employee selection.");
        System.out.println("  Press Enter to return to menu...");
        scanner.nextLine();
    }

    private static boolean isPharmacist(Employee employee) {
        String department = employee.getDepartment() == null ? "" : employee.getDepartment();
        String role = employee.getRole() == null ? "" : employee.getRole();
        return department.equalsIgnoreCase("Pharmacy") || role.toLowerCase().contains("pharmacist");
    }

}
