package services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import objects.Patient;

public class PharmacyService {
    private final Path inventoryFile;
    private final Path prescriptionsFile;

    public PharmacyService(Path dataDir) {
        this.inventoryFile = dataDir.resolve("pharmacy_inventory.csv");
        this.prescriptionsFile = dataDir.resolve("prescriptions.csv");
    }

    public void initializeFiles() {
        try {
            if (!Files.exists(inventoryFile)) {
                Files.createFile(inventoryFile);
            }
            if (!Files.exists(prescriptionsFile)) {
                Files.createFile(prescriptionsFile);
            }
        } catch (IOException e) {
            System.out.println("Error initializing pharmacy files: " + e.getMessage());
        }
    }

    public void prescribeMedication(Scanner scanner, List<Patient> patients, Runnable savePatientsCallback) {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Prescribe Medication (Doctor) ===\n");

        List<Patient> admittedPatients = new ArrayList<>();
        for (Patient p : patients) {
            if ("admitted".equalsIgnoreCase(p.getStatus())) {
                admittedPatients.add(p);
            }
        }

        if (admittedPatients.isEmpty()) {
            System.out.println("  No admitted patients.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        System.out.println("  Select Patient:");
        for (int i = 0; i < admittedPatients.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + admittedPatients.get(i).getName() + " (ID: " + admittedPatients.get(i).getPatientId() + ")");
        }
        System.out.print("  Choice (or 'q' to return): ");
        String pChoice = scanner.nextLine().trim();
        if (pChoice.equalsIgnoreCase("q")) return;
        int pIndex;
        try {
            pIndex = Integer.parseInt(pChoice) - 1;
        } catch (NumberFormatException e) {
            System.out.println("  Invalid selection.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        if (pIndex < 0 || pIndex >= admittedPatients.size()) {
            System.out.println("  Invalid selection.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        Patient selectedPatient = admittedPatients.get(pIndex);

        System.out.println("\n  === Medical Profile ===");
        System.out.println("  Name: " + selectedPatient.getName());
        System.out.println("  ID: " + selectedPatient.getPatientId());
        System.out.println("  Diagnosis: " + selectedPatient.getDiagnosis());
        System.out.println("  Allergies: " + (selectedPatient.getAllergies().isEmpty() ? "None" : String.join(", ", selectedPatient.getAllergies())));
        System.out.println("  Active Meds: " + (selectedPatient.getActiveMedications().isEmpty() ? "None" : String.join(", ", selectedPatient.getActiveMedications())));

        Map<String, Integer> inventory = loadInventory();
        List<Prescription> prescriptions = loadPrescriptions();

        while (true) {
            System.out.println("\n  Available Medications:");
            for (String med : inventory.keySet()) {
                System.out.println("  - " + med);
            }
            System.out.print("\n  Type medication to prescribe (or 'cancel'): ");
            String selectedMedication = scanner.nextLine().trim();
            if (selectedMedication.equalsIgnoreCase("cancel")) return;

            String foundMedication = null;
            for (String med : inventory.keySet()) {
                if (med.equalsIgnoreCase(selectedMedication)) {
                    foundMedication = med;
                    break;
                }
            }

            if (foundMedication == null || inventory.getOrDefault(foundMedication, 0) <= 0) {
                System.out.println("\n  [ALERT] Medication '" + selectedMedication + "' is OUT OF STOCK or not found.");
                System.out.println("  Available equivalent/alternative medications:");
                List<String> alternatives = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
                    if (entry.getValue() > 0) alternatives.add(entry.getKey());
                }
                if (alternatives.isEmpty()) {
                    System.out.println("  No medications available. Cannot prescribe.");
                    System.out.println("  Press Enter to return...");
                    scanner.nextLine();
                    return;
                }
                for (int i = 0; i < alternatives.size(); i++) {
                    System.out.println("  [" + (i + 1) + "] " + alternatives.get(i));
                }
                System.out.print("  Select alternative number (or 0 to search again, 'q' to return): ");
                String aChoice = scanner.nextLine().trim();
                if (aChoice.equalsIgnoreCase("q")) return;
                int aIndex;
                try {
                    aIndex = Integer.parseInt(aChoice) - 1;
                } catch (NumberFormatException e) { continue; }
                if (aIndex < 0 || aIndex >= alternatives.size()) continue;
                foundMedication = alternatives.get(aIndex);
                System.out.println("  Selected alternative: " + foundMedication);
            }

            System.out.print("  Dosage (e.g., 500mg): "); String dosage = scanner.nextLine().trim();
            System.out.print("  Route (e.g., Oral): "); String route = scanner.nextLine().trim();
            System.out.print("  Frequency (e.g., Twice daily): "); String freq = scanner.nextLine().trim();
            System.out.print("  Duration (e.g., 7 days): "); String duration = scanner.nextLine().trim();
            System.out.print("  Quantity to Dispense: ");
            int qty;
            try {
                qty = Integer.parseInt(scanner.nextLine().trim());
            } catch (Exception e) {
                System.out.println("  Invalid quantity.");
                continue;
            }

            boolean conflict = false;
            for (String allergy : selectedPatient.getAllergies()) {
                if (allergy.equalsIgnoreCase(foundMedication)) conflict = true;
            }
            for (String activeMed : selectedPatient.getActiveMedications()) {
                if (activeMed.equalsIgnoreCase(foundMedication)) conflict = true;
            }

            if (conflict) {
                System.out.println("\n  [CRITICAL WARNING] DANGEROUS CONFLICT DETECTED!");
                System.out.println("  Medication conflicts with allergies or active medications.");
                System.out.print("  Press Enter to acknowledge and abort draft...");
                scanner.nextLine();
                continue;
            }

            System.out.println("\n  === Review Prescription ===");
            String inst = dosage + ", " + route + ", " + freq + " for " + duration;
            System.out.println("  Medication: " + foundMedication);
            System.out.println("  Instructions: " + inst);
            System.out.println("  Quantity to Dispense: " + qty);
            System.out.print("  Submit? (y/n): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                List<String> meds = new ArrayList<>(selectedPatient.getActiveMedications());
                meds.add(foundMedication);
                selectedPatient.setActiveMedications(meds);
                savePatientsCallback.run();

                String rxId = "RX" + System.currentTimeMillis();
                Prescription newRx = new Prescription(rxId, selectedPatient.getPatientId(), selectedPatient.getName(), foundMedication, inst, qty, "pending", LocalDate.now().plusDays(30));
                prescriptions.add(newRx);
                savePrescriptions(prescriptions);

                System.out.println("\n  [SUCCESS] Prescription submitted and routed to Pharmacy.");
                System.out.println("  Medical file updated.");
                System.out.print("  Press Enter to return...");
                scanner.nextLine();
                return;
            } else {
                System.out.println("  Draft aborted.");
            }
        }
    }

    public void dispensePrescribedMedication(Scanner scanner, List<Patient> admittedPatients) {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Dispense Prescribed Medication ===\n");

        List<Prescription> prescriptions = loadPrescriptions();
        Map<String, Integer> inventory = loadInventory();

        // Precondition: only show prescriptions whose patient is currently admitted
        // and whose medication exists in inventory (quantity > 0).
        ArrayList<Prescription> pending = new ArrayList<>();
        for (Prescription p : prescriptions) {
            if (!"pending".equalsIgnoreCase(p.status)) {
                continue;
            }
            boolean admitted = false;
            for (Patient patient : admittedPatients) {
                if (patient.getPatientId().equalsIgnoreCase(p.patientId)) {
                    admitted = true;
                    break;
                }
            }
            if (!admitted) {
                continue;
            }
            int available = inventory.getOrDefault(p.medicationName, 0);
            if (available <= 0) {
                continue;
            }
            pending.add(p);
        }

        if (pending.isEmpty()) {
            System.out.println("  No pending prescriptions.");
            System.out.println("\n  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        System.out.println("  Pending Prescriptions:");
        for (int i = 0; i < pending.size(); i++) {
            Prescription p = pending.get(i);
            System.out.println("  [" + (i + 1) + "] Rx " + p.prescriptionId
                + ", Patient: " + p.patientName
                + ", Medication: " + p.medicationName
                + ", Qty: " + p.requiredQuantity
                + ", Expires: " + p.expiryDate);
        }

        System.out.print("\n  Select prescription number to process (or 'q' to return): ");
        String selection = scanner.nextLine().trim();
        if (selection.equalsIgnoreCase("q")) return;

        int index;
        try {
            index = Integer.parseInt(selection) - 1;
        } catch (NumberFormatException ex) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        if (index < 0 || index >= pending.size()) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        Prescription selected = pending.get(index);

        System.out.println("\n  Selected Prescription:");
        System.out.println("  Medication: " + selected.medicationName);
        System.out.println("  Dosage: " + selected.dosage);
        System.out.println("  Required Quantity: " + selected.requiredQuantity);

        LocalDate today = LocalDate.now();
        if (selected.expiryDate != null && selected.expiryDate.isBefore(today)) {
            System.out.println("\n  Warning: Prescription is expired.");
            System.out.print("  Abort fulfillment and mark as invalid? (y/n): ");
            String abort = scanner.nextLine().trim();
            if (abort.equalsIgnoreCase("y")) {
                selected.status = "invalid";
                savePrescriptions(prescriptions);
                System.out.println("\n  Prescription marked invalid. Notify Medical Services for a new prescription.");
            } else {
                System.out.println("\n  No changes saved.");
            }
            System.out.println("\n  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        int available = inventory.getOrDefault(selected.medicationName, 0);
        System.out.println("  Inventory Available: " + available);

        if (available < selected.requiredQuantity) {
            System.out.println("\n  Insufficient inventory. Fulfillment canceled.");
            System.out.println("  Prescription remains pending. Initiate pharmacy supply ordering process.");
            System.out.println("\n  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        System.out.print("\n  Confirm medication is prepared and ready? (y/n): ");
        String ready = scanner.nextLine().trim();
        if (!ready.equalsIgnoreCase("y")) {
            System.out.println("\n  Fulfillment canceled. Prescription remains pending.");
            System.out.println("\n  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        inventory.put(selected.medicationName, available - selected.requiredQuantity);
        selected.status = "fulfilled";

        saveInventory(inventory);
        savePrescriptions(prescriptions);

        System.out.println("\n  Fulfillment confirmed.");
        System.out.println("  Inventory updated and prescription marked fulfilled.");
        System.out.println("\n  Press Enter to return...");
        scanner.nextLine();
    }

    private List<Prescription> loadPrescriptions() {
        ArrayList<Prescription> prescriptions = new ArrayList<>();
        try {
            if (!Files.exists(prescriptionsFile)) {
                return prescriptions;
            }
            List<String> lines = Files.readAllLines(prescriptionsFile);
            for (int i = 1; i < lines.size(); i++) { // skip header
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] parts = parseCsvLine(line, 8);
                if (parts.length != 8) continue;
                LocalDate expiry = null;
                if (!parts[7].isBlank()) {
                    try {
                        expiry = LocalDate.parse(parts[7]);
                    } catch (DateTimeParseException ignored) {
                        expiry = null;
                    }
                }
                Prescription p = new Prescription(
                        parts[0],
                        parts[1],
                        parts[2],
                        parts[3],
                        parts[4],
                        Integer.parseInt(parts[5]),
                        parts[6],
                        expiry
                );
                prescriptions.add(p);
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error loading prescriptions: " + e.getMessage());
        }
        return prescriptions;
    }

    private Map<String, Integer> loadInventory() {
        HashMap<String, Integer> inventory = new HashMap<>();
        try {
            if (!Files.exists(inventoryFile)) {
                return inventory;
            }
            List<String> lines = Files.readAllLines(inventoryFile);
            for (int i = 1; i < lines.size(); i++) { // skip header
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] parts = parseCsvLine(line, 2);
                if (parts.length != 2) continue;
                inventory.put(parts[0], Integer.parseInt(parts[1]));
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error loading inventory: " + e.getMessage());
        }
        return inventory;
    }

    private void savePrescriptions(List<Prescription> prescriptions) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("PrescriptionId,PatientId,PatientName,MedicationName,Dosage,RequiredQuantity,Status,ExpiryDate");
        for (Prescription p : prescriptions) {
            lines.add(String.join(",",
                    escapeCsv(p.prescriptionId),
                    escapeCsv(p.patientId),
                    escapeCsv(p.patientName),
                    escapeCsv(p.medicationName),
                    escapeCsv(p.dosage),
                    Integer.toString(p.requiredQuantity),
                    escapeCsv(p.status),
                    p.expiryDate == null ? "" : p.expiryDate.toString()
            ));
        }
        try {
            Files.write(prescriptionsFile, lines);
        } catch (IOException e) {
            System.out.println("Error saving prescriptions: " + e.getMessage());
        }
    }

    private void saveInventory(Map<String, Integer> inventory) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Medication,Quantity");
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            lines.add(escapeCsv(entry.getKey()) + "," + entry.getValue());
        }
        try {
            Files.write(inventoryFile, lines);
        } catch (IOException e) {
            System.out.println("Error saving inventory: " + e.getMessage());
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

    private static class Prescription {
        String prescriptionId;
        String patientId;
        String patientName;
        String medicationName;
        String dosage;
        int requiredQuantity;
        String status;
        LocalDate expiryDate;

        Prescription(String prescriptionId, String patientId, String patientName,
                     String medicationName, String dosage, int requiredQuantity,
                     String status, LocalDate expiryDate) {
            this.prescriptionId = prescriptionId;
            this.patientId = patientId;
            this.patientName = patientName;
            this.medicationName = medicationName;
            this.dosage = dosage;
            this.requiredQuantity = requiredQuantity;
            this.status = status;
            this.expiryDate = expiryDate;
        }
    }
}
