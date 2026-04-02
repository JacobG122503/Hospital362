package objects;

public class Person {
    protected String name;
    protected int age;
    protected String gender;
    protected String phoneNumber;
    protected String address;

    public Person(String name, int age, String gender, String phoneNumber, String address) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }

    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setGender(String gender) { this.gender = gender; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String toString() {
        return "Name: " + name + ", Age: " + age + ", Gender: " + gender +
               ", Phone: " + phoneNumber + ", Address: " + address;
    }
}
