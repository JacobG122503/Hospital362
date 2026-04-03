package objects;

public class JobApplicant extends Person {
    private final String desiredDepartment;
    private final String desiredRole;

    public JobApplicant(
            String name,
            int age,
            String gender,
            String phoneNumber,
            String address,
            String desiredDepartment,
            String desiredRole
    ) {
        super(name, age, gender, phoneNumber, address);
        this.desiredDepartment = desiredDepartment;
        this.desiredRole = desiredRole;
    }

    public String getDesiredDepartment() {
        return desiredDepartment;
    }

    public String getDesiredRole() {
        return desiredRole;
    }

    public String getApplicantKey() {
        return (getName().trim() + "|" + getPhoneNumber().trim()).toLowerCase();
    }
}