package services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import objects.Billing;
import objects.Patient;

public class PatientsService {
    private static String patientIDTag = "P";
    private final Path patientBillingPath;
    private final Path administrationLogPath;

    public PatientsService(Path dataDir) {
        this.patientBillingPath = dataDir.resolve("patients_billing.csv");
        this.administrationLogPath = dataDir.resolve("medication_administrations.csv");
    }

    public static void createService(Scanner scanner, List<Patient> patients, PatientsService service, RoomService roomService,Runnable onSave)
    {
        List<Billing> allBills = service.loadBills();

        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Patient Repository ===\n");

        System.out.println("  [1] New Patient");
        System.out.println("  [2] View All Patients");
        System.out.println("  [3] Search Patient");
        System.out.println("  [4] Discharge Patient");
        System.out.println("  [5] Bill Patient");
        System.out.println("  [6] View Patient Bills");
        System.out.println("  [7] View All Bills");
        System.out.println("  [8] Record Patient Vitals");
        System.out.println("  [9] Administration History");
        System.out.println("  [10] Change Patient Room");
        System.out.print("\n  Select type (or 'q' to return): ");
        String type = scanner.nextLine().trim();
        if (type.equalsIgnoreCase("q")) return;

        if(type.equals(("1")))
        {
            System.out.println("Input the Patient Information below to Admit the Patient");
            System.out.println("Admitting Patient...");

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

        }
        else if(type.equals(("2")))
        {
            System.out.print("\033[H\033[2J\033[3J");
            System.out.flush();
            System.out.println("\n  === All Patients ===\n");

            if (patients.isEmpty()) {
                System.out.println("  No patients in the system.");
            } else {
                for (int i = 0; i < patients.size(); i++) {
                    Patient p = patients.get(i);
                        System.out.println("  [" + (i + 1) + "] " + p.getName()
                            + ", ID: " + p.getPatientId()
                            + ", Room: " + p.getRoomNumber()
                            + ", Diagnosis: " + p.getDiagnosis());
                }

                System.out.print("\n  Select patient number to view details (or 'q' to return): ");
                String selection = scanner.nextLine().trim();
                if (selection.equalsIgnoreCase("q")) {
                    return;
                }

                int selectedIndex;
                try {
                    selectedIndex = Integer.parseInt(selection) - 1;
                } catch (NumberFormatException e) {
                    System.out.println("\n  Invalid selection.");
                    System.out.println("  Press Enter to return to menu...");
                    scanner.nextLine();
                    return;
                }

                if (selectedIndex < 0 || selectedIndex >= patients.size()) {
                    System.out.println("\n  Invalid selection.");
                    System.out.println("  Press Enter to return to menu...");
                    scanner.nextLine();
                    return;
                }

                showPatientDetails(patients.get(selectedIndex));
            }
        }
        else if(type.equals(("3")))
        {
            System.out.print("  Name: ");
            String patientName = scanner.nextLine().trim();
            List<Patient> similarNames = new ArrayList<Patient>();
            for(Patient p : patients)
            {
                if(p.getName().toLowerCase().contains(patientName.toLowerCase()))
                {
                    similarNames.add(p);
                }
            }
            if(similarNames.isEmpty())
            {
                System.out.println("No patient found with that name");
            }
            for(Patient p : similarNames)
            {
                System.out.println(p.toString());
            }
        } else if (type.equals("4")) {
            DischargeService.runDischargeFlow(scanner, patients, roomService, onSave);
            return;
        }
        else if(type.equals("5"))
        {
            // Generating Bill
            Patient selectedPatient = null;
            System.out.println("Select a Patient ID from the following List: \n");
            for(Patient p : patients)
            {
                System.out.println("[" + p.getPatientId() + "] " + p.getName());
            }
            String patientID = scanner.nextLine().trim().toLowerCase();
            System.out.print("  ID: " + patientID);
            for(Patient p2 : patients)
            {
                if(p2.getPatientId().toLowerCase().equals(patientID))
                {
                    selectedPatient = p2;
                    break;
                }
            }
            System.out.println(".................");
            if(selectedPatient == null)
            {
                System.out.println("Patient ID not found");
                System.out.println("  Press Enter to return to menu...");
                scanner.nextLine();
                return;
            }
            System.out.println("Selected: " + selectedPatient.getName());

            System.out.println("Estimated Medical Care Price: ");
            int estimatedPrice = Integer.parseInt(scanner.nextLine().trim());
            System.out.println("List out all the medical care they received: ");
            String medicalCareReceived = scanner.nextLine().trim();
            System.out.println("Should their Medical Insurance (" + selectedPatient.getInsuranceProvider() + ") cover part of the bill? (Yes / No): ");
            String coverBill = scanner.nextLine().trim();
            int insuranceCoverdAmount = 0;

            if(coverBill.toLowerCase().equals("yes"))
            {
                System.out.println("How much should the insurance cover: $");
                insuranceCoverdAmount = Integer.parseInt(scanner.nextLine().trim());
            }
            int billPrice = estimatedPrice - insuranceCoverdAmount;
            System.out.println("Is the patient able to pay the bill in full? (Yes / No)");
            String payInFull = scanner.nextLine().trim();
            if(payInFull.toLowerCase().equals("no"))
            {
                System.out.println("Setting up Automatic Payment Plan...");
                System.out.println("Total Price: " + billPrice + " | 24 Months @ (" + billPrice / 24 + ") Per month");
            }
            System.out.println("Successfully Billed " + selectedPatient.getName() + " for $" + billPrice);
            Billing newBill = new Billing(
                    selectedPatient.getPatientId(),
                    selectedPatient.getName(),
                    medicalCareReceived,
                    selectedPatient.getInsuranceProvider(),
                    payInFull.equalsIgnoreCase(payInFull),
                    billPrice
            );
            allBills.add(newBill);
            service.saveBills(allBills);
        }
        else if(type.equals("6"))
        {
            // Searching
            System.out.println(allBills.stream().count());
            Patient selectedPatient = null;
            System.out.println("Select a Patient ID from the following List: \n");
            for(Patient p : patients)
            {
                System.out.println("[" + p.getPatientId() + "] " + p.getName());
            }
            String patientID = scanner.nextLine().trim().toLowerCase();
            System.out.print("  ID: " + patientID);
            for(Patient p2 : patients)
            {
                if(p2.getPatientId().toLowerCase().equals(patientID))
                {
                    selectedPatient = p2;
                    break;
                }
            }
            System.out.println(".................");
            if(selectedPatient == null)
            {
                System.out.println("Patient ID not found");
                System.out.println("  Press Enter to return to menu...");
                scanner.nextLine();
                return;
            }
            System.out.println("Selected: " + selectedPatient.getName());
            int count = 0;
            for(Billing b : allBills)
            {
                if(b.getPatientId().equalsIgnoreCase(selectedPatient.getPatientId()))
                {
                    count++;
                    System.out.println(b.toString());
                }
            }
            if(count == 0)
            {
                System.out.println("Patient does not have any Bills to pay.");
            }
        }
        else if(type.equals("7"))
        {
            System.out.println("Viewing all Bills:\n");
            for(Billing b : allBills)
            {
                System.out.println(b.toString());
            }
        }
        else if (type.equals("8")) {
            recordPatientVitals(scanner, patients, onSave);
        } else if (type.equals("9")) {
            showAdministrationHistory(scanner, patients, service);
        } else if (type.equals("10")) {
            RoomTransferService.runChangePatientRoomFlow(scanner, patients, roomService, onSave);
            return;
        } else {
            System.out.println("\n  Invalid selection.");
        }
        System.out.println("  Press Enter to return to menu...");
        scanner.nextLine();

    }

