package services;

import java.util.List;
import java.util.Scanner;
import java.util.Set;
import objects.Employee;
import objects.JobApplicant;

public class HiringService {

    public static void runHireNewEmployeeFlow(
            Scanner scanner,
            List<Employee> employees,
            Set<String> blacklistedApplicants,
            Runnable onSave
    ) {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Hire New Employee ===\n");

        JobApplicant applicant = collectApplicant(scanner);
        String applicantKey = applicant.getApplicantKey();

        if (blacklistedApplicants.contains(applicantKey)) {
            sendRejectionLetter(applicant, "Applicant is blacklisted from future applications.");
            pause(scanner);
            return;
        }

        System.out.println("\n  Step 1: Human Resources reviews application.");
        if (!askYesNo(scanner, "  Is the applicant a good fit? (y/n): ")) {
            sendRejectionLetter(applicant, "Applicant was not a fit for the position.");
            pause(scanner);
            return;
        }

        System.out.println("\n  Step 2: Interview is scheduled with appropriate manager/director.");
        System.out.print("  Interview date (YYYY-MM-DD): ");
        String interviewDate = scanner.nextLine().trim();
        System.out.println("  Interview scheduled for " + interviewDate + ".");

        System.out.println("\n  Step 3: Manager/director interviews applicant.");
        if (!askYesNo(scanner, "  Did the manager/director approve the candidate? (y/n): ")) {
            sendRejectionLetter(applicant, "Applicant did not pass the interview stage.");
            pause(scanner);
            return;
        }

        System.out.println("\n  Step 4: Background check is performed.");
        if (!askYesNo(scanner, "  Did the applicant pass the background check? (y/n): ")) {
            blacklistedApplicants.add(applicantKey);
            onSave.run();
            sendRejectionLetter(applicant, "Applicant failed background check and is now blacklisted.");
            pause(scanner);
            return;
        }

        System.out.println("\n  Step 5: Job offer is sent.");
        if (!askYesNo(scanner, "  Did the applicant accept the job offer? (y/n): ")) {
            System.out.println("\n  Applicant declined the offer. Hiring process ended.");
            pause(scanner);
            return;
        }

        System.out.println("\n  Step 6: Applicant begins onboarding.");
        Employee newEmployee = createEmployeeFromApplicant(scanner, applicant, employees.size() + 1);
        employees.add(newEmployee);
        onSave.run();

        System.out.println("\n  Onboarding started. New employee added to system:");
        System.out.println("  " + newEmployee.getName() + " | ID: " + newEmployee.getEmployeeId());
        pause(scanner);
    }

    private static JobApplicant collectApplicant(Scanner scanner) {
        System.out.print("  Applicant Name: ");
        String name = scanner.nextLine().trim();
        int age = readInt(scanner, "  Applicant Age: ");
        System.out.print("  Applicant Gender: ");
        String gender = scanner.nextLine().trim();
        System.out.print("  Applicant Phone: ");
        String phone = scanner.nextLine().trim();
        System.out.print("  Applicant Address: ");
        String address = scanner.nextLine().trim();
        System.out.print("  Desired Department: ");
        String desiredDepartment = scanner.nextLine().trim();
        System.out.print("  Desired Role: ");
        String desiredRole = scanner.nextLine().trim();

        return new JobApplicant(name, age, gender, phone, address, desiredDepartment, desiredRole);
    }

    private static Employee createEmployeeFromApplicant(Scanner scanner, JobApplicant applicant, int nextIndex) {
        String defaultId = "E" + nextIndex;
        System.out.print("  Employee ID [" + defaultId + "]: ");
        String employeeId = scanner.nextLine().trim();
        if (employeeId.isEmpty()) {
            employeeId = defaultId;
        }

        double salary = readDouble(scanner, "  Salary: ");
        System.out.print("  Hire Date (YYYY-MM-DD): ");
        String hireDate = scanner.nextLine().trim();

        return new Employee(
                applicant.getName(),
                applicant.getAge(),
                applicant.getGender(),
                applicant.getPhoneNumber(),
                applicant.getAddress(),
                employeeId,
                applicant.getDesiredDepartment(),
                applicant.getDesiredRole(),
                salary,
                hireDate
        );
    }

    private static void sendRejectionLetter(JobApplicant applicant, String reason) {
        System.out.println("\n  Rejection letter sent to " + applicant.getName() + ".");
        System.out.println("  Reason: " + reason);
    }

    private static boolean askYesNo(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim().toLowerCase();
            if ("y".equals(value) || "yes".equals(value)) {
                return true;
            }
            if ("n".equals(value) || "no".equals(value)) {
                return false;
            }
            System.out.println("  Please answer y or n.");
        }
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                System.out.println("  Please enter a valid number.");
            }
        }
    }

    private static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
                System.out.println("  Please enter a valid amount.");
            }
        }
    }

    private static void pause(Scanner scanner) {
        System.out.println("\n  Press Enter to return to menu...");
        scanner.nextLine();
    }
}