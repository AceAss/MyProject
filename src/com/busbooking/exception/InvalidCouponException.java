package com.busbooking.exception;

/** The coupon code is unknown or has expired. */
public class InvalidCouponException extends BookingException {       // [V2-2] [V2-4] [F14]

    private static final long serialVersionUID = 1L;

    private final String code;

    public InvalidCouponException(String code, String reason) {
        super("Coupon '" + code + "' " + reason);           // [F15]
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
