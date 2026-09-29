package com.busbooking.model;

/**
 * A customer who books tickets. Eligible for concessions (Discountable).
 */
public class Passenger extends Person implements Discountable {   // [F14] subclass #1

    private static final long serialVersionUID = 1L;

    private static int passengerCounter = 0;            // [F11] static
    private final String passengerId;
    private boolean student;

    public Passenger() {                                // [F09] default constructor
        this("Unknown", "0000000000", 0, false);        // [F12] this() chaining
    }

    public Passenger(String name, String phone, int age) {
        this(name, phone, age, false);                  // [F12] this() chaining
    }

    public Passenger(String name, String phone, int age, boolean student) {
        super(name, phone, age);                        // [F15] super() constructor call
        this.student = student;
        passengerCounter++;
        this.passengerId = "P" + String.format("%03d", passengerCounter);
    }

    /** Continue numbering after the highest passenger ID already in use (used after load / reset). */
    public static void restoreCounter(int lastIssued) {
        passengerCounter = lastIssued;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public boolean isStudent() {
        return student;
    }

    public void setStudent(boolean student) {
        this.student = student;
    }

    @Override
    public double getDiscountRate() {
        if (getAge() >= 60) {                           // [F06] if-else chain
            return SENIOR_RATE;
        } else if (student) {
            return STUDENT_RATE;
        } else {
            return 0.0;
        }
    }

    @Override
    public double applyDiscount(double amount) {
        return amount - amount * getDiscountRate();     // [F03] '*' is evaluated before '-' (precedence)
    }

    @Override
    public String getRole() {                           // [F16] overriding
        return "Passenger";
    }

    @Override
    public String describe() {                          // [F16] overriding
        return super.describe()                         // [F15] super.method() call
                + " | ID: " + passengerId
                + (student ? " | Student" : "");        // ternary operator
    }
}
