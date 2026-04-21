package objects;

public class Billing {
    public String PatientId;
    public String Name;
    public String MedicalCare;
    public String InsuranceProvider;
    public boolean PayInFull;
    public int Bill;
    public Billing(String PatientId, String Name, String MedicalCare, String InsuranceProvider, boolean PayInFull, int Bill)
    {
        this.PatientId = PatientId;
        this.Name = Name;
        this.MedicalCare = MedicalCare;
        this.InsuranceProvider = InsuranceProvider;
        this.PayInFull = PayInFull;
        this.Bill = Bill;
    }

    public String getPatientId() {
        return PatientId;
    }
    public String getName()
    {
        return Name;
    }

    public String getMedicalInfo()
    {
        return MedicalCare;
    }
    public String getInsuranceProvider() {
        return InsuranceProvider;
    }
    public boolean getPayInFull() {
        return PayInFull;
    }
    public int getBill() {
        return Bill;
    }

    public String toString() {
        String base = "Patient ID: " + PatientId
                + " | Name: " + Name
                + " | Medical: " + MedicalCare
                + " | Insurance: " + InsuranceProvider
                + " | Paid In Full: " + PayInFull
                + " | Amount: $" + Bill;
        return base;
    }
}
