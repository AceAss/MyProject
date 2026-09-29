package com.busbooking.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.busbooking.exception.PaymentFailedException;
import com.busbooking.util.DateFmt;

/**
 * One ticket booking: who, which trip, which seats, how much.
 */
public class Booking implements Payable, Serializable {               // [F18] class implements interface  [F01] encapsulated

    private static final long serialVersionUID = 1L;

    private static int bookingCounter = 1000;           // [F11] static counter used to build PNRs

    private final String pnr;
    private final Passenger passenger;
    private final Trip trip;                            // this bus on this date (not just "the bus")
    private final int[] seatNumbers;                    // [F07] array (of primitives)
    private final FareBreakdown fare;
    private final String couponCode;                    // "" when no coupon was used
    private final LocalDateTime bookedAt;
    private boolean paid;
    private BookingStatus status;
    private double refundAmount;                        // 0 until the booking is cancelled

    public Booking(Passenger passenger, Trip trip, int[] seatNumbers,
                   FareBreakdown fare, String couponCode, LocalDateTime bookedAt) {
        bookingCounter++;
        this.pnr = "PNR" + bookingCounter;
        this.passenger = passenger;
        this.trip = trip;
        this.seatNumbers = seatNumbers.clone();
        this.fare = fare;
        this.couponCode = couponCode;
        this.bookedAt = bookedAt;
        this.paid = false;
        this.status = BookingStatus.CONFIRMED;
        this.refundAmount = 0.0;
    }

    /** PNR is the ticket's identity - it is final so no subclass can change how it is reported. */
    public final String getPnr() {                      // [F20] final method
        return pnr;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public Trip getTrip() {
        return trip;
    }

    public int[] getSeatNumbers() {
        return seatNumbers.clone();
    }

    public FareBreakdown getFare() {
        return fare;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    /** Marks the ticket cancelled and records how much money was given back. */
    public void markCancelled(double refund) {
        this.status = BookingStatus.CANCELLED;
        this.refundAmount = refund;
    }

    /** The operator cancelled the whole trip: the passenger gets everything back. */
    public void markTripCancelled(double refund) {          // [V2-5]
        this.status = BookingStatus.TRIP_CANCELLED;
        this.refundAmount = refund;
    }

    public static int getTotalBookingsCreated() {       // [F11] static method
        return bookingCounter - 1000;
    }

    /** Continue PNR numbering after the highest PNR already issued (used after load / reset; 1000 = none yet). */
    public static void restoreCounter(int lastPnrNumber) {
        bookingCounter = Math.max(1000, lastPnrNumber);
    }

    public String getSeatsAsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seatNumbers.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(seatNumbers[i]);
        }
        return sb.toString();
    }

    // ---- Payable implementation ----

    @Override
    public double getPayableAmount() {
        return fare.getTotal();
    }

    @Override
    public void pay(double amountTendered) throws PaymentFailedException {     // [V2-2] throws
        if (status != BookingStatus.CONFIRMED) {
            throw new PaymentFailedException("Booking " + pnr + " is " + status);          // [V2-2] throw
        }
        if (amountTendered < fare.getTotal()) {
            throw new PaymentFailedException(fare.getTotal(), amountTendered);
        }
        paid = true;
    }

    @Override
    public boolean isPaid() {
        return paid;
    }

    // ---- display ----

    private static String row(String label, String value) {
        return String.format("| %-10s: %-34.34s |%n", label, value);
    }

    /** Multi-line printable ticket. */
    public String getTicketText() {
        Bus bus = trip.getBus();
        String border = "+------------------------------------------------+" + System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append(border);
        sb.append(String.format("| %-46s |%n", "            BUS TICKET - " + status));
        sb.append(border);
        sb.append(row("PNR", pnr));
        sb.append(row("Passenger", passenger.getName()));
        sb.append(row("Phone", passenger.getPhone()));
        sb.append(row("Trip", trip.getTripId()));
        sb.append(row("Bus", bus.getBusNumber() + " (" + bus.getType() + ")"));
        sb.append(row("Route", bus.getRoute().toString()));
        sb.append(row("Departure", DateFmt.dateTime(trip.getDeparture())));
        sb.append(row("Seats", getSeatsAsString()));
        sb.append(row("Fare", String.format("Rs.%.2f (%d x Rs.%.2f)",
                fare.getBaseFare(), fare.getSeats(), fare.getFarePerSeat())));
        if (fare.getConcessionAmount() > 0) {
            sb.append(row("Concession", String.format("-Rs.%.2f (%.0f%%)",
                    fare.getConcessionAmount(), fare.getConcessionRate() * 100)));
        }
        if (fare.getCouponAmount() > 0) {
            sb.append(row("Coupon", String.format("-Rs.%.2f (%s)", fare.getCouponAmount(), couponCode)));
        }
        sb.append(row("GST", String.format("+Rs.%.2f", fare.getGst())));
        if (Math.abs(fare.getRoundOff()) >= 0.005) {
            sb.append(row("Round off", String.format("%+.2f", fare.getRoundOff())));
        }
        sb.append(row("Amount", String.format("Rs.%.2f", fare.getTotal())));
        sb.append(row("Booked on", DateFmt.dateTime(bookedAt)));
        if (status != BookingStatus.CONFIRMED) {
            sb.append(row("Refunded", String.format("Rs.%.2f", refundAmount)));
        }
        sb.append(border);
        return sb.toString();
    }

    @Override
    public String toString() {                          // [F19]
        return String.format("%-8s %-16s %-5s %-10s %s  Seats: %-12s Rs.%-9.2f %s",
                pnr, passenger.getName(), trip.getTripId(), trip.getBus().getBusNumber(),
                DateFmt.date(trip.getDate()), getSeatsAsString(), fare.getTotal(), status);
    }

    @Override
    public boolean equals(Object obj) {                 // [F19]
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Booking)) {
            return false;
        }
        return pnr.equalsIgnoreCase(((Booking) obj).pnr);
    }

    @Override
    public int hashCode() {
        return pnr.hashCode();
    }
}
