import java.util.ArrayList;
import java.util.HashSet;
import java.nio.file.Paths;
import java.util.Scanner;
import objects.Employee;
import objects.Patient;
import services.*;

public class Main {
    static ArrayList<Patient> patients = new ArrayList<>();
    static ArrayList<Employee> employees = new ArrayList<>();
    static HashSet<String> blacklistedApplicants = new HashSet<>();
    static Scanner scanner = new Scanner(System.in);
    static final PatientsService patientsService = new PatientsService(Paths.get("data"));
    static final DataStoreService dataStoreService = new DataStoreService(Paths.get("data"));
    static final PharmacyService pharmacyService = new PharmacyService(Paths.get("data"));
    static final RoomService roomService = new RoomService(Paths.get("data"));
    static final SurgicalService surgicalService = new SurgicalService(Paths.get("data"));

    public static void main(String[] args) {
        initializeData();
        BannerService.showWelcomeBanner();
        showMainMenu();
    }

    private static void initializeData() {
        dataStoreService.initializeDataDirectory();
        dataStoreService.loadData(patients, employees);
        dataStoreService.loadBlacklistedApplicants(blacklistedApplicants);
        pharmacyService.initializeFiles();
        roomService.initializeFile();
        roomService.loadQueue();
        surgicalService.initializeFile();
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
                "[4]  Hire New Employee",
                "[5]  Immediate Assistance",
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
                            patientsService,
                            roomService,
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
                case "4":
                    HiringService.runHireNewEmployeeFlow(
                            scanner,
                            employees,
                            blacklistedApplicants,
                            () -> {
                                dataStoreService.saveData(patients, employees);
                                dataStoreService.saveBlacklistedApplicants(blacklistedApplicants);
                            }
                    );
                    break;
                case "5":
                    needAssistance();
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

    // Doctor workflow for prescribing medication
    private static void prescribeMedicationWorkflow() {
        pharmacyService.prescribeMedication(scanner, patients, () -> dataStoreService.saveData(patients, employees));
    }

    private static boolean isDoctor(Employee employee) {
        String department = employee.getDepartment() == null ? "" : employee.getDepartment();
        String role = employee.getRole() == null ? "" : employee.getRole();
        return department.equalsIgnoreCase("Medical Services") || role.toLowerCase().contains("doctor") || role.toLowerCase().contains("physician") || role.toLowerCase().contains("surgeon");
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
            System.out.println("  [" + (i + 1) + "] " + e.getName()
                    + ", ID: " + e.getEmployeeId()
                    + ", Department: " + e.getDepartment()
                    + ", Role: " + e.getRole());
        }

        System.out.print("\n  Select employee number (or 'q' to return): ");
        String selection = scanner.nextLine().trim();
        if (selection.equalsIgnoreCase("q")) return;

        try {
            int index = Integer.parseInt(selection) - 1;
            if (index >= 0 && index < employees.size()) {
                Employee selected = employees.get(index);
                System.out.println("\n  Welcome, " + selected.getName() + "!");
                System.out.println("  Department: " + selected.getDepartment());
                System.out.println("  Role: " + selected.getRole());

                boolean pharmacist = isPharmacist(selected);
                boolean nurse = isNurse(selected);
                boolean facilities = isFacilitiesManagement(selected);
                boolean doctor = isDoctor(selected);

                if (!pharmacist && !nurse && !facilities && !doctor) {
                    System.out.println("\n  Press Enter to return to menu...");
                    scanner.nextLine();
                    return;
                }

                while (true) {
                    System.out.println();
                    int optNum = 1;
                    if (doctor)     System.out.println("  [" + optNum++ + "] Prescribe medication");
                    if (pharmacist) System.out.println("  [" + optNum++ + "] Dispense prescribed medication");
                    if (pharmacist) System.out.println("  [" + optNum++ + "] Audit medication inventory");
                    if (nurse)      System.out.println("  [" + optNum++ + "] Request room cleaning");
                    if (facilities) System.out.println("  [" + optNum++ + "] Process cleaning queue");
                    if (doctor)     System.out.println("  [" + optNum++ + "] Schedule surgical procedure");
                    System.out.println("  [" + optNum + "] Return to main menu");
                    System.out.print("\n  Select option (or 'q' to return): ");
                    String empChoice = scanner.nextLine().trim();
                    if (empChoice.equalsIgnoreCase("q")) return;

                    int opt = 1;
                    boolean handled = false;
                    if (doctor) {
                        if (String.valueOf(opt).equals(empChoice)) {
                            prescribeMedicationWorkflow();
                            handled = true;
                        }
                        opt++;
                    }
                    if (!handled && pharmacist) {
                        if (String.valueOf(opt).equals(empChoice)) {
                            pharmacyService.dispensePrescribedMedication(scanner, patients);
                            handled = true;
                        }
                        opt++;
                    }
                    if (!handled && pharmacist) {
                        if (String.valueOf(opt).equals(empChoice)) {
                            pharmacyService.auditMedicationInventory(scanner);
                            handled = true;
                        }
                        opt++;
                    }
                    if (!handled && nurse) {
                        if (String.valueOf(opt).equals(empChoice)) {
                            roomService.showNurseMenu(scanner, selected.getName());
                            handled = true;
                        }
                        opt++;
                    }
                    if (!handled && facilities) {
                        if (String.valueOf(opt).equals(empChoice)) {
                            roomService.showFacilitiesMenu(scanner);
                            handled = true;
                        }
                        opt++;
                    }
                    if (!handled && doctor) {
                        if (String.valueOf(opt).equals(empChoice)) {
                            surgicalService.runScheduleSurgeryFlow(scanner, patients, employees, roomService);
                            handled = true;
                        }
                        opt++;
                    }
                    if (!handled || String.valueOf(opt).equals(empChoice)) {
                        break;
                    }
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

    public static void needAssistance()
    {
        System.out.println("Immediate Assistance button pressed");
        System.out.println("Assistance is on the way... Stand by");
        System.out.println("  Press Enter to return to menu...");
        scanner.nextLine();
    }

    private static boolean isPharmacist(Employee employee) {
        String department = employee.getDepartment() == null ? "" : employee.getDepartment();
        String role = employee.getRole() == null ? "" : employee.getRole();
        return department.equalsIgnoreCase("Pharmacy") || role.toLowerCase().contains("pharmacist");
    }

    private static boolean isNurse(Employee employee) {
        String role = employee.getRole() == null ? "" : employee.getRole();
        return role.toLowerCase().contains("nurse");
    }

    private static boolean isFacilitiesManagement(Employee employee) {
        String department = employee.getDepartment() == null ? "" : employee.getDepartment();
        return department.equalsIgnoreCase("Facilities Management");
    }

}
