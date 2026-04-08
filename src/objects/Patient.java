package objects;

public class Patient extends Person {
    private String patientId;
    private String diagnosis;
    private String roomNumber;
    private String admissionDate;
    private String insuranceProvider;
    private String status;
    private String dischargeDate;

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
    }

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

        if ("discharged".equalsIgnoreCase(status)) {
            base += ", Discharged: " + dischargeDate;
        }

        return base;
    }
}
