package services;

import java.util.List;
import java.util.Scanner;
import objects.Employee;
import objects.Patient;

public class PersonCreationService {
    private static String patientIDTag = "P";
    public static void createNewPerson(
            Scanner scanner,
            List<Patient> patients,
            List<Employee> employees,
            Runnable onSave
    ) {
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
            System.out.print("  Diagnosis: ");
            String diag = scanner.nextLine().trim();
            System.out.print("  Room Number: ");
            String room = scanner.nextLine().trim();
            System.out.print("  Admission Date (YYYY-MM-DD): ");
            String admDate = scanner.nextLine().trim();
            System.out.print("  Insurance Provider: ");
            String ins = scanner.nextLine().trim();

            patients.add(new Patient(name, age, gender, phone, address, patientIDTag + (patients.size()  + 1), diag, room, admDate, ins));
            onSave.run();
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
            onSave.run();
            System.out.println("\n  Employee created successfully!");
        } else {
            System.out.println("\n  Invalid selection.");
        }

        System.out.println("  Press Enter to return to menu...");
        scanner.nextLine();
    }
}
