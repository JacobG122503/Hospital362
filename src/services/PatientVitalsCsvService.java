package services;

import objects.Patient;
import objects.PatientVitals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PatientVitalsCsvService {

            /**
             * Shows abnormal vitals alerts for physicians, using the provided patient list and scanner.
             */
            public static void showAbnormalVitalsAlerts(java.util.List<objects.Patient> patients, java.util.Scanner scanner) {
                PatientVitalsCsvService vitalsCsvService = new PatientVitalsCsvService(java.nio.file.Paths.get("data"));
                java.util.Map<String, objects.PatientVitals> mostRecentAbnormal = vitalsCsvService.getMostRecentAbnormalVitalsByPatient(patients);
                java.util.Map<String, String> patientIdToName = new java.util.HashMap<>();
                for (objects.Patient p : patients) {
                    patientIdToName.put(p.getPatientId(), p.getName());
                }
                System.out.print("\033[H\033[2J\033[3J");
                System.out.flush();
                System.out.println("\n  === Abnormal Vitals Alerts ===\n");
                if (mostRecentAbnormal.isEmpty()) {
                    System.out.println("  No patients currently flagged for abnormal vitals.\n");
                    System.out.println("  Press Enter to return...");
                    scanner.nextLine();
                    return;
                }
                java.util.List<String> flaggedIds = new java.util.ArrayList<>(mostRecentAbnormal.keySet());
                for (int i = 0; i < flaggedIds.size(); i++) {
                    String pid = flaggedIds.get(i);
                    String name = patientIdToName.getOrDefault(pid, pid);
                    objects.Patient p = null;
                    for (objects.Patient pat : patients) {
                        if (pat.getPatientId().equals(pid)) { p = pat; break; }
                    }
                    String room = (p != null) ? p.getRoomNumber() : "?";
                    System.out.println("  [" + (i + 1) + "] " + name + " (ID: " + pid + ", Room: " + room + ")");
                }
                System.out.print("\n  Select patient number to view most recent abnormal vitals (or 'q' to return): ");
                String sel = scanner.nextLine().trim();
                if (sel.equalsIgnoreCase("q"))
                    return;
                int idx;
                try {
                    idx = Integer.parseInt(sel) - 1;
                } catch (NumberFormatException e) {
                    System.out.println("  Invalid selection.");
                    System.out.println("  Press Enter to return...");
                    scanner.nextLine();
                    return;
                }
                if (idx < 0 || idx >= flaggedIds.size()) {
                    System.out.println("  Invalid selection.");
                    System.out.println("  Press Enter to return...");
                    scanner.nextLine();
                    return;
                }
                String selectedId = flaggedIds.get(idx);
                String selectedName = patientIdToName.getOrDefault(selectedId, selectedId);
                objects.PatientVitals v = mostRecentAbnormal.get(selectedId);
                System.out.print("\033[H\033[2J\033[3J");
                System.out.flush();
                System.out.println("\n  === Most Recent Abnormal Vitals for " + selectedName + " ===\n");
                if (v != null) {
                    System.out.println("  " + v.toString());
                } else {
                    System.out.println("  No abnormal vitals found for this patient.");
                }
                System.out.println("\n  Press Enter to return...");
                scanner.nextLine();
            }
        /**
         * Returns a map of patientId -> most recent abnormal PatientVitals (from CSV), using the provided patient list for age lookup.
         */
        public java.util.Map<String, PatientVitals> getMostRecentAbnormalVitalsByPatient(java.util.List<objects.Patient> patients) {
            java.util.Map<String, PatientVitals> mostRecentAbnormal = new java.util.HashMap<>();
            java.util.Map<String, String> mostRecentTimestamp = new java.util.HashMap<>();
            java.util.Map<String, objects.Patient> patientMap = new java.util.HashMap<>();
            for (objects.Patient p : patients) {
                patientMap.put(p.getPatientId(), p);
            }
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(vitalsCsvPath);
                if (lines.size() <= 1) {
                    lines = java.util.Collections.emptyList();
                } else {
                    lines = lines.subList(1, lines.size()); // skip header
                }
                for (String line : lines) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split(",", -1);
                    if (parts.length < 12) continue;
                    String patientId = parts[0];
                    String timestamp = parts[1];
                    double temperature = Double.parseDouble(parts[2]);
                    String bloodPressure = parts[3];
                    int heartRate = Integer.parseInt(parts[4]);
                    String notes = parts[5];
                    boolean dizziness = Boolean.parseBoolean(parts[6]);
                    boolean nausea = Boolean.parseBoolean(parts[7]);
                    boolean chestPain = Boolean.parseBoolean(parts[8]);
                    boolean confusion = Boolean.parseBoolean(parts[9]);
                    boolean hasFainted = Boolean.parseBoolean(parts[10]);
                    boolean troubleBreathing = Boolean.parseBoolean(parts[11]);
                    java.time.LocalDateTime ts = java.time.LocalDateTime.parse(timestamp, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    objects.Patient patient = patientMap.get(patientId);
                    if (patient == null) continue;
                    int age = patient.getAge();
                    PatientVitals v = new PatientVitals(ts, temperature, bloodPressure, heartRate, notes, dizziness, nausea, chestPain, confusion, hasFainted, troubleBreathing);
                    boolean abnormal = v.isBloodPressureAbnormal(age) || v.isTemperatureAbnormal(age) || v.isHeartRateAbnormal(age);
                    if (abnormal) {
                        if (!mostRecentTimestamp.containsKey(patientId) || ts.isAfter(java.time.LocalDateTime.parse(mostRecentTimestamp.get(patientId), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))) {
                            mostRecentAbnormal.put(patientId, v);
                            mostRecentTimestamp.put(patientId, timestamp);
                        }
                    }
                }
            } catch (Exception e) {
                // Optionally log or rethrow
            }
            return mostRecentAbnormal;
        }
    private final Path vitalsCsvPath;
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String HEADER = "PatientId,Timestamp,Temperature,BloodPressure,HeartRate,Notes,Dizziness,Nausea,ChestPain,Confusion,HasFainted,TroubleBreathing";

    public PatientVitalsCsvService(Path dataDir) {
        this.vitalsCsvPath = dataDir.resolve("patients_vitals.csv");
        try {
            if (!Files.exists(vitalsCsvPath)) {
                Files.writeString(vitalsCsvPath, HEADER + "\n");
            }
        } catch (IOException e) {
            System.out.println("Error initializing vitals CSV: " + e.getMessage());
        }
    }

    public void savePatientVitals(String patientId, PatientVitals vitals) {
        String line = String.join(",",
                escape(patientId),
                escape(vitals.getTimestamp().format(TS_FMT)),
                String.valueOf(vitals.getTemperature()),
                escape(vitals.getBloodPressure()),
                String.valueOf(vitals.getHeartRate()),
                escape(vitals.getNotes()),
                String.valueOf(vitals.hasDizziness()),
                String.valueOf(vitals.hasNausea()),
                String.valueOf(vitals.hasChestPain()),
                String.valueOf(vitals.hasConfusion()),
                String.valueOf(vitals.hasFainted()),
                String.valueOf(vitals.troubleBreathing())
        );
        try {
            Files.writeString(vitalsCsvPath, line + "\n", StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Error saving patient vitals: " + e.getMessage());
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
