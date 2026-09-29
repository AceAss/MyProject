package com.busbooking.service;

import com.busbooking.model.Coupon;
import com.busbooking.model.Discountable;
import com.busbooking.model.FareBreakdown;
import com.busbooking.model.RefundTier;

/**
 * Fare, GST and refund arithmetic.
 */
public final class FareCalculator {                     // [F20] final class

    // final because this is a pure utility class of static methods - there is nothing sensible to extend.

    public static final double GST_RATE = 0.05;         // [F02] final constant

    private FareCalculator() {
    }

    /** Bill with a passenger concession only (no coupon). */
    public static FareBreakdown breakdown(double farePerSeat, int seats, Discountable concession) {   // [F10] overloading
        return breakdown(farePerSeat, seats, concession, Coupon.NONE);
    }

    /**
     * Bill with a concession AND a coupon. The two stack in this order:
     * base fare -> concession -> coupon (on the reduced amount) -> GST on what is left.
     */
    public static FareBreakdown breakdown(double farePerSeat, int seats,
                                          Discountable concession, Discountable coupon) {   // [V2-4] [F10] [F18] two interface parameters
        double base = farePerSeat * seats;
        double afterConcession = concession.applyDiscount(base);        // [F18] Passenger's rule runs
        double afterCoupon = coupon.applyDiscount(afterConcession);     // [F18] Coupon's rule runs on the reduced amount
        double gst = afterCoupon * GST_RATE;                            // [F03] '*' before any '+'
        double total = toWholeRupees(afterCoupon + gst);
        return new FareBreakdown(farePerSeat, seats, concession.getDiscountRate(),
                base - afterConcession, afterConcession - afterCoupon, gst, total);
    }

    /** Refund for a paid ticket cancelled in the given time window, in whole rupees. */
    public static double refundAmount(double amountPaid, RefundTier tier) {     // [V2-3] [F10] overloading
        return toWholeRupees(amountPaid * tier.getRefundRate());
    }

    /** Same, working the tier out from the minutes left before departure. */
    public static double refundAmount(double amountPaid, long minutesToDeparture) {
        return refundAmount(amountPaid, RefundTier.forMinutesLeft(minutesToDeparture));
    }

    /** Bills are settled in whole rupees. */
    public static int toWholeRupees(double amount) {
        long rounded = Math.round(amount);              // [F02] long
        return (int) rounded;                           // [F04] explicit narrowing cast long -> int
    }
}
