package objects;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PatientVitals implements Serializable {
    private LocalDateTime timestamp;
    private double temperature;
    private String bloodPressure;
    private int heartRate;
    private String notes;

    public PatientVitals(LocalDateTime timestamp, double temperature, String bloodPressure, int heartRate, String notes) {
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.bloodPressure = bloodPressure;
        this.heartRate = heartRate;
        this.notes = notes;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public double getTemperature() { return temperature; }
    public String getBloodPressure() { return bloodPressure; }
    public int getHeartRate() { return heartRate; }
    public String getNotes() { return notes; }

    @Override
    public String toString() {
        return "Vitals @ " + timestamp + ": Temp=" + temperature + "F, BP=" + bloodPressure + ", HR=" + heartRate + (notes.isBlank() ? "" : ", Notes: " + notes);
    }
}
