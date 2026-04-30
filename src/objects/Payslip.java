package objects;

public class Payslip {
    private String employeeId;
    private String employeeName;
    private String periodStart;
    private String periodEnd;
    private double amount;
    private String processedDate;

    public Payslip(String employeeId, String employeeName, String periodStart,
                   String periodEnd, double amount, String processedDate) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.amount = amount;
        this.processedDate = processedDate;
    }

    public String getEmployeeId()    { return employeeId; }
    public String getEmployeeName()  { return employeeName; }
    public String getPeriodStart()   { return periodStart; }
    public String getPeriodEnd()     { return periodEnd; }
    public double getAmount()        { return amount; }
    public String getProcessedDate() { return processedDate; }

    @Override
    public String toString() {
        return "Employee: " + employeeName
                + " (ID: " + employeeId + ")"
                + " | Period: " + periodStart + " to " + periodEnd
                + " | Amount: $" + String.format("%.2f", amount)
                + " | Processed: " + processedDate;
    }
}