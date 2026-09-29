package com.busbooking.exception;

/** A seat does not exist, is already taken, or the trip has no free seats left. */
public class SeatUnavailableException extends BookingException {     // [V2-2] [F14]

    private static final long serialVersionUID = 1L;

    private final int seatNo;                               // -1 when the problem is not about one particular seat

    public SeatUnavailableException(int seatNo, String reason) {
        super("Seat " + seatNo + " " + reason);             // [F15]
        this.seatNo = seatNo;
    }

    public SeatUnavailableException(String message) {
        super(message);                                     // [F15]
        this.seatNo = -1;
    }

    public int getSeatNo() {
        return seatNo;
    }
}
