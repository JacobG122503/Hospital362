package objects;

public class Employee extends Person {
    private String employeeId;
    private String department;
    private String role;
    private double salary;
    private String hireDate;

    public Employee(String name, int age, String gender, String phoneNumber, String address,
                    String employeeId, String department, String role, double salary, String hireDate) {
        super(name, age, gender, phoneNumber, address);
        this.employeeId = employeeId;
        this.department = department;
        this.role = role;
        this.salary = salary;
        this.hireDate = hireDate;
    }

    public String getEmployeeId() { return employeeId; }
    public String getDepartment() { return department; }
    public String getRole() { return role; }
    public double getSalary() { return salary; }
    public String getHireDate() { return hireDate; }

    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setDepartment(String department) { this.department = department; }
    public void setRole(String role) { this.role = role; }
    public void setSalary(double salary) { this.salary = salary; }
    public void setHireDate(String hireDate) { this.hireDate = hireDate; }

    @Override
    public String toString() {
        return super.toString() + ", Employee ID: " + employeeId + ", Dept: " + department +
               ", Role: " + role + ", Salary: $" + String.format("%.2f", salary) +
               ", Hired: " + hireDate;
    }
}