    private static void recordPatientVitals(Scanner scanner, List<Patient> patients, Runnable onSave) {
    if (patients.isEmpty()) {
        System.out.println("  No patients in the system.");
        return;
    }
    System.out.println("\n  === Record Patient Vitals ===\n");
    for (int i = 0; i < patients.size(); i++) {
        Patient p = patients.get(i);
        System.out.println("  [" + (i + 1) + "] " + p.getName() + ", ID: " + p.getPatientId() + ", Room: " + p.getRoomNumber());
    }
    System.out.print("\n  Select patient number (or 'q' to return): ");
    String sel = scanner.nextLine().trim();
    if (sel.equalsIgnoreCase("q")) return;

    int idx;
    try {
        idx = Integer.parseInt(sel) - 1;
    } catch (NumberFormatException e) {
        System.out.println("  Invalid selection: must be a number.");
        return;
    }
    if (idx < 0 || idx >= patients.size()) {
        System.out.println("  Invalid selection: out of range.");
        return;
    }

    Patient patient = patients.get(idx);

    boolean abnormal = false;
    // Temperature
    System.out.print("  Temperature (F): ");
    String tempInput = scanner.nextLine().trim();
    double temp;
    try {
        temp = Double.parseDouble(tempInput);
    } catch (NumberFormatException e) {
        System.out.println("  Invalid temperature: must be a decimal number (e.g., 98.6).");
        return;
    }
    if (temp < 86.0 || temp > 113.0) {
        System.out.println("  Invalid temperature: must be between 86.0F and 113.0F.");
        return;
    }
    // Immediate temperature abnormal flag
    objects.PatientVitals tempCheck = new objects.PatientVitals(java.time.LocalDateTime.now(), temp, "120/80", 80, "");
    if (tempCheck.isTemperatureAbnormal(patient.getAge())) {
        System.out.println("  WARNING: Temperature is ABNORMAL for this patient's age group!");
        abnormal = true;
    }
    String feverGrade = tempCheck.getFeverGrade();
    if (!"None".equals(feverGrade)) {
        System.out.println("  Fever detected: " + feverGrade + " fever.");
    }

    // Blood Pressure
    System.out.print("  Blood Pressure (e.g., 120/80): ");
    String bp = scanner.nextLine().trim();
    if (!bp.matches("\\d{2,3}/\\d{2,3}")) {
        System.out.println("  Invalid blood pressure: must be in format systolic/diastolic (e.g., 120/80).");
        return;
    }
    String[] bpParts = bp.split("/");
    int systolic = Integer.parseInt(bpParts[0]);
    int diastolic = Integer.parseInt(bpParts[1]);
    if (systolic < 50 || systolic > 300 || diastolic < 30 || diastolic > 200) {
        System.out.println("  Invalid blood pressure: values out of plausible range.");
        return;
    }
    // Immediate blood pressure abnormal flag
    objects.PatientVitals bpCheck = new objects.PatientVitals(java.time.LocalDateTime.now(), temp, bp, 80, "");
    if (bpCheck.isBloodPressureAbnormal(patient.getAge())) {
        System.out.println("  WARNING: Blood pressure is ABNORMAL for this patient's age group!");
        abnormal = true;
    }

    // Heart Rate
    System.out.print("  Heart Rate (bpm): ");
    String hrInput = scanner.nextLine().trim();
    int hr;
    try {
        hr = Integer.parseInt(hrInput);
    } catch (NumberFormatException e) {
        System.out.println("  Invalid heart rate: must be a whole number.");
        return;
    }
    if (hr < 20 || hr > 300) {
        System.out.println("  Invalid heart rate: must be between 20 and 300 bpm.");
        return;
    }
    // Immediate heart rate abnormal flag
    objects.PatientVitals hrCheck = new objects.PatientVitals(java.time.LocalDateTime.now(), temp, bp, hr, "");
    if (hrCheck.isHeartRateAbnormal(patient.getAge())) {
        System.out.println("  WARNING: Heart rate is ABNORMAL for this patient's age group!");
        abnormal = true;
    }

    // Notes
    System.out.print("  Notes (optional): ");
    String notes = scanner.nextLine().trim();

    boolean dizziness = false, nausea = false, chestPain = false, confusion = false, hasFainted = false, troubleBreathing = false;
    if (abnormal) {
        System.out.println("\n  One or more vitals are abnormal.");
        // Prompt for symptoms
        System.out.println("  Is the patient experiencing any of the following symptoms? (y/n)");
        System.out.print("    Dizziness: ");
        String input = scanner.nextLine().trim();
        dizziness = input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes");
        System.out.print("    Nausea: ");
        input = scanner.nextLine().trim();
        nausea = input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes");
        System.out.print("    Chest Pain: ");
        input = scanner.nextLine().trim();
        chestPain = input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes");
        System.out.print("    Confusion: ");
        input = scanner.nextLine().trim();
        confusion = input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes");
        System.out.print("    Has Fainted: ");
        input = scanner.nextLine().trim();
        hasFainted = input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes");
        System.out.print("    Trouble breathing: ");
        input = scanner.nextLine().trim();
        confusion = input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes");
    }

    objects.PatientVitals vitals = new objects.PatientVitals(
        java.time.LocalDateTime.now(), temp, bp, hr, notes, dizziness, nausea, chestPain, confusion, hasFainted, troubleBreathing
    );
    patient.addVitals(vitals);
    onSave.run();
    System.out.println("  Vitals recorded for " + patient.getName() + ".");

}

