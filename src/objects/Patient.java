package objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Patient extends Person {
    private String patientId;
    private String diagnosis;
    private String roomNumber;
    private String admissionDate;
    private String insuranceProvider;
    private String status;
    private String dischargeDate;
    private List<String> allergies = new ArrayList<>();
    private List<String> activeMedications = new ArrayList<>();

    public Patient(String name, int age, String gender, String phoneNumber, String address,
                   String patientId, String diagnosis, String roomNumber, String admissionDate,
                   String insuranceProvider) {
        super(name, age, gender, phoneNumber, address);
        this.patientId = patientId;
        this.diagnosis = diagnosis;
        this.roomNumber = roomNumber;
        this.admissionDate = admissionDate;
        this.insuranceProvider = insuranceProvider;
        this.status = "admitted";
        this.dischargeDate = "";
        this.allergies = new ArrayList<>();
        this.activeMedications = new ArrayList<>();
    }

    public Patient(String name, int age, String gender, String phoneNumber, String address,
               String patientId, String diagnosis, String roomNumber, String admissionDate,
               String insuranceProvider, String status, String dischargeDate,
               String allergiesCsv, String activeMedicationsCsv) {
        super(name, age, gender, phoneNumber, address);
        this.patientId = patientId;
        this.diagnosis = diagnosis;
        this.roomNumber = roomNumber;
        this.admissionDate = admissionDate;
        this.insuranceProvider = insuranceProvider;
        this.status = status;
        this.dischargeDate = dischargeDate;
        this.allergies = allergiesCsv == null || allergiesCsv.isBlank() ? new ArrayList<>() : Arrays.asList(allergiesCsv.split(";"));
        this.activeMedications = activeMedicationsCsv == null || activeMedicationsCsv.isBlank() ? new ArrayList<>() : Arrays.asList(activeMedicationsCsv.split(";"));
    }
    public List<String> getAllergies() { return allergies; }
    public void setAllergies(List<String> allergies) { this.allergies = allergies; }
    public List<String> getActiveMedications() { return activeMedications; }
    public void setActiveMedications(List<String> meds) { this.activeMedications = meds; }

    public String getPatientId()         { return patientId; }
    public String getDiagnosis()         { return diagnosis; }
    public String getRoomNumber()        { return roomNumber; }
    public String getAdmissionDate()     { return admissionDate; }
    public String getInsuranceProvider() { return insuranceProvider; }
    public String getStatus()            { return status; }
    public String getDischargeDate()     { return dischargeDate; }

    public void setPatientId(String patientId)                { this.patientId = patientId; }
    public void setDiagnosis(String diagnosis)                { this.diagnosis = diagnosis; }
    public void setRoomNumber(String roomNumber)              { this.roomNumber = roomNumber; }
    public void setAdmissionDate(String admissionDate)        { this.admissionDate = admissionDate; }
    public void setInsuranceProvider(String insuranceProvider){ this.insuranceProvider = insuranceProvider; }
    public void setStatus(String status)                      { this.status = status; }
    public void setDischargeDate(String dischargeDate)        { this.dischargeDate = dischargeDate; }

    @Override
    public String toString() {
        String base = super.toString()
                + ", Patient ID: " + patientId
                + ", Diagnosis: " + diagnosis
                + ", Room: " + roomNumber
                + ", Admitted: " + admissionDate
                + ", Insurance: " + insuranceProvider
                + ", Status: " + status;
        if (!allergies.isEmpty()) {
            base += ", Allergies: " + String.join(";", allergies);
        }
        if (!activeMedications.isEmpty()) {
            base += ", Active Medications: " + String.join(";", activeMedications);
        }
        if ("discharged".equalsIgnoreCase(status)) {
            base += ", Discharged: " + dischargeDate;
        }
        return base;
    }
}
