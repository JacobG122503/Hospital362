package objects;

public class Patient extends Person {
    private String patientId;
    private String diagnosis;
    private String roomNumber;
    private String admissionDate;
    private String insuranceProvider;

    public Patient(String name, int age, String gender, String phoneNumber, String address,
                   String patientId, String diagnosis, String roomNumber, String admissionDate,
                   String insuranceProvider) {
        super(name, age, gender, phoneNumber, address);
        this.patientId = patientId;
        this.diagnosis = diagnosis;
        this.roomNumber = roomNumber;
        this.admissionDate = admissionDate;
        this.insuranceProvider = insuranceProvider;
    }

    public String getPatientId() { return patientId; }
    public String getDiagnosis() { return diagnosis; }
    public String getRoomNumber() { return roomNumber; }
    public String getAdmissionDate() { return admissionDate; }
    public String getInsuranceProvider() { return insuranceProvider; }

    public void setPatientId(String patientId) { this.patientId = patientId; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setAdmissionDate(String admissionDate) { this.admissionDate = admissionDate; }
    public void setInsuranceProvider(String insuranceProvider) { this.insuranceProvider = insuranceProvider; }

    @Override
    public String toString() {
        return super.toString() + ", Patient ID: " + patientId + ", Diagnosis: " + diagnosis +
               ", Room: " + roomNumber + ", Admitted: " + admissionDate +
               ", Insurance: " + insuranceProvider;
    }
}
