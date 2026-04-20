package services;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import objects.Patient;

public class PatientsService {
    private static String patientIDTag = "P";
    public static void createService(
            Scanner scanner,
            List<Patient> patients,
            RoomService roomService,
            Runnable onSave
    )
    {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Patient Repository ===\n");

        System.out.println("  [1] New Patient");
        System.out.println("  [2] View All Patients");
        System.out.println("  [3] Search Patient");
        System.out.println("  [4] Discharge Patient");
        System.out.println("  [5] Record Patient Vitals");
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
        else if (type.equals("5")) {
            recordPatientVitals(scanner, patients, onSave);
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
            System.out.println("  Invalid selection.");
            return;
        }
        if (idx < 0 || idx >= patients.size()) {
            System.out.println("  Invalid selection.");
            return;
        }
        Patient patient = patients.get(idx);
        try {
            System.out.print("  Temperature (C): ");
            double temp = Double.parseDouble(scanner.nextLine().trim());
            System.out.print("  Blood Pressure (e.g., 120/80): ");
            String bp = scanner.nextLine().trim();
            System.out.print("  Heart Rate (bpm): ");
            int hr = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("  Notes (optional): ");
            String notes = scanner.nextLine().trim();
            objects.PatientVitals vitals = new objects.PatientVitals(java.time.LocalDateTime.now(), temp, bp, hr, notes);
            patient.addVitals(vitals);
            onSave.run();
            System.out.println("  Vitals recorded for " + patient.getName() + ".");
        } catch (Exception e) {
            System.out.println("  Invalid input. Vitals not recorded.");
        }
    }
}
