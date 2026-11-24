package model;

/**
 * Person is the parent (super) class for Customer.
 * It holds details that every person has, whether or not they bank with us.
 *
 * Concepts shown:
 *  - Encapsulation: all fields are private and reached through getters/setters.
 *  - Constructors: a no-argument constructor and a parameterized constructor.
 *  - this keyword: this.name = name separates the field from the parameter.
 *  - Inheritance: Customer extends Person.
 *  - Method overriding: getDescription() is overridden in Customer.
 */
public class Person {

    private String name;
    private int age;
    private String email;
    private String phone;
    private String address;

    /** No-argument (default) constructor. */
    public Person() {
        this("Unknown", 18, "", "", "");   // calls the parameterized constructor below
    }

    /** Parameterized constructor. */
    public Person(String name, int age, String email, String phone, String address) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    /** Short description. Customer overrides this method. */
    public String getDescription() {
        return "Person: " + name + " (age " + age + ")";
    }

    // ---------------- Getters and setters ----------------

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
