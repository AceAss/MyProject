package com.busbooking.model;

import com.busbooking.exception.PaymentFailedException;

/**
 * Anything that has an amount to be paid (implemented by Booking).
 */
public interface Payable {                              // [F18] interface #1
    double getPayableAmount();

    /** Accepts the payment, or throws PaymentFailedException if the tendered amount does not cover it. */
    void pay(double amountTendered) throws PaymentFailedException;     // [V2-2] 'throws' replaces V1's boolean result

    boolean isPaid();
}
