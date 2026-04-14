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
import objects.Room;
import types.CleaningType;

public class RoomService {
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Path roomsPath;
    private final Path cleaningRequestsPath;
    private final Queue<CleaningRequest> cleaningQueue = new LinkedList<>();

    public RoomService(Path dataDir) {
        this.roomsPath = dataDir.resolve("rooms.csv");
        this.cleaningRequestsPath = dataDir.resolve("cleaning_requests.csv");
    }

    public void initializeFile() {
        try {
            if (!Files.exists(roomsPath)) {
                Files.createFile(roomsPath);
            }
            if (!Files.exists(cleaningRequestsPath)) {
                Files.createFile(cleaningRequestsPath);
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

    // ------------------------------------------------------------------ //
    //  Interactive menu methods                                            //
    // ------------------------------------------------------------------ //

    /** Menu shown to a nurse to submit a room cleaning request. */
    public void showNurseMenu(Scanner scanner, String nurseName) {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Request Room Cleaning ===\n");

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
            System.out.println("  [" + (i + 1) + "] Room " + r.getRoomNum()
                    + "  (Status: " + r.getStatus() + ")");
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

        // Prevent duplicate pending requests for the same room
        for (CleaningRequest existing : cleaningQueue) {
            if (existing.getRoomNumber().equalsIgnoreCase(selectedRoom)) {
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




