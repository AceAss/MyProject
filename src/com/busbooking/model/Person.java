package com.busbooking.model; // [F21] custom package

import java.io.Serializable;

/**
 * Base class for every human in the system (Passenger, Driver).
 */
public class Person implements Serializable {                                   // [F14] base class of the hierarchy

    private static final long serialVersionUID = 1L;

    // ---- instance variables: one copy per object, private => encapsulation ----
    private String name;                                // [F01] [F02] String
    private String phone;
    private int age;                                    // [F02] int

    // ---- static variable: ONE copy shared by the whole class ----
    private static int totalPeople = 0;                 // [F11] static counter [F02] static scope

    /** Default constructor - delegates to the parameterised one. */
    public Person() {                                   // [F09] default constructor
        this("Unknown", "0000000000", 0);               // [F12] this() constructor chaining
    }

    public Person(String name, String phone, int age) { // [F09] parameterised constructor
        this.name = name;                               // [F12] 'this' resolves parameter shadowing
        this.phone = phone;
        this.age = age;
        totalPeople++;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {    // [F13] String methods
            return;                                     // [F06] return (ignore invalid value)
        }
        this.name = name.trim();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (phone == null || !phone.matches("\\d{10}")) {
            return;
        }
        this.phone = phone;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        if (age < 0 || age > 120) {
            return;
        }
        this.age = age;
    }

    /** Role label - overridden by subclasses. */
    public String getRole() {
        return "Person";
    }

    /** One-line description; uses getRole(), so the SUBCLASS version runs (dynamic binding). */
    public String describe() {
        return String.format("%-10s %-20s Age: %-3d Phone: %s", getRole(), name, age, phone);   // [F08] String.format
    }

    public static int getTotalPeople() {                // [F11] static method
        return totalPeople;
    }

    /** Constructors are not run when objects are read back from a save file, so the counter is set by hand. */
    public static void restoreTotalPeople(int total) {
        totalPeople = total;
    }

    @Override
    public String toString() {                          // [F19] toString override
        return getRole() + "[" + name + ", " + phone + "]";
    }

    @Override
    public boolean equals(Object obj) {                 // [F19] equals override
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Person other = (Person) obj;
        return phone.equals(other.phone);               // phone number identifies a person
    }

    @Override
    public int hashCode() {
        return phone.hashCode();
    }
}