    private static void showAdministrationHistory(Scanner scanner, List<Patient> patients, PatientsService service) {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Administration History ===\n");

        if (patients.isEmpty()) {
            System.out.println("  No patients in the system.");
            System.out.println("  Press Enter to return to menu...");
            scanner.nextLine();
            return;
        }

        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);
            System.out.println("  [" + (i + 1) + "] " + p.getName() + "  (ID: " + p.getPatientId() + ")");
        }

        System.out.print("\n  Select patient (or 'q' to return): ");
        String sel = scanner.nextLine().trim();
        if (sel.equalsIgnoreCase("q")) return;

        int idx;
        try {
            idx = Integer.parseInt(sel) - 1;
        } catch (NumberFormatException e) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to return to menu...");
            scanner.nextLine();
            return;
        }
        if (idx < 0 || idx >= patients.size()) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to return to menu...");
            scanner.nextLine();
            return;
        }

        Patient patient = patients.get(idx);
        List<String[]> history = service.getAdministrationHistory(patient.getPatientId());

        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Administration History — " + patient.getName() + " ===\n");

        if (history.isEmpty()) {
            System.out.println("  No administrations recorded for this patient.");
        } else {
            // columns: LogId[0], PrescriptionId[1], PatientId[2], PatientName[3],
            //          MedicationName[4], Dosage[5], AmountAdministered[6], AdministeredBy[7], Timestamp[8]
            for (int i = 0; i < history.size(); i++) {
                String[] row = history.get(i);
                System.out.println("  [" + (i + 1) + "] " + row[8]
                        + "  |  " + row[4]
                        + "  —  " + row[5]
                        + "  |  Amount: " + row[6]
                        + "  |  By: " + row[7]
                        + "  |  Rx: " + row[1]);
            }
        }
    }

    private List<String[]> getAdministrationHistory(String patientId) {
        List<String[]> result = new ArrayList<>();
        try {
            if (!Files.exists(administrationLogPath)) return result;
            List<String> lines = Files.readAllLines(administrationLogPath);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] parts = parseCsvLine(line, 9);
                if (parts.length >= 9 && parts[2].equalsIgnoreCase(patientId)) {
                    result.add(parts);
                }
            }
        } catch (IOException e) {
            System.out.println("  Warning: could not read administration log: " + e.getMessage());
        }
        return result;
    }

    private static void showPatientDetails(Patient patient) {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Patient Details ===\n");
        System.out.println("  Name: " + patient.getName());
        System.out.println("  ID: " + patient.getPatientId());
        System.out.println("  Room: " + patient.getRoomNumber());
        System.out.println("  Diagnosis: " + patient.getDiagnosis());
        System.out.println("  Status: " + patient.getStatus());

        List<objects.PatientVitals> vitalsHistory = patient.getVitalsHistory();
        System.out.println("\n  Vitals History:");
        if (vitalsHistory.isEmpty()) {
            System.out.println("  No vitals recorded yet.");
            return;
        }

        for (int i = 0; i < vitalsHistory.size(); i++) {
            objects.PatientVitals v = vitalsHistory.get(i);
            System.out.println("  [" + (i + 1) + "] " + v.getTimestamp());
            System.out.println("      Temperature: " + v.getTemperature() + " F");
            System.out.println("      Blood Pressure: " + v.getBloodPressure());
            System.out.println("      Heart Rate: " + v.getHeartRate() + " bpm");
            if (!v.getNotes().isBlank()) {
                System.out.println("      Notes: " + v.getNotes());
            }
        }
    }


    private List<Billing> loadBills() {
        ArrayList<Billing> patientBills = new ArrayList<>();
        try {
            if (!Files.exists(patientBillingPath)) return patientBills;
            List<String> lines = Files.readAllLines(patientBillingPath);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] parts = parseCsvLine(line, 6);
                if (parts.length < 6) continue;
                Billing bill = new Billing(
                        parts[0],
                        parts[1],
                        parts[2],
                        parts[3],
                        Boolean.parseBoolean(parts[4]),
                        Integer.parseInt(parts[5].trim())
                );
                patientBills.add(bill);
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error loading Billing: " + e.getMessage());
        }
        return patientBills;
    }

    public void saveBills(List<Billing> bills) {
        List<String> lines = new ArrayList<>();
        lines.add("PatientID,PatientName,Description,IsPaid,Amount");

        for (Billing b : bills) {
            lines.add(String.join(",",
                    escapeCsv(b.getPatientId()),
                    escapeCsv(b.getName()),
                    escapeCsv(b.getMedicalInfo()),
                    escapeCsv(b.getInsuranceProvider()),
                    Boolean.toString(b.getPayInFull()),
                    Integer.toString(b.getBill())
            ));
        }

        try {
            Files.write(patientBillingPath, lines);
        } catch (IOException e) {
            System.err.println("Error saving bills: " + e.getMessage());
        }
    }

    private String escapeCsv(String data) {
        if (data == null) return "";
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            return "\"" + data.replace("\"", "\"\"") + "\"";
        }
        return data;
    }

    private String[] parseCsvLine(String line, int expectedCols) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().replace("\"\"", "\""));
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString().replace("\"\"", "\""));
        while (result.size() < expectedCols) result.add("");
        return result.toArray(new String[0]);
    }

    public void addPatient()
    {

    }

}
