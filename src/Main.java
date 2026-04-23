import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    private static String getBorderChar(int r, int c, int startRow, int startCol, int width, int height) {
        if ((r == startRow && c == startCol) || (r == startRow && c == startCol + width - 1) ||
            (r == startRow + height - 1 && c == startCol) || (r == startRow + height - 1 && c == startCol + width - 1)) {
            return "+";
        }
        if (r == startRow || r == startRow + height - 1) {
            return "=";
        }
        return "|";
    }

    private static void showMainMenu() {
        int termWidth = 80;
        int termHeight = 24;

        while (true) {
            System.out.print("\033[H\033[2J\033[3J");
            System.out.flush();

            String[] banner = {
                " _   _                 _ _        _       _____  ____  _____ ",
                "| | | |               (_) |      | |     |____ |/ ___|/ __  \\",
                "| |_| | ___  ___ _ __  _| |_ __ _| |______   / / /___ `' / /'",
                "|  _  |/ _ \\/ __| '_ \\| | __/ _` | |______|  \\ \\ ___ \\  / /  ",
                "| | | | (_) \\__ \\ |_) | | || (_| | |     .___/ / \\_/ |./ /___",
                "\\_| |_/\\___/|___/ .__/|_|\\__\\__,_|_|     \\____/\\_____/\\_____/",
                "                | |                                          ",
                "                |_|                                          "
            };

            int boxWidth = 75;
            int leftPadding = Math.max(0, (termWidth - boxWidth) / 2);
            String pad = " ".repeat(leftPadding);

            List<String> menuLines = new ArrayList<>();
            menuLines.add("+" + "=".repeat(boxWidth - 2) + "+");
            menuLines.add("|" + " ".repeat(boxWidth - 2) + "|");
            
            for (String b : banner) {
                int padLen = (boxWidth - 2 - b.length()) / 2;
                int rightPadLen = boxWidth - 2 - b.length() - padLen;
                if (padLen < 0) padLen = 0;
                if (rightPadLen < 0) rightPadLen = 0;
                String leftB = " ".repeat(padLen);
                String rightB = " ".repeat(rightPadLen);
                menuLines.add("|" + leftB + b + rightB + "|");
            }
            
            menuLines.add("|" + " ".repeat(boxWidth - 2) + "|");
            
            String[] options = {
                "[1]  Log in as Employee",
                "[2]  Patients",
                "[3]  Create New Person",
                "[4]  Hire New Employee",
                "[5]  Immediate Assistance",
                "[q]  Quit"
            };
            
            for (String opt : options) {
                int padLen = (boxWidth - 2 - opt.length()) / 2;
                int rightPadLen = boxWidth - 2 - opt.length() - padLen;
                String leftO = " ".repeat(padLen);
                String rightO = " ".repeat(rightPadLen);
                menuLines.add("|" + leftO + opt + rightO + "|");
            }
            
            menuLines.add("|" + " ".repeat(boxWidth - 2) + "|");
            menuLines.add("+" + "=".repeat(boxWidth - 2) + "+");
            menuLines.add("");
            menuLines.add("Select an option: ");

            int topPadding = Math.max(0, (termHeight - menuLines.size()) / 2);
            for (int i = 0; i < topPadding; i++) {
                System.out.println();
            }

            for (int i = 0; i < menuLines.size(); i++) {
                if (i == menuLines.size() - 1) {
                    System.out.print(pad + menuLines.get(i));
                } else {
                    System.out.println(pad + menuLines.get(i));
                }
            }
            System.out.flush();

            int startRow = topPadding + 1;
            int startCol = leftPadding + 1;
            int height = menuLines.size() - 2;
            
            List<int[]> perimeter = new ArrayList<>();
            for (int c = 0; c < boxWidth - 1; c++) perimeter.add(new int[]{startRow, startCol + c});
            for (int r = 0; r < height - 1; r++) perimeter.add(new int[]{startRow + r, startCol + boxWidth - 1});
            for (int c = boxWidth - 1; c > 0; c--) perimeter.add(new int[]{startRow + height - 1, startCol + c});
            for (int r = height - 1; r > 0; r--) perimeter.add(new int[]{startRow + r, startCol});

            Thread animThread = new Thread(() -> {
                try {
                    int pos1 = 0;
                    int pos2 = perimeter.size() / 2;
                    String char1 = "*"; 
                    String char2 = "@"; 

                    while (!Thread.currentThread().isInterrupted()) {
                        System.out.print("\0337"); // Save cursor (VT100)
                        System.out.print("\033[s"); // Save cursor (ANSI)
                        
                        int[] old1 = perimeter.get(pos1);
                        int[] old2 = perimeter.get(pos2);
                        
                        String borderChar1 = getBorderChar(old1[0], old1[1], startRow, startCol, boxWidth, height);
                        String borderChar2 = getBorderChar(old2[0], old2[1], startRow, startCol, boxWidth, height);
                        
                        System.out.print("\033[" + old1[0] + ";" + old1[1] + "H" + borderChar1);
                        System.out.print("\033[" + old2[0] + ";" + old2[1] + "H" + borderChar2);
                        
                        pos1 = (pos1 + 1) % perimeter.size();
                        pos2 = (pos2 + 1) % perimeter.size();
                        
                        int[] new1 = perimeter.get(pos1);
                        int[] new2 = perimeter.get(pos2);
                        
                        System.out.print("\033[" + new1[0] + ";" + new1[1] + "H" + char1);
                        System.out.print("\033[" + new2[0] + ";" + new2[1] + "H" + char2);
                        
                        System.out.print("\0338"); // Restore cursor (VT100)
                        System.out.print("\033[u"); // Restore cursor (ANSI)
                        System.out.flush();
                        
                        Thread.sleep(75);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            animThread.start();
            
            String choice = scanner.nextLine().trim();
            animThread.interrupt();
            try {
                animThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

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
                    if (nurse)      System.out.println("  [" + optNum++ + "] View rooms");
                    if (facilities) System.out.println("  [" + optNum++ + "] Process cleaning queue");
                    if (doctor)     System.out.println("  [" + optNum++ + "] Schedule surgical procedure");
                    if (facilities) System.out.println("  [" + optNum++ + "] Manage rooms & equipment");
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
                    if (!handled && pharmacist) {
                        if (String.valueOf(opt).equals(empChoice)) {
                            pharmacyService.orderPharmacySupplies(scanner);
                            handled = true;
                        }
                        opt++;
                    }
                    if (!handled && nurse) {
                        if (String.valueOf(opt).equals(empChoice)) {
                            roomService.showNurseMenu(
                                    scanner,
                                    selected.getName(),
                                    patients,
                                    () -> dataStoreService.saveData(patients, employees)
                            );
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
                    if (!handled && facilities) {
                        if (String.valueOf(opt).equals(empChoice)) {
                            roomService.showRoomManagementMenu(scanner);
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
