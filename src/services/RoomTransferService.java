package services;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import objects.Equipment;
import objects.Patient;
import objects.Room;

public class RoomTransferService {

    public static void runChangePatientRoomFlow(
            Scanner scanner,
            List<Patient> patients,
            RoomService roomService,
            Runnable onSave
    ) {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Change Patient Room ===\n");

        System.out.print("  Enter patient ID or name: ");
        String query = scanner.nextLine().trim();

        List<Patient> matches = new ArrayList<>();
        for (Patient patient : patients) {
            if (!"admitted".equalsIgnoreCase(patient.getStatus())) continue;
            if (patient.getPatientId().toLowerCase().contains(query.toLowerCase())
                    || patient.getName().toLowerCase().contains(query.toLowerCase())) {
                matches.add(patient);
            }
        }

        if (matches.isEmpty()) {
            System.out.println("\n  No admitted patients found matching \"" + query + "\".");
            System.out.println("  Only admitted patients can be reassigned.");
            pause(scanner);
            return;
        }

        for (int i = 0; i < matches.size(); i++) {
            Patient patient = matches.get(i);
            System.out.println("  [" + (i + 1) + "] " + patient.getName()
                    + "  (ID: " + patient.getPatientId() + ", Current Room: " + patient.getRoomNumber() + ")");
        }

        System.out.print("\n  Select patient number (or 'q' to return): ");
        String selection = scanner.nextLine().trim();
        if (selection.equalsIgnoreCase("q")) return;

        int selectedIndex;
        try {
            selectedIndex = Integer.parseInt(selection) - 1;
        } catch (NumberFormatException e) {
            System.out.println("\n  Invalid selection.");
            pause(scanner);
            return;
        }

        if (selectedIndex < 0 || selectedIndex >= matches.size()) {
            System.out.println("\n  Invalid selection.");
            pause(scanner);
            return;
        }

        Patient selectedPatient = matches.get(selectedIndex);
        String currentRoom = selectedPatient.getRoomNumber();

        System.out.println("\n  Selected patient: " + selectedPatient.getName());
        System.out.println("  Current room: " + currentRoom);

        while (true) {
            System.out.print("\n  Enter target room number (or 'q' to cancel): ");
            String targetRoom = scanner.nextLine().trim();
            if (targetRoom.equalsIgnoreCase("q")) return;

            if (targetRoom.isBlank()) {
                System.out.println("  Room number cannot be blank.");
                continue;
            }

            if (targetRoom.equalsIgnoreCase(currentRoom)) {
                System.out.println("  Patient is already assigned to Room " + currentRoom + ".");
                continue;
            }

            Room targetRoomRecord = roomService.findRoomByNumber(targetRoom);
            if (targetRoomRecord == null) {
                System.out.println("  Invalid room number. Room does not exist.");
                continue;
            }

            if (!roomService.isRoomAvailableForTransfer(targetRoomRecord.getStatus())) {
                System.out.println("  Room " + targetRoom + " is not available (status: "
                        + targetRoomRecord.getStatus() + ").");
                System.out.println("  Choose a different room.");
                continue;
            }

            if (!isRoomAppropriateForPatient(selectedPatient, targetRoom, roomService)) {
                System.out.println("  Room " + targetRoom + " does not currently meet this patient's care needs.");
                System.out.println("  Choose a different room.");
                continue;
            }

            System.out.print("  Confirm transfer to Room " + targetRoom + "? (y/n): ");
            String confirm = scanner.nextLine().trim();
            if (!confirm.equalsIgnoreCase("y")) {
                System.out.println("  Transfer cancelled.");
                pause(scanner);
                return;
            }

            boolean updatedRooms = roomService.transferPatientRoom(currentRoom, targetRoom);
            if (!updatedRooms) {
                System.out.println("\n  Could not complete room transfer. Verify room data and try again.");
                pause(scanner);
                return;
            }

            selectedPatient.setRoomNumber(targetRoom);
            onSave.run();

            System.out.println("\n  Room transfer complete.");
            System.out.println("  Patient " + selectedPatient.getName() + " moved from Room "
                    + currentRoom + " to Room " + targetRoom + ".");
            pause(scanner);
            return;
        }
    }

    private static void pause(Scanner scanner) {
        System.out.println("\n  Press Enter to return to menu...");
        scanner.nextLine();
    }

    private static boolean isRoomAppropriateForPatient(Patient patient, String roomNumber, RoomService roomService) {
        String diagnosis = patient.getDiagnosis() == null ? "" : patient.getDiagnosis().toLowerCase();
        boolean highAcuity = diagnosis.contains("critical")
                || diagnosis.contains("icu")
                || diagnosis.contains("post-op")
                || diagnosis.contains("post op")
                || diagnosis.contains("surgery")
                || diagnosis.contains("respiratory");

        if (!highAcuity) {
            return true;
        }

        for (Equipment equipment : roomService.loadEquipment()) {
            if (equipment.getAssociatedRoomNum().equalsIgnoreCase(roomNumber) && equipment.getIsWorking()) {
                return true;
            }
        }
        return false;
    }
}
