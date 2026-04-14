package services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import objects.Employee;
import objects.Patient;

public class DataStoreService {
    private final Path dataDir;
    private final Path patientsFile;
    private final Path employeesFile;
    private final Path blacklistedApplicantsFile;

    public DataStoreService(Path dataDir) {
        this.dataDir = dataDir;
        this.patientsFile = dataDir.resolve("patients.csv");
        this.employeesFile = dataDir.resolve("employees.csv");
        this.blacklistedApplicantsFile = dataDir.resolve("blacklisted_applicants.csv");
    }

    public void initializeDataDirectory() {
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            System.out.println("Unable to create data directory: " + e.getMessage());
        }
    }

    public void loadData(List<Patient> patients, List<Employee> employees) {
        patients.clear();
        employees.clear();

        try {
            if (Files.exists(patientsFile)) {
                List<String> lines = Files.readAllLines(patientsFile);
                for (int i = 1; i < lines.size(); i++) { // skip header
                    String line = lines.get(i);
                    if (line.isBlank()) continue;
                    String[] p = parseCsvLine(line, 14);
                    if (p.length >= 12) {
                        // Backward compatible: if no allergies/meds columns, use empty
                        String allergies = p.length > 12 ? p[12] : "";
                        String activeMeds = p.length > 13 ? p[13] : "";
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
                            p[9],
                            p[10],
                            p[11],
                            allergies,
                            activeMeds
                        ));
                    }
                }
            }

            if (Files.exists(employeesFile)) {
                List<String> lines = Files.readAllLines(employeesFile);
                for (int i = 1; i < lines.size(); i++) { // skip header
                    String line = lines.get(i);
                    if (line.isBlank()) continue;
                    String[] e = parseCsvLine(line, 10);
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

    public void saveData(List<Patient> patients, List<Employee> employees) {
        ArrayList<String> patientLines = new ArrayList<>();
        ArrayList<String> employeeLines = new ArrayList<>();

        patientLines.add("Name,Age,Gender,Phone,Address,PatientId,Diagnosis,Room,AdmissionDate,InsuranceProvider,Status,DischargeDate,Allergies,ActiveMedications");
        for (Patient p : patients) {
            String allergies = p.getAllergies() == null ? "" : String.join(";", p.getAllergies());
            String activeMeds = p.getActiveMedications() == null ? "" : String.join(";", p.getActiveMedications());
            patientLines.add(String.join(",",
                escapeCsv(p.getName()),
                Integer.toString(p.getAge()),
                escapeCsv(p.getGender()),
                escapeCsv(p.getPhoneNumber()),
                escapeCsv(p.getAddress()),
                escapeCsv(p.getPatientId()),
                escapeCsv(p.getDiagnosis()),
                escapeCsv(p.getRoomNumber()),
                escapeCsv(p.getAdmissionDate()),
                escapeCsv(p.getInsuranceProvider()),
                escapeCsv(p.getStatus()),
                escapeCsv(p.getDischargeDate()),
                escapeCsv(allergies),
                escapeCsv(activeMeds)
            ));
        }

        employeeLines.add("Name,Age,Gender,Phone,Address,EmployeeId,Department,Role,Salary,HireDate");
        for (Employee e : employees) {
            employeeLines.add(String.join(",",
                    escapeCsv(e.getName()),
                    Integer.toString(e.getAge()),
                    escapeCsv(e.getGender()),
                    escapeCsv(e.getPhoneNumber()),
                    escapeCsv(e.getAddress()),
                    escapeCsv(e.getEmployeeId()),
                    escapeCsv(e.getDepartment()),
                    escapeCsv(e.getRole()),
                    Double.toString(e.getSalary()),
                    escapeCsv(e.getHireDate())
            ));
        }

        try {
            Files.write(patientsFile, patientLines);
            Files.write(employeesFile, employeeLines);
        } catch (IOException e) {
            System.out.println("Error saving data files: " + e.getMessage());
        }
    }

    public void loadBlacklistedApplicants(java.util.Set<String> blacklistedApplicants) {
        blacklistedApplicants.clear();
        if (!Files.exists(blacklistedApplicantsFile)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(blacklistedApplicantsFile);
            for (int i = 1; i < lines.size(); i++) { // skip header
                String line = lines.get(i);
                if (!line.isBlank()) {
                    blacklistedApplicants.add(line.trim().toLowerCase());
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading blacklist file: " + e.getMessage());
        }
    }

    public void saveBlacklistedApplicants(java.util.Set<String> blacklistedApplicants) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("ApplicantName");
        for (String name : blacklistedApplicants) {
            lines.add(name);
        }
        try {
            Files.write(blacklistedApplicantsFile, lines);
        } catch (IOException e) {
            System.out.println("Error saving blacklist file: " + e.getMessage());
        }
    }

    // CSV escaping: wrap in quotes if contains comma or quote, double quotes inside
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            value = value.replace("\"", "\"\"");
            return '"' + value + '"';
        }
        return value;
    }

    // Simple CSV parser for fixed columns (does not handle all edge cases)
    private String[] parseCsvLine(String line, int expectedCols) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        int col = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().replace("\"\"", "\""));
                sb.setLength(0);
                col++;
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString().replace("\"\"", "\""));
        // Pad with empty strings if missing columns
        while (result.size() < expectedCols) result.add("");
        return result.toArray(new String[0]);
    }
}
