package services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import objects.Employee;
import objects.Payslip;

public class PayrollService {

    private final Path payrollFile;
    private static final int BIWEEKLY_DAYS = 14;

    public PayrollService(Path dataDir) {
        this.payrollFile = dataDir.resolve("payroll.csv");
    }

    public void initializeFile() {
        try {
            if (!Files.exists(payrollFile)) {
                Files.write(payrollFile, List.of(
                        "EmployeeId,EmployeeName,PeriodStart,PeriodEnd,Amount,ProcessedDate"
                ));
            }
        } catch (IOException e) {
            System.out.println("Error initializing payroll file: " + e.getMessage());
        }
    }

    public void runProcessPayrollFlow(Scanner scanner, List<Employee> employees) {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Process Employee Payroll ===\n");

        // Step 1: Search for employee by name or ID
        System.out.print("  Enter employee name or ID: ");
        String query = scanner.nextLine().trim();

        List<Employee> matches = new ArrayList<>();
        for (Employee e : employees) {
            if (e.getEmployeeId().toLowerCase().contains(query.toLowerCase())
                    || e.getName().toLowerCase().contains(query.toLowerCase())) {
                matches.add(e);
            }
        }

        if (matches.isEmpty()) {
            System.out.println("\n  No employees found matching \"" + query + "\".");
            pause(scanner);
            return;
        }

        // Step 2: Select employee
        for (int i = 0; i < matches.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + matches.get(i).getName()
                    + " | ID: " + matches.get(i).getEmployeeId()
                    + " | Role: " + matches.get(i).getRole()
                    + " | Salary: $" + String.format("%.2f", matches.get(i).getSalary()));
        }

        System.out.print("\n  Select employee number: ");
        int index;
        try {
            index = Integer.parseInt(scanner.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("\n  Invalid selection.");
            pause(scanner);
            return;
        }
        if (index < 0 || index >= matches.size()) {
            System.out.println("\n  Invalid selection.");
            pause(scanner);
            return;
        }

        Employee employee = matches.get(index);

        // Step 3: Load payroll history and find last processed period end
        List<Payslip> allPayslips = loadPayslips();
        LocalDate lastPeriodEnd = getLastPeriodEnd(allPayslips, employee.getEmployeeId());

        // Step 4: Determine start point
        // If never paid, start from hire date. Otherwise start day after last period end.
        LocalDate periodStart;
        if (lastPeriodEnd == null) {
            try {
                periodStart = LocalDate.parse(employee.getHireDate());
            } catch (Exception e) {
                System.out.println("\n  Invalid hire date on record. Cannot process payroll.");
                pause(scanner);
                return;
            }
        } else {
            periodStart = lastPeriodEnd.plusDays(1);
        }

        // Step 5: Calculate outstanding pay periods
        LocalDate today = LocalDate.now();
        List<Payslip> outstanding = new ArrayList<>();
        double biweeklyAmount = employee.getSalary() / 26.0;

        LocalDate start = periodStart;
        while (!start.isAfter(today)) {
            LocalDate end = start.plusDays(BIWEEKLY_DAYS - 1);
            if (end.isAfter(today)) break; // incomplete period, don't process yet
            outstanding.add(new Payslip(
                    employee.getEmployeeId(),
                    employee.getName(),
                    start.toString(),
                    end.toString(),
                    biweeklyAmount,
                    today.toString()
            ));
            start = end.plusDays(1);
        }

        if (outstanding.isEmpty()) {
            System.out.println("\n  Payroll is up to date for " + employee.getName() + ".");
            pause(scanner);
            return;
        }

        // Step 6: Display outstanding periods and confirm
        System.out.println("\n  Outstanding pay periods for " + employee.getName() + ":");
        for (Payslip p : outstanding) {
            System.out.println("  " + p.getPeriodStart() + " to " + p.getPeriodEnd()
                    + " | $" + String.format("%.2f", p.getAmount()));
        }
        System.out.printf("\n  Total to be paid: $%.2f over %d period(s).%n",
                biweeklyAmount * outstanding.size(), outstanding.size());

        System.out.print("\n  Confirm and process all outstanding payslips? (y/n): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("\n  Payroll processing cancelled.");
            pause(scanner);
            return;
        }

        // Step 7: Save all new payslips
        allPayslips.addAll(outstanding);
        allPayslips.sort(Comparator.comparing(Payslip::getEmployeeId)
                .thenComparing(Payslip::getPeriodStart));
        savePayslips(allPayslips);

        // Step 8: Confirmation summary
        System.out.println("\n  Payroll processed successfully.");
        System.out.println("  " + outstanding.size() + " payslip(s) generated for "
                + employee.getName() + ":");
        for (Payslip p : outstanding) {
            System.out.println("  " + p.toString());
        }
        pause(scanner);
    }

    private LocalDate getLastPeriodEnd(List<Payslip> payslips, String employeeId) {
        LocalDate last = null;
        for (Payslip p : payslips) {
            if (p.getEmployeeId().equalsIgnoreCase(employeeId)) {
                LocalDate end = LocalDate.parse(p.getPeriodEnd());
                if (last == null || end.isAfter(last)) {
                    last = end;
                }
            }
        }
        return last;
    }

    private List<Payslip> loadPayslips() {
        List<Payslip> payslips = new ArrayList<>();
        try {
            if (!Files.exists(payrollFile)) return payslips;
            List<String> lines = Files.readAllLines(payrollFile);
            for (int i = 1; i < lines.size(); i++) { // skip header
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length >= 6) {
                    payslips.add(new Payslip(
                            parts[0],
                            parts[1],
                            parts[2],
                            parts[3],
                            Double.parseDouble(parts[4]),
                            parts[5]
                    ));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading payroll file: " + e.getMessage());
        }
        return payslips;
    }

    private void savePayslips(List<Payslip> payslips) {
        List<String> lines = new ArrayList<>();
        lines.add("EmployeeId,EmployeeName,PeriodStart,PeriodEnd,Amount,ProcessedDate");
        for (Payslip p : payslips) {
            lines.add(String.join(",",
                    escapeCsv(p.getEmployeeId()),
                    escapeCsv(p.getEmployeeName()),
                    escapeCsv(p.getPeriodStart()),
                    escapeCsv(p.getPeriodEnd()),
                    String.format("%.2f", p.getAmount()),
                    escapeCsv(p.getProcessedDate())
            ));
        }
        try {
            Files.write(payrollFile, lines);
        } catch (IOException e) {
            System.out.println("Error saving payroll file: " + e.getMessage());
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            value = value.replace("\"", "\"\"");
            return '"' + value + '"';
        }
        return value;
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }

    private static void pause(Scanner scanner) {
        System.out.println("\n  Press Enter to return to menu...");
        scanner.nextLine();
    }
}