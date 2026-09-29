package com.busbooking.model;

/** Life-cycle state of a booking. */
public enum BookingStatus {                             // [F05] second enum (with a label field)
    CONFIRMED("CONFIRMED"),
    CANCELLED("CANCELLED"),                             // the passenger cancelled (refund depends on the time left)
    TRIP_CANCELLED("TRIP CANCELLED");                   // the operator cancelled the trip (refunded in full)

    private final String label;

    BookingStatus(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
