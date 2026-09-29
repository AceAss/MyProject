package com.busbooking.exception; // [F21] custom package

/**
 * Root of every business-rule failure in the system.
 * It is a CHECKED exception (it extends Exception): a method that can throw it must say
 * "throws BookingException", and every caller must either catch it or pass it on.
 */
public class BookingException extends Exception {          // [V2-2] [F14] base class of the exception hierarchy

    private static final long serialVersionUID = 1L;

    public BookingException(String message) {              // [F09] overloaded constructors
        super(message);                                     // [F15] super(...) hands the message to Exception
    }

    /** Wraps a lower-level error (exception chaining) so the original cause is not lost. */
    public BookingException(String message, Throwable cause) {
        super(message, cause);                              // [F15]
    }
}
