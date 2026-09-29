package com.busbooking.model;

/** Whether a trip is still running as planned. (Whether it has already left is worked out from the clock.) */
public enum TripStatus {                                // [F05] enum
    SCHEDULED,
    CANCELLED
}
