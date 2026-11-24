package model;

import interfaces.Displayable;

import java.util.Objects;

/**
 * A bank customer. Customer IS-A Person (inheritance) and adds a customer ID.
 *
 * Concepts shown:
 *  - Inheritance with super(...) constructor call.
 *  - Method overriding (getDescription, toString, equals, hashCode).
 *  - Interface implementation (Displayable).
 *  - Comparable: the "natural order" of customers is by customer ID.
 *    Other orders (by name, by age) are created with Comparator in the services.
 */
public class Customer extends Person implements Displayable, Comparable<Customer> {

    private String customerId;

    /** No-argument constructor, used when the object is filled in step by step. */
    public Customer() {
        super();
    }

    /** Parameterized constructor. */
    public Customer(String customerId, String name, int age, String email, String phone, String address) {
        super(name, age, email, phone, address);   // let Person store the common fields
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /** Overrides Person.getDescription() - runtime polymorphism. */
    @Override
    public String getDescription() {
        return "Customer " + customerId + ": " + getName();
    }

    @Override
    public String getDisplayText() {
        // StringBuilder is used because we join many pieces of text.
        // Plain String "+" in a loop would create a new String object each time (Strings are immutable).
        StringBuilder text = new StringBuilder();
        text.append("Customer ID : ").append(customerId).append('\n');
        text.append("Full Name   : ").append(getName()).append('\n');
        text.append("Age         : ").append(getAge()).append('\n');
        text.append("Email       : ").append(getEmail()).append('\n');
        text.append("Phone       : ").append(getPhone()).append('\n');
        text.append("Address     : ").append(getAddress());
        return text.toString();
    }

    @Override
    public String[] toTableRow() {
        // Integer.toString / String.valueOf turn the int age into text (wrapper class use)
        return new String[] {customerId, getName(), String.valueOf(getAge()), getEmail(), getPhone(), getAddress()};
    }

    /** Natural ordering used by Collections.sort(list) and TreeSet. */
    @Override
    public int compareTo(Customer other) {
        return this.customerId.compareTo(other.customerId);
    }

    /** Two Customer objects are equal when their IDs are equal (needed for HashSet / HashMap). */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Customer)) {
            return false;
        }
        Customer that = (Customer) other;
        return Objects.equals(customerId, that.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    @Override
    public String toString() {
        return customerId + " - " + getName();
    }
}
