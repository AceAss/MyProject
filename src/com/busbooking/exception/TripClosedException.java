package com.busbooking.exception;

/** The trip can no longer be booked or cancelled: the bus has already left, or the operator cancelled the trip. */
public class TripClosedException extends BookingException {          // [V2-2] [V2-3] [F14]

    private static final long serialVersionUID = 1L;

    private final String tripId;

    public TripClosedException(String tripId, String reason) {
        super("Trip " + tripId + " " + reason);             // [F15]
        this.tripId = tripId;
    }

    public String getTripId() {
        return tripId;
    }
}
