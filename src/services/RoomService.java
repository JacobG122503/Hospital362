package services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

import objects.CleaningRequest;
import objects.Equipment;
import objects.Room;
import objects.Patient;
import types.CleaningType;

public class RoomService {
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Path roomsPath;
    private final Path cleaningRequestsPath;
    private final Path equipmentPath;
    private final Queue<CleaningRequest> cleaningQueue = new LinkedList<>();
    private final PatientVitalsCsvService vitalsCsvService;
    private final PharmacyService pharmacyService;

    public RoomService(Path dataDir) {
        this.roomsPath = dataDir.resolve("rooms.csv");
        this.cleaningRequestsPath = dataDir.resolve("cleaning_requests.csv");
        this.equipmentPath = dataDir.resolve("equipment.csv");
        this.vitalsCsvService = new PatientVitalsCsvService(dataDir);
        this.pharmacyService = new PharmacyService(dataDir);
    }

    public void initializeFile() {
        try {
            if (!Files.exists(roomsPath)) {
                Files.createFile(roomsPath);
            }
            if (!Files.exists(cleaningRequestsPath)) {
                Files.createFile(cleaningRequestsPath);
            }
            if (!Files.exists(equipmentPath)) {
                Files.createFile(equipmentPath);
            }
        } catch (IOException e) {
            System.out.println("Error initializing room files: " + e.getMessage());
        }
    }

