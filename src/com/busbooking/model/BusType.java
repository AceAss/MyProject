package com.busbooking.model;

/**
 * Category of bus. Each constant carries its own label and per-km rate.
 */
public enum BusType {                                   // [F05] enum with fields + constructor
    ORDINARY("Ordinary", 1.50),
    AC_SEATER("AC Seater", 2.40),
    AC_SLEEPER("AC Sleeper", 3.10);

    private final String label;
    private final double ratePerKm;

    BusType(String label, double ratePerKm) {
        this.label = label;
        this.ratePerKm = ratePerKm;
    }

    public String getLabel() {
        return label;
    }

    public double getRatePerKm() {
        return ratePerKm;
    }

    @Override
    public String toString() {
        return label;
    }
}
