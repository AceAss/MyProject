package com.busbooking.model;

import java.io.Serializable;

/**
 * An itemised bill for one booking: base fare, concession, coupon, GST and the final total.
 * Immutable (all fields final), hence the class itself is final too.
 */
public final class FareBreakdown implements Serializable {                      // [V2-4] [F20] final class - a plain value object, nothing to extend

    private static final long serialVersionUID = 1L;

    private final double farePerSeat;
    private final int seats;
    private final double concessionRate;
    private final double concessionAmount;              // rupees taken off by the senior / student concession
    private final double couponAmount;                  // rupees taken off by the coupon
    private final double gst;
    private final double total;                         // whole rupees

    public FareBreakdown(double farePerSeat, int seats, double concessionRate,
                         double concessionAmount, double couponAmount, double gst, double total) {
        this.farePerSeat = farePerSeat;
        this.seats = seats;
        this.concessionRate = concessionRate;
        this.concessionAmount = concessionAmount;
        this.couponAmount = couponAmount;
        this.gst = gst;
        this.total = total;
    }

    public double getFarePerSeat() {
        return farePerSeat;
    }

    public int getSeats() {
        return seats;
    }

    public double getBaseFare() {
        return farePerSeat * seats;
    }

    public double getConcessionRate() {
        return concessionRate;
    }

    public double getConcessionAmount() {
        return concessionAmount;
    }

    public double getCouponAmount() {
        return couponAmount;
    }

    /** Amount GST is charged on. */
    public double getTaxable() {
        return getBaseFare() - concessionAmount - couponAmount;
    }

    public double getGst() {
        return gst;
    }

    /** Difference between the exact amount and the whole-rupee total. */
    public double getRoundOff() {
        return total - getTaxable() - gst;
    }

    public double getTotal() {
        return total;
    }
}
