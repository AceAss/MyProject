package com.busbooking.model;

import java.io.Serializable;
import java.time.LocalDate;

import com.busbooking.exception.InvalidCouponException;
import com.busbooking.util.DateFmt;

/**
 * A promo code: a percentage off, capped at a maximum rupee amount, valid until a date.
 * It is Discountable, just like Passenger, so the fare calculator can treat both the same way -
 * and the two STACK: concession first, then the coupon on what is left.
 */
public class Coupon implements Discountable, Serializable {           // [V2-4] [F18] second implementer of Discountable

    private static final long serialVersionUID = 1L;

    /** "No coupon" - lets callers avoid null. It never expires and takes nothing off. */
    public static final Coupon NONE = new Coupon("", 0.0, 0.0, LocalDate.MAX);

    private final String code;
    private final double rate;                          // 0.10 = 10 %
    private final double maxDiscount;                   // cap in rupees
    private final LocalDate validUntil;                 // last day the code works

    public Coupon(String code, double rate, double maxDiscount, LocalDate validUntil) {
        this.code = code.trim().toUpperCase();
        this.rate = rate;
        this.maxDiscount = maxDiscount;
        this.validUntil = validUntil;
    }

    public String getCode() {
        return code;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public boolean isNone() {
        return code.isEmpty();
    }

    /** Throws InvalidCouponException if the coupon has expired on the given day. */
    public void validateOn(LocalDate today) throws InvalidCouponException {
        if (today.isAfter(validUntil)) {
            throw new InvalidCouponException(code, "expired on " + DateFmt.date(validUntil));   // [V2-2] throw
        }
    }

    @Override
    public double getDiscountRate() {
        return rate;
    }

    @Override
    public double applyDiscount(double amount) {
        double off = Math.min(amount * rate, maxDiscount);     // [F03] '*' first, then the cap is applied
        return amount - off;
    }

    @Override
    public String toString() {                          // [F19]
        return String.format("%s (%.0f%% off, max Rs.%.0f, valid till %s)",
                code, rate * 100, maxDiscount, DateFmt.date(validUntil));
    }

    @Override
    public boolean equals(Object obj) {                 // [F19]
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Coupon)) {
            return false;
        }
        return code.equals(((Coupon) obj).code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