    /** Load pending cleaning requests from disk into the in-memory queue. */
    public void loadQueue() {
        cleaningQueue.clear();
        try {
            if (!Files.exists(cleaningRequestsPath)) {
                return;
            }
            List<String> lines = Files.readAllLines(cleaningRequestsPath);
            for (int i = 1; i < lines.size(); i++) { // skip header
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] parts = parseCsvLine(line, 6);
                if (parts.length < 5) continue;
                CleaningType type;
                try {
                    type = CleaningType.valueOf(parts[1]);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                String details = parts.length >= 6 ? parts[5] : "";
                CleaningRequest req = new CleaningRequest(
                        parts[0], type, parts[2], parts[3], parts[4], details);
                if ("pending".equalsIgnoreCase(req.getStatus())) {
                    cleaningQueue.add(req);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading cleaning requests: " + e.getMessage());
        }
    }

    /** Persist only the pending requests currently in the queue. */
    private void saveQueue() {
        List<String> lines = new ArrayList<>();
        lines.add("RoomNum,CleaningType,Date,RequestedBy,Status,Details");
        for (CleaningRequest req : cleaningQueue) {
            lines.add(String.join(",",
                    escapeCsv(req.getRoomNumber()),
                    escapeCsv(req.getCleaningType().name()),
                    escapeCsv(req.getTimestamp()),
                    escapeCsv(req.getRequestedByName()),
                    escapeCsv(req.getStatus()),
                    escapeCsv(req.getDetails())
            ));
        }
        try {
            Files.write(cleaningRequestsPath, lines);
        } catch (IOException e) {
            System.out.println("Error saving cleaning requests: " + e.getMessage());
        }
    }

    /** Load all rooms from rooms.txt. */
    public List<Room> loadRooms() {
        List<Room> rooms = new ArrayList<>();
        try {
            if (!Files.exists(roomsPath)) return rooms;
            List<String> lines = Files.readAllLines(roomsPath);
            for (int i = 1; i < lines.size(); i++) { // skip header
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] parts = parseCsvLine(line, 2);
                if (parts.length == 2) {
                    rooms.add(new Room(parts[0], parts[1]));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading rooms: " + e.getMessage());
        }
        return rooms;
    }

    /** Persist the current room list to rooms.txt. */
    private void saveRooms(List<Room> rooms) {
        List<String> lines = new ArrayList<>();
        lines.add("RoomNum,Status");
        for (Room r : rooms) {
            lines.add(escapeCsv(r.getRoomNum()) + "," + escapeCsv(r.getStatus()));
        }
        try {
            Files.write(roomsPath, lines);
        } catch (IOException e) {
            System.out.println("Error saving rooms: " + e.getMessage());
        }
    }

    /** Enqueue a new cleaning request submitted by a nurse. */
    public void requestCleaning(String roomNumber, CleaningType cleaningType, String nurseName, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        CleaningRequest req = new CleaningRequest(roomNumber, cleaningType, nurseName, timestamp, "pending", details);
        cleaningQueue.add(req);
        saveQueue();
        System.out.println("\n  Cleaning request submitted for Room " + roomNumber + ".");
    }

    /** Allow external classes to set a room's status to dirty */
    public void markRoomDirty(String roomNumber) {
        List<Room> rooms = loadRooms();
        for (Room r : rooms) {
            if (r.getRoomNum().equalsIgnoreCase(roomNumber)) {
                r.setStatus("Dirty");
                break;
            }
        }
        saveRooms(rooms);
    }

    /**
     * Process the next pending cleaning request (Facilities Management).
     * Returns the completed request, or null if the queue is empty.
     */
    public CleaningRequest processNextRequest() {
        CleaningRequest req = cleaningQueue.poll();
        if (req != null) {
            req.setStatus("complete");
            saveQueue();
            List<Room> rooms = loadRooms();
            for (Room r : rooms) {
                if (r.getRoomNum().equalsIgnoreCase(req.getRoomNumber())) {
                    r.setStatus("Available");
                    break;
                }
            }
            saveRooms(rooms);
        }
        return req;
    }

    /** Load all equipment from equipment.csv. */
    public List<Equipment> loadEquipment() {
        List<Equipment> equipment = new ArrayList<>();
        try {
            if (!Files.exists(equipmentPath)) return equipment;
            List<String> lines = Files.readAllLines(equipmentPath);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] parts = parseCsvLine(line, 3);
                if (parts.length >= 2) {
                    String roomNum = parts[0].trim();
                    String name = parts[1].trim();
                    boolean isWorking = parts.length < 3 || "true".equalsIgnoreCase(parts[2].trim());
                    Equipment eq = new Equipment(roomNum, name);
                    eq.setIsWorking(isWorking);
                    equipment.add(eq);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading equipment: " + e.getMessage());
        }
        return equipment;
    }

    /** Persist the equipment list to equipment.csv. */
    private void saveEquipment(List<Equipment> equipment) {
        List<String> lines = new ArrayList<>();
        lines.add("RoomNum,Name,IsWorking");
        for (Equipment eq : equipment) {
            lines.add(String.join(",",
                    escapeCsv(eq.getAssociatedRoomNum()),
                    escapeCsv(eq.getName()),
                    Boolean.toString(eq.getIsWorking())
            ));
        }
        try {
            Files.write(equipmentPath, lines);
        } catch (IOException e) {
            System.out.println("Error saving equipment: " + e.getMessage());
        }
    }

    /**
     * Create a new room with "Available" status.
     * Returns false if a room with that number already exists.
     */
    public boolean createRoom(String roomNumber) {
        List<Room> rooms = loadRooms();
        for (Room r : rooms) {
            if (r.getRoomNum().equalsIgnoreCase(roomNumber)) return false;
        }
        rooms.add(new Room(roomNumber, "Available"));
        saveRooms(rooms);
        return true;
    }

    /**
     * Delete a room and all its equipment.
     * Returns false if the room does not exist.
     */
    public boolean deleteRoom(String roomNumber) {
        List<Room> rooms = loadRooms();
        boolean removed = rooms.removeIf(r -> r.getRoomNum().equalsIgnoreCase(roomNumber));
        if (removed) {
            saveRooms(rooms);
            List<Equipment> equipment = loadEquipment();
            equipment.removeIf(eq -> eq.getAssociatedRoomNum().equalsIgnoreCase(roomNumber));
            saveEquipment(equipment);
        }
        return removed;
    }

    /** Add a piece of equipment to a room. */
    public void addEquipmentToRoom(String roomNumber, String equipmentName) {
        List<Equipment> equipment = loadEquipment();
        equipment.add(new Equipment(roomNumber, equipmentName));
        saveEquipment(equipment);
    }

    /** Find a room by number, or null if it does not exist. */
    public Room findRoomByNumber(String roomNumber) {
        for (Room room : loadRooms()) {
            if (room.getRoomNum().equalsIgnoreCase(roomNumber)) {
                return room;
            }
        }
        return null;
    }

    /** A room is transferable only when explicitly marked Available. */
    public boolean isRoomAvailableForTransfer(String roomStatus) {
        return roomStatus != null && roomStatus.equalsIgnoreCase("Available");
    }

    /**
     * Transfer occupancy from one room to another.
     * Returns false if either room does not exist.
     */
    public boolean transferPatientRoom(String fromRoomNumber, String toRoomNumber) {
        List<Room> rooms = loadRooms();
        Room fromRoom = null;
        Room toRoom = null;

        for (Room room : rooms) {
            if (room.getRoomNum().equalsIgnoreCase(fromRoomNumber)) {
                fromRoom = room;
            }
            if (room.getRoomNum().equalsIgnoreCase(toRoomNumber)) {
                toRoom = room;
            }
        }

        if (fromRoom == null || toRoom == null) {
            return false;
        }

        fromRoom.setStatus("Available");
        toRoom.setStatus("Occupied");
        saveRooms(rooms);
        return true;
    }

    /**
     * Remove equipment from a room by its 0-based index within that room's equipment list.
     * Returns false if the index is out of range.
     */
    public boolean removeEquipmentFromRoom(String roomNumber, int index) {
        List<Equipment> all = loadEquipment();
        List<Equipment> roomEquipment = new ArrayList<>();
        for (Equipment eq : all) {
            if (eq.getAssociatedRoomNum().equalsIgnoreCase(roomNumber)) roomEquipment.add(eq);
        }
        if (index < 0 || index >= roomEquipment.size()) return false;
        all.remove(roomEquipment.get(index));
        saveEquipment(all);
        return true;
    }

    // ------------------------------------------------------------------ //
    //  Interactive menu methods                                            //
    // ------------------------------------------------------------------ //

    /** Menu shown to a nurse to submit a room cleaning request. */
    public void showNurseMenu(Scanner scanner, String nurseName, List<Patient> patients, Runnable onSave) {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Rooms ===\n");

        List<Room> rooms = loadRooms();
        if (rooms.isEmpty()) {
            System.out.println("  No rooms are registered in the system.");
            System.out.println("\n  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        System.out.println("  Available Rooms:");
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            String occupantInfo = "";
            if ("Occupied".equalsIgnoreCase(r.getStatus())) {
                for (Patient p : patients) {
                    if (r.getRoomNum().equalsIgnoreCase(p.getRoomNumber()) && !"discharged".equalsIgnoreCase(p.getStatus())) {
                        occupantInfo = "  —  " + p.getName();
                        break;
                    }
                }
            }
            System.out.println("  [" + (i + 1) + "] Room " + r.getRoomNum()
                    + "  (Status: " + r.getStatus() + ")" + occupantInfo);
        }

        System.out.print("\n  Select room number (or 'q' to return): ");
        String roomInput = scanner.nextLine().trim();
        if (roomInput.equalsIgnoreCase("q")) return;
        int roomIndex;
        try {
            roomIndex = Integer.parseInt(roomInput) - 1;
        } catch (NumberFormatException e) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }
        if (roomIndex < 0 || roomIndex >= rooms.size()) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }
        Room selectedRoomObj = rooms.get(roomIndex);
        String selectedRoom = selectedRoomObj.getRoomNum();
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Room " + selectedRoom + " ===\n");
        System.out.println("  [1] Request room cleaning");
        System.out.println("  [2] Request equipment maintenance");
        // Find patient in this room
        Patient roomPatient = null;
        for (Patient p : patients) {
            if (selectedRoom.equalsIgnoreCase(p.getRoomNumber()) && !"discharged".equalsIgnoreCase(p.getStatus())) {
                roomPatient = p;
                break;
            }
        }
        if (roomPatient != null) {
            System.out.println("  [3] Record patient vitals");
            System.out.println("  [4] Administer medication");
        }
        System.out.print("\n  Select an option (or 'q' to return): ");
        String roomOption = scanner.nextLine().trim();
        if (roomOption.equalsIgnoreCase("q")) return;

        if ("1".equals(roomOption)) {
            requestRoomCleaning(scanner, nurseName, rooms, selectedRoomObj, selectedRoom);
        } else if ("2".equals(roomOption)) {
            requestEquipmentMaintenance(scanner, nurseName, selectedRoom);
        } else if ("3".equals(roomOption) && roomPatient != null) {
            recordVitalsForRoomPatient(scanner, roomPatient, onSave);
        } else if ("4".equals(roomOption) && roomPatient != null) {
            administerMedication(scanner, nurseName, roomPatient, onSave);
        } else {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
        }

    }

    // Helper for nurse to record vitals for a patient in a room
   private void recordVitalsForRoomPatient(Scanner scanner, Patient patient, Runnable onSave) {
    System.out.print("\033[H\033[2J\033[3J");
    System.out.flush();
    System.out.println("\n  === Record Vitals for " + patient.getName() + " (Room " + patient.getRoomNumber() + ") ===\n");


    try {
        boolean abnormal = false;
        // Temperature
        System.out.print("  Temperature (F): ");
        String tempInput = scanner.nextLine().trim();
        if (!tempInput.matches("-?\\d+(\\.\\d+)?")) {
            System.out.println("  Invalid temperature: must be a numeric value (e.g., 98.6).");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }
        double temp = Double.parseDouble(tempInput);
        if (temp < 80.0 || temp > 115.0) {
            System.out.println("  Invalid temperature: must be between 80°F and 115°F.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
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
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }
        String[] bpParts = bp.split("/");
        int systolic = Integer.parseInt(bpParts[0]);
        int diastolic = Integer.parseInt(bpParts[1]);
        if (systolic < 50 || systolic > 250 || diastolic < 30 || diastolic > 150) {
            System.out.println("  Invalid blood pressure: values out of physiological range.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
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
        if (!hrInput.matches("\\d+")) {
            System.out.println("  Invalid heart rate: must be a whole number (e.g., 72).");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }
        int hr = Integer.parseInt(hrInput);
        if (hr < 20 || hr > 300) {
            System.out.println("  Invalid heart rate: must be between 20 and 300 bpm.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
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
            System.out.print("    Trouble Breathing: ");
            input = scanner.nextLine().trim();
            troubleBreathing = input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes");
        }

        objects.PatientVitals vitals = new objects.PatientVitals(
            java.time.LocalDateTime.now(), temp, bp, hr, notes, dizziness, nausea, chestPain, confusion, hasFainted, troubleBreathing
        );
        patient.addVitals(vitals);
        // Save to persistent CSV
        if (vitalsCsvService != null) {
            vitalsCsvService.savePatientVitals(patient.getPatientId(), vitals);
        }
        onSave.run();
        System.out.println("\n  Vitals recorded for " + patient.getName() + ".");
        if (abnormal) {
            System.out.println("  An alert has been sent out. A doctor should be with you shortly.");
        }

    } catch (Exception e) {
        System.out.println("  Unexpected error: " + e.getMessage() + ". Vitals not recorded.");
    }

    System.out.println("  Press Enter to return...");
    scanner.nextLine();
}

    // Nurse workflow: administer a fulfilled prescription to a patient
    private void administerMedication(Scanner scanner, String nurseName, Patient patient, Runnable onSave) {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Administer Medication — " + patient.getName() + " ===\n");

        List<String[]> prescriptions = pharmacyService.getAdministerablePrescriptions(patient.getPatientId());

        if (prescriptions.isEmpty()) {
            System.out.println("  No fulfilled prescriptions available to administer for this patient.");

            List<String[]> administered = pharmacyService.getAdministeredPrescriptions(patient.getPatientId());
            if (administered.isEmpty()) {
                System.out.println("  (Prescriptions must be dispensed by the pharmacist before a nurse can administer them.)");
                System.out.println("\n  Press Enter to return...");
                scanner.nextLine();
                return;
            }

            System.out.println("  The following prescriptions have been fully administered and may need a refill:");
            for (int i = 0; i < administered.size(); i++) {
                String[] rx = administered.get(i);
                System.out.println("  [" + (i + 1) + "] " + rx[1] + "  —  " + rx[2]
                        + "  |  Qty: " + rx[3] + "  (Rx ID: " + rx[0] + ")");
            }
            System.out.print("\n  Request a refill? Enter number (or 'n' to return): ");
            String refillSel = scanner.nextLine().trim();
            if (!refillSel.equalsIgnoreCase("n")) {
                try {
                    int refillIdx = Integer.parseInt(refillSel) - 1;
                    if (refillIdx >= 0 && refillIdx < administered.size()) {
                        String[] rx = administered.get(refillIdx);
                        boolean ok = pharmacyService.createRefillRequest(rx[0]);
                        if (ok) {
                            System.out.println("\n  Refill request submitted for " + rx[1] + ".");
                            System.out.println("  The pharmacist will need to fulfill it before you can administer.");
                        } else {
                            System.out.println("\n  Could not create refill request. Please contact the pharmacist.");
                        }
                    } else {
                        System.out.println("\n  Invalid selection.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("\n  Invalid selection.");
                }
            }
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        System.out.println("  Prescriptions ready to administer:");
        for (int i = 0; i < prescriptions.size(); i++) {
            // rx = [prescriptionId, medicationName, dosage, requiredQuantity, administeredQuantity]
            String[] rx = prescriptions.get(i);
            int required = Integer.parseInt(rx[3]);
            int administered = Integer.parseInt(rx[4]);
            int remaining = required - administered;
            System.out.println("  [" + (i + 1) + "] " + rx[1] + "  —  " + rx[2]
                    + "  |  Prescribed: " + required + "  Administered: " + administered
                    + "  Remaining: " + remaining
                    + "  (Rx ID: " + rx[0] + ")");
        }

        System.out.print("\n  Select prescription to administer (or 'q' to return): ");
        String sel = scanner.nextLine().trim();
        if (sel.equalsIgnoreCase("q")) return;

        int idx;
        try {
            idx = Integer.parseInt(sel) - 1;
        } catch (NumberFormatException e) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }
        if (idx < 0 || idx >= prescriptions.size()) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        String[] selectedRx = prescriptions.get(idx);
        int required = Integer.parseInt(selectedRx[3]);
        int alreadyAdministered = Integer.parseInt(selectedRx[4]);
        int remaining = required - alreadyAdministered;

        System.out.println("\n  Medication    : " + selectedRx[1]);
        System.out.println("  Instructions  : " + selectedRx[2]);
        System.out.println("  Prescribed    : " + required);
        System.out.println("  Administered  : " + alreadyAdministered);
        System.out.println("  Remaining     : " + remaining);

        System.out.print("\n  Enter amount to administer: ");
        String amtStr = scanner.nextLine().trim();
        int amount;
        try {
            amount = Integer.parseInt(amtStr);
        } catch (NumberFormatException e) {
            System.out.println("\n  Invalid amount.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }
        if (amount <= 0) {
            System.out.println("\n  Amount must be greater than zero.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }
        if (amount > remaining) {
            System.out.println("\n  Amount exceeds remaining quantity (" + remaining + ").");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        System.out.print("\n  Confirm administering " + amount + " of " + selectedRx[1] + "? (y/n): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("y")) {
            System.out.println("\n  Administration cancelled.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        pharmacyService.administerDose(selectedRx[0], amount);
        pharmacyService.logAdministration(
                selectedRx[0],
                patient.getPatientId(),
                patient.getName(),
                selectedRx[1],
                selectedRx[2],
                amount,
                nurseName
        );
        onSave.run();
        int newRemaining = remaining - amount;
        if (newRemaining == 0) {
            System.out.println("\n  Administered. Prescription fully completed.");
            System.out.print("  Request a refill for " + selectedRx[1] + "? (y/n): ");
            String refillConfirm = scanner.nextLine().trim();
            if (refillConfirm.equalsIgnoreCase("y")) {
                boolean ok = pharmacyService.createRefillRequest(selectedRx[0]);
                if (ok) {
                    System.out.println("  Refill request submitted. The pharmacist will need to fulfill it before the next administration.");
                } else {
                    System.out.println("  Could not submit refill request. Please contact the pharmacist.");
                }
            }
        } else {
            System.out.println("\n  Administered " + amount + ". Remaining: " + newRemaining + ".");
        }
        System.out.println("  Press Enter to return...");
        scanner.nextLine();
    }

    private void requestRoomCleaning(Scanner scanner, String nurseName,
            List<Room> rooms, Room selectedRoomObj, String selectedRoom) {
        // Prevent duplicate pending requests for the same room
        for (CleaningRequest existing : cleaningQueue) {
            if (existing.getRoomNumber().equalsIgnoreCase(selectedRoom)
                    && existing.getCleaningType() != CleaningType.MAINTENANCE) {
                System.out.println("\n  A cleaning request for Room " + selectedRoom
                        + " is already pending.");
                System.out.println("  Press Enter to return...");
                scanner.nextLine();
                return;
            }
        }

        String overrideNote = "";
        if (!selectedRoomObj.getStatus().equalsIgnoreCase("Dirty")) {
            System.out.println("\n  Room " + selectedRoom + " is currently marked as \""
                    + selectedRoomObj.getStatus() + "\", not Dirty.");
            System.out.print("  Proceed anyway? (y/n): ");
            String confirm = scanner.nextLine().trim();
            if (!confirm.equalsIgnoreCase("y")) {
                System.out.println("\n  Request cancelled.");
                System.out.println("  Press Enter to return...");
                scanner.nextLine();
                return;
            }
            System.out.print("  Please leave a note explaining why this room needs cleaning: ");
            overrideNote = scanner.nextLine().trim();
            if (overrideNote.isBlank()) {
                System.out.println("\n  A note is required. Request cancelled.");
                System.out.println("  Press Enter to return...");
                scanner.nextLine();
                return;
            }
        }

        System.out.println("\n  Cleaning Type:");
        CleaningType[] types = CleaningType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.println("  [" + (i + 1) + "] " + types[i]);
        }
        System.out.print("\n  Select cleaning type (or 'q' to return): ");
        String typeInput = scanner.nextLine().trim();
        if (typeInput.equalsIgnoreCase("q")) return;
        int typeIndex;
        try {
            typeIndex = Integer.parseInt(typeInput) - 1;
        } catch (NumberFormatException e) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }
        if (typeIndex < 0 || typeIndex >= types.length) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }
        CleaningType selectedType = types[typeIndex];

        String details = "";

        if (selectedType == CleaningType.BIOHAZARD) {
            String[] materials = {
                "Blood",
                "Bodily fluids",
                "Sharps",
                "Laboratory cultures",
                "Pathological tissue"
            };
            System.out.println("\n  Biohazard Material:");
            for (int i = 0; i < materials.length; i++) {
                System.out.println("  [" + (i + 1) + "] " + materials[i]);
            }
            System.out.print("\n  Select material (or 'q' to return): ");
            String matInput = scanner.nextLine().trim();
            if (matInput.equalsIgnoreCase("q")) return;
            int matIndex;
            try {
                matIndex = Integer.parseInt(matInput) - 1;
            } catch (NumberFormatException e) {
                System.out.println("\n  Invalid selection.");
                System.out.println("  Press Enter to return...");
                scanner.nextLine();
                return;
            }
            if (matIndex < 0 || matIndex >= materials.length) {
                System.out.println("\n  Invalid selection.");
                System.out.println("  Press Enter to return...");
                scanner.nextLine();
                return;
            }
            details = materials[matIndex];
        } else if (selectedType == CleaningType.MAINTENANCE) {
            System.out.print("\n  Describe the maintenance issue: ");
            details = scanner.nextLine().trim();
        }

        if (!overrideNote.isEmpty()) {
            details = details.isEmpty()
                    ? "Override note: " + overrideNote
                    : details + " | Override note: " + overrideNote;
        }

        requestCleaning(selectedRoom, selectedType, nurseName, details);

        if (!overrideNote.isEmpty()) {
            rooms = loadRooms();
            for (Room r : rooms) {
                if (r.getRoomNum().equalsIgnoreCase(selectedRoom)) {
                    r.setStatus("Unavailable");
                    break;
                }
            }
            saveRooms(rooms);
        }

        System.out.println("  Press Enter to return...");
        scanner.nextLine();
    }

    private void requestEquipmentMaintenance(Scanner scanner, String nurseName, String selectedRoom) {
        List<Equipment> all = loadEquipment();
        List<Equipment> roomEquipment = new ArrayList<>();
        for (Equipment eq : all) {
            if (eq.getAssociatedRoomNum().equalsIgnoreCase(selectedRoom)) roomEquipment.add(eq);
        }

        if (roomEquipment.isEmpty()) {
            System.out.println("\n  No equipment registered in Room " + selectedRoom + ".");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        System.out.println("\n  Equipment in Room " + selectedRoom + ":");
        for (int i = 0; i < roomEquipment.size(); i++) {
            Equipment eq = roomEquipment.get(i);
            System.out.println("  [" + (i + 1) + "] " + eq.getName()
                    + "  (Working: " + eq.getIsWorking() + ")");
        }
        System.out.print("\n  Select equipment (or 'q' to return): ");
        String eqInput = scanner.nextLine().trim();
        if (eqInput.equalsIgnoreCase("q")) return;

        int eqIndex;
        try {
            eqIndex = Integer.parseInt(eqInput) - 1;
        } catch (NumberFormatException e) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }
        if (eqIndex < 0 || eqIndex >= roomEquipment.size()) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        Equipment selectedEq = roomEquipment.get(eqIndex);
        System.out.print("\n  Describe the issue with \"" + selectedEq.getName() + "\": ");
        String issue = scanner.nextLine().trim();
        if (issue.isBlank()) {
            System.out.println("\n  A description is required. Request cancelled.");
            System.out.println("  Press Enter to return...");
            scanner.nextLine();
            return;
        }

        // Mark equipment as not working
        for (Equipment eq : all) {
            if (eq.getAssociatedRoomNum().equalsIgnoreCase(selectedRoom)
                    && eq.getName().equals(selectedEq.getName())) {
                eq.setIsWorking(false);
                break;
            }
        }
        saveEquipment(all);

        String details = "Equipment: " + selectedEq.getName() + " | Issue: " + issue;
        requestCleaning(selectedRoom, CleaningType.MAINTENANCE, nurseName, details);

        System.out.println("  Press Enter to return...");
        scanner.nextLine();
    }

    /** Menu shown to a Facilities Management employee to process the cleaning queue. */
    public void showFacilitiesMenu(Scanner scanner) {
        while (true) {
            System.out.print("\033[H\033[2J\033[3J");
            System.out.flush();
            System.out.println("\n  === Cleaning Queue ===\n");

            if (cleaningQueue.isEmpty()) {
                System.out.println("  No pending cleaning requests.");
                System.out.println("\n  Press Enter to return...");
                scanner.nextLine();
                return;
            }

            List<CleaningRequest> pending = new ArrayList<>(cleaningQueue);
            for (int i = 0; i < pending.size(); i++) {
                System.out.println("  [" + (i + 1) + "] " + pending.get(i));
            }

            System.out.println("\n  [1] Process next request");
            System.out.println("  [2] Return to main menu");
            System.out.print("\n  Select option (or 'q' to return): ");
            String choice = scanner.nextLine().trim();
            if (choice.equalsIgnoreCase("q")) return;

            if ("1".equals(choice)) {
                CleaningRequest completed = processNextRequest();
                if (completed != null) {
                    System.out.println("\n  Completed: Room " + completed.getRoomNumber()
                            + " (" + completed.getCleaningType() + ") — marked complete.");
                }
                System.out.println("  Press Enter to continue...");
                scanner.nextLine();
            } else {
                return;
            }
        }
    }

    /** Menu shown to a Facilities Management employee to manage rooms and equipment. */
    public void showRoomManagementMenu(Scanner scanner) {
        while (true) {
            System.out.print("\033[H\033[2J\033[3J");
            System.out.flush();
            System.out.println("\n  === Room Management ===\n");

            List<Room> rooms = loadRooms();
            if (rooms.isEmpty()) {
                System.out.println("  No rooms registered.");
            } else {
                for (Room r : rooms) {
                    System.out.println("  Room " + r.getRoomNum() + "  (Status: " + r.getStatus() + ")");
                }
            }

            System.out.println("\n  [1] Create room");
            System.out.println("  [2] Delete room");
            System.out.println("  [3] Manage equipment");
            System.out.println("  [q] Return");
            System.out.print("\n  Select option: ");
            String choice = scanner.nextLine().trim();
            if (choice.equalsIgnoreCase("q")) return;

            switch (choice) {
                case "1": {
                    System.out.print("\n  Enter new room number: ");
                    String roomNum = scanner.nextLine().trim();
                    if (!roomNum.isBlank()) {
                        if (createRoom(roomNum)) {
                            System.out.println("\n  Room " + roomNum + " created successfully.");
                        } else {
                            System.out.println("\n  Room " + roomNum + " already exists.");
                        }
                        System.out.println("  Press Enter to continue...");
                        scanner.nextLine();
                    }
                    break;
                }
                case "2": {
                    if (rooms.isEmpty()) break;
                    System.out.println("\n  Select room to delete:");
                    for (int i = 0; i < rooms.size(); i++) {
                        System.out.println("  [" + (i + 1) + "] Room " + rooms.get(i).getRoomNum()
                                + "  (" + rooms.get(i).getStatus() + ")");
                    }
                    System.out.print("\n  Select room (or 'q' to cancel): ");
                    String sel = scanner.nextLine().trim();
                    if (!sel.equalsIgnoreCase("q")) {
                        try {
                            int idx = Integer.parseInt(sel) - 1;
                            if (idx >= 0 && idx < rooms.size()) {
                                String roomNum = rooms.get(idx).getRoomNum();
                                System.out.print("  Delete Room " + roomNum
                                        + "? This also removes all its equipment. (y/n): ");
                                if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                                    deleteRoom(roomNum);
                                    System.out.println("\n  Room " + roomNum + " deleted.");
                                } else {
                                    System.out.println("\n  Cancelled.");
                                }
                            } else {
                                System.out.println("\n  Invalid selection.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("\n  Invalid selection.");
                        }
                    }
                    System.out.println("  Press Enter to continue...");
                    scanner.nextLine();
                    break;
                }
                case "3": {
                    showEquipmentMenu(scanner, rooms);
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void showEquipmentMenu(Scanner scanner, List<Room> rooms) {
        if (rooms.isEmpty()) {
            System.out.println("\n  No rooms available. Create a room first.");
            System.out.println("  Press Enter to continue...");
            scanner.nextLine();
            return;
        }

        System.out.println("\n  Select room to manage equipment:");
        for (int i = 0; i < rooms.size(); i++) {
            System.out.println("  [" + (i + 1) + "] Room " + rooms.get(i).getRoomNum()
                    + "  (" + rooms.get(i).getStatus() + ")");
        }
        System.out.print("\n  Select room (or 'q' to return): ");
        String sel = scanner.nextLine().trim();
        if (sel.equalsIgnoreCase("q")) return;

        int idx;
        try {
            idx = Integer.parseInt(sel) - 1;
        } catch (NumberFormatException e) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to continue...");
            scanner.nextLine();
            return;
        }
        if (idx < 0 || idx >= rooms.size()) {
            System.out.println("\n  Invalid selection.");
            System.out.println("  Press Enter to continue...");
            scanner.nextLine();
            return;
        }

        String roomNumber = rooms.get(idx).getRoomNum();

        while (true) {
            System.out.print("\033[H\033[2J\033[3J");
            System.out.flush();
            System.out.println("\n  === Equipment — Room " + roomNumber + " ===\n");

            List<Equipment> all = loadEquipment();
            List<Equipment> roomEquipment = new ArrayList<>();
            for (Equipment eq : all) {
                if (eq.getAssociatedRoomNum().equalsIgnoreCase(roomNumber)) roomEquipment.add(eq);
            }

            if (roomEquipment.isEmpty()) {
                System.out.println("  No equipment in this room.");
            } else {
                for (int i = 0; i < roomEquipment.size(); i++) {
                    Equipment eq = roomEquipment.get(i);
                    System.out.println("  [" + (i + 1) + "] " + eq.getName()
                            + "  (Working: " + eq.getIsWorking() + ")");
                }
            }

            System.out.println("\n  [1] Add equipment");
            System.out.println("  [2] Remove equipment");
            System.out.println("  [q] Return");
            System.out.print("\n  Select option: ");
            String choice = scanner.nextLine().trim();
            if (choice.equalsIgnoreCase("q")) return;

            if ("1".equals(choice)) {
                System.out.print("\n  Enter equipment name: ");
                String name = scanner.nextLine().trim();
                if (!name.isBlank()) {
                    addEquipmentToRoom(roomNumber, name);
                    System.out.println("\n  \"" + name + "\" added to Room " + roomNumber + ".");
                }
                System.out.println("  Press Enter to continue...");
                scanner.nextLine();
            } else if ("2".equals(choice)) {
                if (roomEquipment.isEmpty()) {
                    System.out.println("\n  No equipment to remove.");
                    System.out.println("  Press Enter to continue...");
                    scanner.nextLine();
                    continue;
                }
                System.out.print("\n  Select equipment to remove: ");
                String removeStr = scanner.nextLine().trim();
                try {
                    int removeIdx = Integer.parseInt(removeStr) - 1;
                    if (removeEquipmentFromRoom(roomNumber, removeIdx)) {
                        System.out.println("\n  Equipment removed.");
                    } else {
                        System.out.println("\n  Invalid selection.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("\n  Invalid selection.");
                }
                System.out.println("  Press Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  Pipe-delimited file helpers (same pattern as DataStoreService)     //
    // ------------------------------------------------------------------ //

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
}




