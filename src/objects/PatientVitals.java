package objects;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PatientVitals implements Serializable {
    private LocalDateTime timestamp;
    private double temperature;
    private String bloodPressure;
    private int heartRate;
    private String notes;

    // Symptom flags
    private boolean dizziness;
    private boolean nausea;
    private boolean chestPain;
    private boolean confusion;
    private boolean hasFainted;
    private boolean troubleBreathing;

    /**
     * Checks if the blood pressure is abnormal for the given age.
     * @param age Patient's age in years
     * @return true if abnormal, false if normal
     */
    public boolean isBloodPressureAbnormal(int age) {
        if (bloodPressure == null || !bloodPressure.matches("\\d{2,3}/\\d{2,3}")) return false;
        String[] parts = bloodPressure.split("/");
        int systolic = Integer.parseInt(parts[0]);
        int diastolic = Integer.parseInt(parts[1]);
        if (age >= 3 && age <= 6) {
            return systolic < 80 || systolic > 100 || diastolic < 55 || diastolic > 75;
        } else if (age >= 7 && age <= 17) {
            return systolic < 90 || systolic > 120 || diastolic < 60 || diastolic > 80;
        } else if (age >= 18 && age <= 59) {
            return systolic != 120 || diastolic != 80;
        } else if (age >= 60) {
            return systolic > 160 || diastolic > 90;
        }
        // For ages below 3, no check
        return false;
    }

    public PatientVitals(LocalDateTime timestamp, double temperature, String bloodPressure, int heartRate, String notes) {
        this(timestamp, temperature, bloodPressure, heartRate, notes, false, false, false, false, false, false);
    }

    public PatientVitals(LocalDateTime timestamp, double temperature, String bloodPressure, int heartRate, String notes,
                         boolean dizziness, boolean nausea, boolean chestPain, boolean confusion, boolean hasFainted, boolean troubleBreathing) {
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.bloodPressure = bloodPressure;
        this.heartRate = heartRate;
        this.notes = notes;
        this.dizziness = dizziness;
        this.nausea = nausea;
        this.chestPain = chestPain;
        this.confusion = confusion;
        this.hasFainted = hasFainted;
        this.troubleBreathing = troubleBreathing;
    }


    public LocalDateTime getTimestamp() { return timestamp; }
    public double getTemperature() { return temperature; }
    public String getBloodPressure() { return bloodPressure; }
    public int getHeartRate() { return heartRate; }
    public String getNotes() { return notes; }

    public boolean hasDizziness() { return dizziness; }
    public boolean hasNausea() { return nausea; }
    public boolean hasChestPain() { return chestPain; }
    public boolean hasConfusion() { return confusion; }
    public boolean hasFainted() { return hasFainted; }
    public boolean troubleBreathing() { return troubleBreathing;}


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vitals @ ").append(timestamp)
          .append(": Temp=").append(temperature)
          .append("F, BP=").append(bloodPressure)
          .append(", HR=").append(heartRate);
        if (!notes.isBlank()) sb.append(", Notes: ").append(notes);
        if (dizziness) sb.append(", Dizziness");
        if (nausea) sb.append(", Nausea");
        if (chestPain) sb.append(", Chest Pain");
        if (confusion) sb.append(", Confusion");
        if (hasFainted) sb.append(", Has Fainted");
        if (troubleBreathing) sb.append(", Trouble Breathing");
        return sb.toString();
    }
        /**
     * Checks if the temperature is abnormal for the given age.
     * @param age Patient's age in years
     * @return true if abnormal, false if normal
     */
    public boolean isTemperatureAbnormal(int age) {
        if (age <= 17) {
            return temperature < 95.9 || temperature > 99.5;
        } else {
            return temperature < 97.0 || temperature > 99.0;
        }
    }

    /**
     * Returns the fever grade based on temperature.
     * @return "Low-grade", "Moderate-grade", "High-grade", or "None"
     */
    public String getFeverGrade() {
        if (temperature >= 102.4 && temperature <= 105.8) {
            return "High-grade";
        } else if (temperature >= 100.6 && temperature <= 102.2) {
            return "Moderate-grade";
        } else if (temperature >= 99.1 && temperature <= 100.4) {
            return "Low-grade";
        } else {
            return "None";
        }
    }
        /**
     * Checks if the heart rate is abnormal for the given age.
     * @param age Patient's age in years
     * @return true if abnormal, false if normal
     */
    public boolean isHeartRateAbnormal(int age) {
        if (age <= 12) {
            return heartRate < 100 || heartRate > 160;
        } else {
            return heartRate < 60 || heartRate > 100;
        }
    }
}
