package objects;

public class Surgery {
    private String procedureName;
    private String patientId;
    private String patientName;
    private String surgeonName;
    private String date;
    private String operatingRoom;

    public Surgery(String procedureName, String patientId, String patientName,
                   String surgeonName, String date, String operatingRoom) {
        this.procedureName = procedureName;
        this.patientId = patientId;
        this.patientName = patientName;
        this.surgeonName = surgeonName;
        this.date = date;
        this.operatingRoom = operatingRoom;
    }

    public String getProcedureName()  { return procedureName; }
    public String getPatientId()      { return patientId; }
    public String getPatientName()    { return patientName; }
    public String getSurgeonName()    { return surgeonName; }
    public String getDate()           { return date; }
    public String getOperatingRoom()  { return operatingRoom; }

    @Override
    public String toString() {
        return "Procedure: " + procedureName
                + ", Patient: " + patientName
                + " (ID: " + patientId + ")"
                + ", Surgeon: " + surgeonName
                + ", Date: " + date
                + ", Room: " + operatingRoom;
    }
}