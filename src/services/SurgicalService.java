package services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import objects.Employee;
import objects.Patient;
import objects.Room;
import objects.Surgery;

public class SurgicalService {

    private final Path surgeriesFile;

    public SurgicalService(Path dataDir) {
        this.surgeriesFile = dataDir.resolve("surgeries.csv");
    }

    public void initializeFile() {
        try {
            if (!Files.exists(surgeriesFile)) {
                Files.write(surgeriesFile, List.of("ProcedureName,PatientId,PatientName,SurgeonName,Date,OperatingRoom"));
            }
        } catch (IOException e) {
            System.out.println("Error initializing surgeries file: " + e.getMessage());
        }
    }

    public void runScheduleSurgeryFlow(
            Scanner scanner,
            List<Patient> patients,
            List<Employee> employees,
            RoomService roomService
    ) {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Schedule Surgical Procedure ===\n");

        // Step 1: Look up admitted patient by name or ID
        System.out.print("  Enter patient ID or name: ");
        String query = scanner.nextLine().trim();

        List<Patient> matches = new ArrayList<>();
        for (Patient p : patients) {
            if (!"admitted".equalsIgnoreCase(p.getStatus())) continue;
            if (p.getPatientId().toLowerCase().contains(query.toLowerCase())
                    || p.getName().toLowerCase().contains(query.toLowerCase())) {
                matches.add(p);
            }
        }

        if (matches.isEmpty()) {
            System.out.println("\n  No admitted patients found matching \"" + query + "\".");
            pause(scanner);
            return;
        }

        // Step 2: Select patient
        for (int i = 0; i < matches.size(); i++) {
            System.out.println("\n  [" + (i + 1) + "] " + matches.get(i).toString());
        }

        System.out.print("\n  Select patient number: ");
        int patientIndex;
        try {
            patientIndex = Integer.parseInt(scanner.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("\n  Invalid selection.");
            pause(scanner);
            return;
        }
        if (patientIndex < 0 || patientIndex >= matches.size()) {
            System.out.println("\n  Invalid selection.");
            pause(scanner);
            return;
        }

        Patient patient = matches.get(patientIndex);

        // Step 3: Select surgeon
        List<Employee> surgeons = new ArrayList<>();
        for (Employee e : employees) {
            if (e.getRole() != null && e.getRole().toLowerCase().contains("surgeon")) {
                surgeons.add(e);
            }
        }

        if (surgeons.isEmpty()) {
            System.out.println("\n  No surgeons are currently registered in the system.");
            pause(scanner);
            return;
        }

        System.out.println("\n  Available Surgeons:");
        for (int i = 0; i < surgeons.size(); i++) {
            Employee s = surgeons.get(i);
            System.out.println("  [" + (i + 1) + "] " + s.getName()
                    + " | ID: " + s.getEmployeeId()
                    + " | Role: " + s.getRole());
        }

        System.out.print("\n  Select surgeon number: ");
        int surgeonIndex;
        try {
            surgeonIndex = Integer.parseInt(scanner.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("\n  Invalid selection.");
            pause(scanner);
            return;
        }
        if (surgeonIndex < 0 || surgeonIndex >= surgeons.size()) {
            System.out.println("\n  Invalid selection.");
            pause(scanner);
            return;
        }

        Employee surgeon = surgeons.get(surgeonIndex);

        // Step 4: Enter procedure name and date
        System.out.print("\n  Procedure name: ");
        String procedureName = scanner.nextLine().trim();

        String date = null;
        while (date == null) {
            System.out.print("  Date (YYYY-MM-DD): ");
            String input = scanner.nextLine().trim();
            try {
                LocalDate.parse(input);
                date = input;
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("  Invalid date. Please use YYYY-MM-DD format.");
            }
        }

        // Step 5: Select operating room
        List<Room> rooms = roomService.loadRooms();

        if (rooms.isEmpty()) {
            System.out.println("\n  No rooms are registered in the system.");
            pause(scanner);
            return;
        }

        System.out.println("\n  Available Rooms:");
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            System.out.println("  [" + (i + 1) + "] Room " + r.getRoomNum()
                    + " | Status: " + r.getStatus());
        }

        System.out.print("\n  Select room number: ");
        int roomIndex;
        try {
            roomIndex = Integer.parseInt(scanner.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("\n  Invalid selection.");
            pause(scanner);
            return;
        }
        if (roomIndex < 0 || roomIndex >= rooms.size()) {
            System.out.println("\n  Invalid selection.");
            pause(scanner);
            return;
        }

        Room selectedRoom = rooms.get(roomIndex);
        String operatingRoom = selectedRoom.getRoomNum();

        // Check for dirty room
        if ("dirty".equalsIgnoreCase(selectedRoom.getStatus())
        || "occupied".equalsIgnoreCase(selectedRoom.getStatus())) {
        System.out.println("\n  Room " + operatingRoom + " is currently " 
                + selectedRoom.getStatus().toLowerCase() + " and cannot be used for surgery.");
        System.out.println("  Please select an available room.");
            pause(scanner);
            return;
        }

        // Step 6: Check for room conflict on that date
        List<Surgery> existing = loadSurgeries();
        boolean conflict = true;
        while (conflict) {
            conflict = false;
            for (Surgery s : existing) {
                if (s.getOperatingRoom().equalsIgnoreCase(operatingRoom)
                        && s.getDate().equals(date)) {
                    conflict = true;
                    System.out.println("\n  Conflict: Room " + operatingRoom
                            + " is already booked on " + date + ".");
                    System.out.println("  Existing procedure: " + s.getProcedureName()
                            + " for patient " + s.getPatientName() + ".");
                    System.out.print("\n  Enter a different date (YYYY-MM-DD) or press Enter to cancel: ");
                    String newDate = scanner.nextLine().trim();
                    if (newDate.isBlank()) {
                        System.out.println("\n  Scheduling cancelled.");
                        pause(scanner);
                        return;
                    }
                    try {
                        LocalDate.parse(newDate);
                        date = newDate;
                    } catch (java.time.format.DateTimeParseException e) {
                        System.out.println("  Invalid date. Please use YYYY-MM-DD format.");
                    }
                    break;
                }
            }
        }

        // Step 7: Save and confirm
        Surgery surgery = new Surgery(procedureName, patient.getPatientId(),
                patient.getName(), surgeon.getName(), date, operatingRoom);
        saveSurgery(surgery);

        System.out.println("\n  Surgical procedure scheduled successfully.");
        System.out.println("  " + surgery.toString());
        pause(scanner);
    }

    private List<Surgery> loadSurgeries() {
        List<Surgery> surgeries = new ArrayList<>();
        try {
            if (!Files.exists(surgeriesFile)) return surgeries;
            List<String> lines = Files.readAllLines(surgeriesFile);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length >= 6) {
                    surgeries.add(new Surgery(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading surgeries: " + e.getMessage());
        }
        return surgeries;
    }

    private void saveSurgery(Surgery surgery) {
        try {
            List<String> lines = Files.exists(surgeriesFile)
                    ? new ArrayList<>(Files.readAllLines(surgeriesFile))
                    : new ArrayList<>(List.of("ProcedureName,PatientId,PatientName,SurgeonName,Date,OperatingRoom"));
            lines.add(String.join(",",
                    escapeCsv(surgery.getProcedureName()),
                    escapeCsv(surgery.getPatientId()),
                    escapeCsv(surgery.getPatientName()),
                    escapeCsv(surgery.getSurgeonName()),
                    escapeCsv(surgery.getDate()),
                    escapeCsv(surgery.getOperatingRoom())
            ));
            Files.write(surgeriesFile, lines);
        } catch (IOException e) {
            System.out.println("Error saving surgery: " + e.getMessage());
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
