package services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final Path purchaseOrdersFile;
    private final Path administrationLogFile;

    public PharmacyService(Path dataDir) {
        this.inventoryFile = dataDir.resolve("pharmacy_inventory.csv");
        this.prescriptionsFile = dataDir.resolve("prescriptions.csv");
        this.purchaseOrdersFile = dataDir.resolve("purchase_orders.csv");
        this.administrationLogFile = dataDir.resolve("medication_administrations.csv");
    }

    public void initializeFiles() {
        try {
            if (!Files.exists(inventoryFile)) {
                Files.createFile(inventoryFile);
            }
            if (!Files.exists(prescriptionsFile)) {
                Files.createFile(prescriptionsFile);
            }
            if (!Files.exists(purchaseOrdersFile)) {
                Files.createFile(purchaseOrdersFile);
            }
            if (!Files.exists(administrationLogFile)) {
                Files.write(administrationLogFile,
                    java.util.List.of("LogId,PrescriptionId,PatientId,PatientName,MedicationName,Dosage,AmountAdministered,AdministeredBy,Timestamp"));
            }
        } catch (IOException e) {
            System.out.println("Error initializing pharmacy files: " + e.getMessage());
        }
    }

    public void auditMedicationInventory(Scanner scanner) {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Audit Medication Inventory ===\n");

        Map<String, InventoryItem> inventory = loadInventoryItems();
        if (inventory.isEmpty()) {
            System.out.println("  No medications found in inventory.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        List<String> medicationNames = new ArrayList<>(inventory.keySet());
        medicationNames.sort(String.CASE_INSENSITIVE_ORDER);

        System.out.println("  Current Inventory:");
        for (String medication : medicationNames) {
            InventoryItem item = inventory.get(medication);
            System.out.println("  - " + medication + " | Current Stock: " + item.quantity);
        }

        System.out.println("\n  Press Enter to return...");
        scanner.nextLine();
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
                Prescription newRx = new Prescription(rxId, selectedPatient.getPatientId(), selectedPatient.getName(), foundMedication, inst, qty, 0, "pending", LocalDate.now().plusDays(30));
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

    public void orderPharmacySupplies(Scanner scanner) {
        Map<String, InventoryItem> inventory = loadInventoryItems();
        List<String> medicationNames = new ArrayList<>(inventory.keySet());
        medicationNames.sort(String.CASE_INSENSITIVE_ORDER);

        String selectedMedication = null;

        while (selectedMedication == null) {
            System.out.print("\033[H\033[2J\033[3J");
            System.out.flush();
            System.out.println("\n  === Order Pharmacy Supplies ===\n");
            System.out.println("  Available Medications to Order:");
            if (medicationNames.isEmpty()) {
                System.out.println("  None in current inventory.");
            } else {
                for (int i = 0; i < medicationNames.size(); i++) {
                    String med = medicationNames.get(i);
                    InventoryItem item = inventory.get(med);
                    System.out.println("  [" + (i + 1) + "] " + med + " (Current Stock: " + item.quantity + ")");
                }
            }

            System.out.println("  [c] Order Custom New Medication");
            System.out.println("\n  Select an option, or type 'search' to find a medication.");
            System.out.print("  Choice (or 'q' to return): ");
            String choice = scanner.nextLine().trim();

            if (choice.equalsIgnoreCase("q")) {
                return;
            }

            if (choice.equalsIgnoreCase("c")) {
                System.out.print("  Enter new medication name: ");
                String customName = scanner.nextLine().trim();
                if (customName.isBlank()) {
                    System.out.println("  [ERROR] Medication name cannot be blank.");
                    continue;
                }
                selectedMedication = customName;
                break;
            }

            if (choice.equalsIgnoreCase("search")) {
                System.out.print("  Enter medication name to search: ");
                String searchName = scanner.nextLine().trim();
                String foundMed = null;
                for (String med : medicationNames) {
                    if (med.equalsIgnoreCase(searchName)) {
                        foundMed = med;
                        break;
                    }
                }
                
                if (foundMed != null) {
                    InventoryItem foundItem = inventory.get(foundMed);
                    System.out.println("\n  Medication Details:");
                    System.out.println("  Name: " + foundMed);
                    System.out.println("  Current Stock: " + foundItem.quantity);
                    selectedMedication = foundMed;
                } else {
                    System.out.println("  [ERROR] Medication not found in inventory. Use 'c' to order a custom medication.");
                    System.out.print("  Press Enter to continue...");
                    scanner.nextLine();
                }
                continue;
            }

            try {
                int index = Integer.parseInt(choice) - 1;
                if (index >= 0 && index < medicationNames.size()) {
                    selectedMedication = medicationNames.get(index);
                } else {
                    System.out.println("  [ERROR] Invalid selection.");
                    System.out.print("  Press Enter to continue...");
                    scanner.nextLine();
                }
            } catch (NumberFormatException e) {
                System.out.println("  [ERROR] Invalid input.");
                System.out.print("  Press Enter to continue...");
                scanner.nextLine();
            }
        }

        int orderQuantity = 0;
        while (true) {
            System.out.print("\n  Enter desired order quantity for " + selectedMedication + ": ");
            String qtyInput = scanner.nextLine().trim();
            try {
                orderQuantity = Integer.parseInt(qtyInput);
                if (orderQuantity <= 0) {
                    System.out.println("  [ERROR] Order quantity must be a positive integer.");
                    System.out.println("  Please enter a valid positive integer.");
                    continue;
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("  [ERROR] Order quantity must be a numeric value.");
                System.out.println("  Please enter a valid positive integer.");
            }
        }

        System.out.println("\n  === Order Summary ===");
        System.out.println("  Medication: " + selectedMedication);
        System.out.println("  Quantity: " + orderQuantity);
        System.out.print("  Confirm and submit supply order? (y/n): ");
        String confirm = scanner.nextLine().trim();

        if (confirm.equalsIgnoreCase("y")) {
            String orderId = "PO" + System.currentTimeMillis();
            savePurchaseOrder(orderId, selectedMedication, orderQuantity);
            System.out.println("\n  [SUCCESS] Order confirmation: Purchase order " + orderId + " has been generated and logged as a pending delivery.");
        } else {
            System.out.println("\n  Order canceled.");
        }

        System.out.print("\n  Press Enter to return...");
        scanner.nextLine();
    }

    public void receivePharmacyDelivery(Scanner scanner) {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Receive Pharmacy Delivery ===\n");

        List<PurchaseOrder> orders = loadPurchaseOrders();
        List<PurchaseOrder> pendingOrders = new ArrayList<>();

        for (PurchaseOrder po : orders) {
            if ("Pending Delivery".equalsIgnoreCase(po.status)) {
                pendingOrders.add(po);
            }
        }

        if (pendingOrders.isEmpty()) {
            System.out.println("  No pending deliveries at this time.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        System.out.println("  Pending Deliveries:");
        for (int i = 0; i < pendingOrders.size(); i++) {
            PurchaseOrder po = pendingOrders.get(i);
            System.out.println("  [" + (i + 1) + "] PO ID: " + po.orderId 
                + " | Medication: " + po.medicationName 
                + " | Qty: " + po.quantity);
        }

        System.out.print("\n  Select a delivery to receive by number (or 'q' to return): ");
        String choice = scanner.nextLine().trim();

        if (choice.equalsIgnoreCase("q")) {
            return;
        }

        try {
            int index = Integer.parseInt(choice) - 1;
            if (index >= 0 && index < pendingOrders.size()) {
                PurchaseOrder selected = pendingOrders.get(index);
                System.out.println("\n  === Confirm Delivery ===");
                System.out.println("  Order ID: " + selected.orderId);
                System.out.println("  Medication: " + selected.medicationName);
                System.out.println("  Quantity: " + selected.quantity);
                System.out.print("  Confirm items received and add to inventory? (y/n): ");
                
                String confirm = scanner.nextLine().trim();
                if (confirm.equalsIgnoreCase("y")) {
                    Map<String, Integer> inventory = loadInventory();
                    int currentStock = inventory.getOrDefault(selected.medicationName, 0);
                    inventory.put(selected.medicationName, currentStock + selected.quantity);
                    saveInventory(inventory);
                    
                    selected.status = "Delivered";
                    saveAllPurchaseOrders(orders);
                    
                    System.out.println("\n  [SUCCESS] Delivery processed. Inventory updated.");
                } else {
                    System.out.println("\n  Delivery receipt canceled.");
                }
            } else {
                System.out.println("  [ERROR] Invalid selection.");
            }
        } catch (NumberFormatException e) {
            System.out.println("  [ERROR] Invalid input.");
        }

        System.out.print("\n  Press Enter to return...");
        scanner.nextLine();
    }

    private List<PurchaseOrder> loadPurchaseOrders() {
        List<PurchaseOrder> orders = new ArrayList<>();
        try {
            if (!Files.exists(purchaseOrdersFile)) {
                return orders;
            }
            List<String> lines = Files.readAllLines(purchaseOrdersFile);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] parts = parseCsvLine(line, 4);
                if (parts.length >= 4) {
                    orders.add(new PurchaseOrder(parts[0], parts[1], Integer.parseInt(parts[2]), parts[3]));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error loading purchase orders: " + e.getMessage());
        }
        return orders;
    }

    private void savePurchaseOrder(String orderId, String medicationName, int quantity) {
        List<PurchaseOrder> orders = loadPurchaseOrders();
        orders.add(new PurchaseOrder(orderId, medicationName, quantity, "Pending Delivery"));
        saveAllPurchaseOrders(orders);
    }

    private void saveAllPurchaseOrders(List<PurchaseOrder> orders) {
        List<String> lines = new ArrayList<>();
        lines.add("OrderId,MedicationName,Quantity,Status");
        for (PurchaseOrder po : orders) {
            lines.add(String.join(",", escapeCsv(po.orderId), escapeCsv(po.medicationName), Integer.toString(po.quantity), escapeCsv(po.status)));
        }
        try {
            Files.write(purchaseOrdersFile, lines);
        } catch (IOException e) {
            System.out.println("Error saving purchase orders: " + e.getMessage());
        }
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
                String[] parts = parseCsvLine(line, 9);
                if (parts.length < 8) continue;
                LocalDate expiry = null;
                if (!parts[7].isBlank()) {
                    try {
                        expiry = LocalDate.parse(parts[7]);
                    } catch (DateTimeParseException ignored) {
                        expiry = null;
                    }
                }
                int administered = 0;
                if (parts.length > 8 && !parts[8].isBlank()) {
                    try { administered = Integer.parseInt(parts[8]); } catch (NumberFormatException ignored) {}
                }
                Prescription p = new Prescription(
                        parts[0],
                        parts[1],
                        parts[2],
                        parts[3],
                        parts[4],
                        Integer.parseInt(parts[5]),
                        administered,
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
        Map<String, InventoryItem> inventoryItems = loadInventoryItems();
        HashMap<String, Integer> inventory = new HashMap<>();
        for (Map.Entry<String, InventoryItem> entry : inventoryItems.entrySet()) {
            inventory.put(entry.getKey(), entry.getValue().quantity);
        }
        return inventory;
    }

    private Map<String, InventoryItem> loadInventoryItems() {
        HashMap<String, InventoryItem> inventory = new HashMap<>();
        try {
            if (!Files.exists(inventoryFile)) {
                return inventory;
            }
            List<String> lines = Files.readAllLines(inventoryFile);
            if (lines.isEmpty()) {
                return inventory;
            }
            for (int i = 1; i < lines.size(); i++) { // skip header
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] parts = parseCsvLine(line, 2);
                if (parts.length < 2) continue;
                int quantity = Integer.parseInt(parts[1]);
                inventory.put(parts[0], new InventoryItem(quantity));
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error loading inventory: " + e.getMessage());
        }
        return inventory;
    }

    private void savePrescriptions(List<Prescription> prescriptions) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("PrescriptionId,PatientId,PatientName,MedicationName,Dosage,RequiredQuantity,Status,ExpiryDate,AdministeredQuantity");
        for (Prescription p : prescriptions) {
            lines.add(String.join(",",
                    escapeCsv(p.prescriptionId),
                    escapeCsv(p.patientId),
                    escapeCsv(p.patientName),
                    escapeCsv(p.medicationName),
                    escapeCsv(p.dosage),
                    Integer.toString(p.requiredQuantity),
                    escapeCsv(p.status),
                    p.expiryDate == null ? "" : p.expiryDate.toString(),
                    Integer.toString(p.administeredQuantity)
            ));
        }
        try {
            Files.write(prescriptionsFile, lines);
        } catch (IOException e) {
            System.out.println("Error saving prescriptions: " + e.getMessage());
        }
    }

    private void saveInventory(Map<String, Integer> inventory) {
        Map<String, InventoryItem> existingItems = loadInventoryItems();
        HashMap<String, InventoryItem> updatedItems = new HashMap<>();
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            updatedItems.put(entry.getKey(), new InventoryItem(entry.getValue()));
        }
        saveInventoryItems(updatedItems);
    }

    private void saveInventoryItems(Map<String, InventoryItem> inventory) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Medication,Quantity");

        List<String> medicationNames = new ArrayList<>(inventory.keySet());
        medicationNames.sort(String.CASE_INSENSITIVE_ORDER);

        for (String medication : medicationNames) {
            InventoryItem item = inventory.get(medication);
            lines.add(String.join(",", escapeCsv(medication), Integer.toString(item.quantity)));
        }
        try {
            Files.write(inventoryFile, lines);
        } catch (IOException e) {
            System.out.println("Error saving inventory: " + e.getMessage());
        }
    }

    /**
     * Returns fully administered prescriptions for a patient as
     * [prescriptionId, medicationName, dosage, requiredQuantity, administeredQuantity] arrays.
     */
    public List<String[]> getAdministeredPrescriptions(String patientId) {
        List<Prescription> prescriptions = loadPrescriptions();
        List<String[]> result = new ArrayList<>();
        for (Prescription p : prescriptions) {
            if (p.patientId.equalsIgnoreCase(patientId) && "administered".equalsIgnoreCase(p.status)) {
                result.add(new String[]{
                    p.prescriptionId,
                    p.medicationName,
                    p.dosage,
                    Integer.toString(p.requiredQuantity),
                    Integer.toString(p.administeredQuantity)
                });
            }
        }
        return result;
    }

    /**
     * Creates a new pending prescription (refill) based on an existing administered prescription.
     * Returns true if the original was found and the refill was saved.
     */
    public boolean createRefillRequest(String originalPrescriptionId) {
        List<Prescription> prescriptions = loadPrescriptions();
        Prescription original = null;
        for (Prescription p : prescriptions) {
            if (p.prescriptionId.equals(originalPrescriptionId)) {
                original = p;
                break;
            }
        }
        if (original == null) return false;
        String newRxId = "RX" + System.currentTimeMillis();
        Prescription refill = new Prescription(
                newRxId,
                original.patientId,
                original.patientName,
                original.medicationName,
                original.dosage,
                original.requiredQuantity,
                0,
                "pending",
                LocalDate.now().plusDays(30)
        );
        prescriptions.add(refill);
        savePrescriptions(prescriptions);
        return true;
    }

    /**
     * Returns fulfilled prescriptions for a patient as
     * [prescriptionId, medicationName, dosage, requiredQuantity, administeredQuantity] arrays.
     */
    public List<String[]> getAdministerablePrescriptions(String patientId) {
        List<Prescription> prescriptions = loadPrescriptions();
        List<String[]> result = new ArrayList<>();
        for (Prescription p : prescriptions) {
            if (p.patientId.equalsIgnoreCase(patientId) && "fulfilled".equalsIgnoreCase(p.status)) {
                result.add(new String[]{
                    p.prescriptionId,
                    p.medicationName,
                    p.dosage,
                    Integer.toString(p.requiredQuantity),
                    Integer.toString(p.administeredQuantity)
                });
            }
        }
        return result;
    }

    /**
     * Appends one row to medication_administrations.csv for audit/record purposes.
     */
    public void logAdministration(String prescriptionId, String patientId, String patientName,
            String medicationName, String dosage, int amount, String administeredBy) {
        try {
            String logId = "ADM" + System.currentTimeMillis();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String line = String.join(",",
                    escapeCsv(logId),
                    escapeCsv(prescriptionId),
                    escapeCsv(patientId),
                    escapeCsv(patientName),
                    escapeCsv(medicationName),
                    escapeCsv(dosage),
                    Integer.toString(amount),
                    escapeCsv(administeredBy),
                    escapeCsv(timestamp)
            );
            Files.write(administrationLogFile, java.util.List.of(line), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("  Warning: could not write administration log: " + e.getMessage());
        }
    }

    /**
     * Records that {@code amount} units were administered for the given prescription.
     * Status is set to "administered" once administeredQuantity >= requiredQuantity.
     */
    public void administerDose(String prescriptionId, int amount) {
        List<Prescription> prescriptions = loadPrescriptions();
        for (Prescription p : prescriptions) {
            if (p.prescriptionId.equals(prescriptionId)) {
                p.administeredQuantity += amount;
                if (p.administeredQuantity >= p.requiredQuantity) {
                    p.status = "administered";
                }
                break;
            }
        }
        savePrescriptions(prescriptions);
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
        // Pad with empty strings if missing columns
        while (result.size() < expectedCols) result.add("");
        return result.toArray(new String[0]);
    }

    private static class PurchaseOrder {
        String orderId;
        String medicationName;
        int quantity;
        String status;

        PurchaseOrder(String orderId, String medicationName, int quantity, String status) {
            this.orderId = orderId;
            this.medicationName = medicationName;
            this.quantity = quantity;
            this.status = status;
        }
    }

    private static class Prescription {
        String prescriptionId;
        String patientId;
        String patientName;
        String medicationName;
        String dosage;
        int requiredQuantity;
        int administeredQuantity;
        String status;
        LocalDate expiryDate;

        Prescription(String prescriptionId, String patientId, String patientName,
                     String medicationName, String dosage, int requiredQuantity,
                     int administeredQuantity, String status, LocalDate expiryDate) {
            this.prescriptionId = prescriptionId;
            this.patientId = patientId;
            this.patientName = patientName;
            this.medicationName = medicationName;
            this.dosage = dosage;
            this.requiredQuantity = requiredQuantity;
            this.administeredQuantity = administeredQuantity;
            this.status = status;
            this.expiryDate = expiryDate;
        }
    }

    private static class InventoryItem {
        int quantity;

        InventoryItem(int quantity) {
            this.quantity = quantity;
        }
    }
}
