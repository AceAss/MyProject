package com.busbooking.model;

/**
 * A bus driver assigned to a bus.
 */
public class Driver extends Person {                    // [F14] subclass #2

    private static final long serialVersionUID = 1L;

    private String licenseNumber;
    private int experienceYears;

    public Driver() {                                   // [F09] default constructor
        super();
        this.licenseNumber = "N/A";
        this.experienceYears = 0;
    }

    public Driver(String name, String phone, int age, String licenseNumber, int experienceYears) {
        super(name, phone, age);                        // [F15] super() constructor call
        this.licenseNumber = licenseNumber;
        this.experienceYears = experienceYears;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    @Override
    public String getRole() {                           // [F16] overriding
        return "Driver";
    }

    @Override
    public String describe() {                          // [F16] overriding
        return super.describe() + " | Licence: " + licenseNumber + " | Exp: " + experienceYears + " yrs";  // [F15]
    }
}
