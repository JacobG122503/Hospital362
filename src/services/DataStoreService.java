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

    public DataStoreService(Path dataDir) {
        this.dataDir = dataDir;
        this.patientsFile = dataDir.resolve("patients.txt");
        this.employeesFile = dataDir.resolve("employees.txt");
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
                for (String line : Files.readAllLines(patientsFile)) {
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

            if (Files.exists(employeesFile)) {
                for (String line : Files.readAllLines(employeesFile)) {
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

    public void saveData(List<Patient> patients, List<Employee> employees) {
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
            Files.write(patientsFile, patientLines);
            Files.write(employeesFile, employeeLines);
        } catch (IOException e) {
            System.out.println("Error saving data files: " + e.getMessage());
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\|");
    }

    private String[] splitEscapedPipe(String line) {
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
}
