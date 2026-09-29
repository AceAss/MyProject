package com.busbooking.model;

/**
 * Anything that can receive a fare concession (implemented by Passenger).
 */
public interface Discountable {                         // [F18] interface #2
    double SENIOR_RATE = 0.20;                          // interface fields are implicitly public static final
    double STUDENT_RATE = 0.10;

    double getDiscountRate();

    double applyDiscount(double amount);
}
