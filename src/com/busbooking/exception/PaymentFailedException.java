package com.busbooking.exception;

/** Money tendered does not cover the amount due, or the booking can no longer be paid. */
public class PaymentFailedException extends BookingException {       // [V2-2] [F14]

    private static final long serialVersionUID = 1L;

    private final double amountDue;
    private final double amountTendered;

    public PaymentFailedException(double amountDue, double amountTendered) {
        super(String.format("Rs.%.2f tendered but Rs.%.2f is due", amountTendered, amountDue));   // [F15]
        this.amountDue = amountDue;
        this.amountTendered = amountTendered;
    }

    public PaymentFailedException(String message) {
        super(message);                                     // [F15]
        this.amountDue = 0.0;
        this.amountTendered = 0.0;
    }

    /** How much more money was needed (0 if the failure was not about the amount). */
    public double getShortfall() {
        return Math.max(0.0, amountDue - amountTendered);
    }
}
