import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import objects.Employee;
import objects.Patient;

public class Main {
    static ArrayList<Patient> patients = new ArrayList<>();
    static ArrayList<Employee> employees = new ArrayList<>();
    static Scanner scanner = new Scanner(System.in);
    static final Path DATA_DIR = Paths.get("data");
    static final Path PATIENTS_FILE = DATA_DIR.resolve("patients.txt");
    static final Path EMPLOYEES_FILE = DATA_DIR.resolve("employees.txt");

    public static void main(String[] args) {
        initializeData();

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

        System.out.println();
        for (String line : banner) {
            System.out.println(line);
            sleep(120);
        }
        System.out.println();

        sleep(1000);
        showMainMenu();
    }

    private static void initializeData() {
        createDataDirectory();
        loadFromFiles();
    }

    private static void createDataDirectory() {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException e) {
            System.out.println("Unable to create data directory: " + e.getMessage());
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\|");
    }

    private static String[] splitEscapedPipe(String line) {
        ArrayList<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '|') {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    private static void loadFromFiles() {
        patients.clear();
        employees.clear();

        try {
            if (Files.exists(PATIENTS_FILE)) {
                for (String line : Files.readAllLines(PATIENTS_FILE)) {
                    if (line.isBlank()) {
                        continue;
                    }
                    String[] p = splitEscapedPipe(line);
                    if (p.length == 10) {
                        patients.add(new Patient(
                                p[0],
                                Integer.parseInt(p[1]),
                                p[2],
                                p[3],
                                p[4],
                                p[5],
                                p[6],
                                p[7],
                                p[8],
                                p[9]
                        ));
                    }
                }
            }

            if (Files.exists(EMPLOYEES_FILE)) {
                for (String line : Files.readAllLines(EMPLOYEES_FILE)) {
                    if (line.isBlank()) {
                        continue;
                    }
                    String[] e = splitEscapedPipe(line);
                    if (e.length == 10) {
                        employees.add(new Employee(
                                e[0],
                                Integer.parseInt(e[1]),
                                e[2],
                                e[3],
                                e[4],
                                e[5],
                                e[6],
                                e[7],
                                Double.parseDouble(e[8]),
                                e[9]
                        ));
                    }
                }
            }
        } catch (IOException | NumberFormatException ex) {
            System.out.println("Error loading data files: " + ex.getMessage());
        }
    }

    private static void saveAllData() {
        ArrayList<String> patientLines = new ArrayList<>();
        ArrayList<String> employeeLines = new ArrayList<>();

        for (Patient p : patients) {
            patientLines.add(String.join("|",
                    escape(p.getName()),
                    Integer.toString(p.getAge()),
                    escape(p.getGender()),
                    escape(p.getPhoneNumber()),
                    escape(p.getAddress()),
                    escape(p.getPatientId()),
                    escape(p.getDiagnosis()),
                    escape(p.getRoomNumber()),
                    escape(p.getAdmissionDate()),
                    escape(p.getInsuranceProvider())
            ));
        }

        for (Employee e : employees) {
            employeeLines.add(String.join("|",
                    escape(e.getName()),
                    Integer.toString(e.getAge()),
                    escape(e.getGender()),
                    escape(e.getPhoneNumber()),
                    escape(e.getAddress()),
                    escape(e.getEmployeeId()),
                    escape(e.getDepartment()),
                    escape(e.getRole()),
                    Double.toString(e.getSalary()),
                    escape(e.getHireDate())
            ));
        }

        try {
            Files.write(PATIENTS_FILE, patientLines);
            Files.write(EMPLOYEES_FILE, employeeLines);
        } catch (IOException e) {
            System.out.println("Error saving data files: " + e.getMessage());
        }
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
                    createNewPerson();
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

    private static void createNewPerson() {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Create a New Person ===\n");
        System.out.println("  [1] New Patient");
        System.out.println("  [2] New Employee");
        System.out.print("\n  Select type: ");
        String type = scanner.nextLine().trim();

        System.out.print("  Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("  Age: ");
        int age = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("  Gender: ");
        String gender = scanner.nextLine().trim();
        System.out.print("  Phone: ");
        String phone = scanner.nextLine().trim();
        System.out.print("  Address: ");
        String address = scanner.nextLine().trim();

        if (type.equals("1")) {
            System.out.print("  Patient ID: ");
            String pid = scanner.nextLine().trim();
            System.out.print("  Diagnosis: ");
            String diag = scanner.nextLine().trim();
            System.out.print("  Room Number: ");
            String room = scanner.nextLine().trim();
            System.out.print("  Admission Date (YYYY-MM-DD): ");
            String admDate = scanner.nextLine().trim();
            System.out.print("  Insurance Provider: ");
            String ins = scanner.nextLine().trim();

            patients.add(new Patient(name, age, gender, phone, address, pid, diag, room, admDate, ins));
            saveAllData();
            System.out.println("\n  Patient created successfully!");
        } else if (type.equals("2")) {
            System.out.print("  Employee ID: ");
            String eid = scanner.nextLine().trim();
            System.out.print("  Department: ");
            String dept = scanner.nextLine().trim();
            System.out.print("  Role: ");
            String role = scanner.nextLine().trim();
            System.out.print("  Salary: ");
            double salary = Double.parseDouble(scanner.nextLine().trim());
            System.out.print("  Hire Date (YYYY-MM-DD): ");
            String hireDate = scanner.nextLine().trim();

            employees.add(new Employee(name, age, gender, phone, address, eid, dept, role, salary, hireDate));
            saveAllData();
            System.out.println("\n  Employee created successfully!");
        } else {
            System.out.println("\n  Invalid selection.");
        }

        System.out.println("  Press Enter to return to menu...");
        scanner.nextLine();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
