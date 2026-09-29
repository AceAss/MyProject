package com.busbooking.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;

import com.busbooking.exception.SeatUnavailableException;
import com.busbooking.exception.TripClosedException;
import com.busbooking.util.DateFmt;

/**
 * One Bus on one date. Every Trip has its OWN seat map, so seat 5 on Monday's bus
 * and seat 5 on Tuesday's bus are different seats.
 */
public class Trip implements Serializable {                                     // [V2-1] [F01] encapsulated

    private static final long serialVersionUID = 1L;

    /** Ready-made sort orders: {@code Arrays.sort(trips, Trip.BY_FARE)}. */
    public static final Comparator<Trip> BY_DEPARTURE = Comparator.comparing(Trip::getDeparture);
    public static final Comparator<Trip> BY_FARE =
            Comparator.comparingDouble(Trip::getFare).thenComparing(Trip::getDeparture);   // cheapest first, ties -> earliest

    private final String tripId;
    private final Bus bus;
    private final LocalDateTime departure;
    private final boolean[] seatBooked;                 // index 0 = seat 1 (one map per trip)
    private TripStatus status;

    /** A trip at the bus's usual departure time. */
    public Trip(String tripId, Bus bus, LocalDate date) {                       // [F09]
        this(tripId, bus, date, bus.getDepartureTime());                        // [F12] constructor chaining
    }

    /** A trip at a special departure time (e.g. an extra late service). */
    public Trip(String tripId, Bus bus, LocalDate date, LocalTime time) {
        this.tripId = tripId;                           // [F12]
        this.bus = bus;
        this.departure = LocalDateTime.of(date, time);
        this.seatBooked = new boolean[bus.getTotalSeats()];
        this.status = TripStatus.SCHEDULED;
    }

    // ---------------- seat handling ----------------

    /** Book a specific seat; throws if the number is invalid or the seat is taken. */
    public void bookSeat(int seatNo) throws SeatUnavailableException {         // [V2-2] [F10] overloading (by parameter list)
        if (seatNo < 1 || seatNo > seatBooked.length) {
            throw new SeatUnavailableException(seatNo, "does not exist (this bus has seats 1-" + seatBooked.length + ")");   // [V2-2] throw
        }
        if (seatBooked[seatNo - 1]) {
            throw new SeatUnavailableException(seatNo, "is already booked");
        }
        seatBooked[seatNo - 1] = true;
    }

    /** Auto-assign the first free seat and return its number; throws if the trip is full. */
    public int bookSeat() throws SeatUnavailableException {                    // [F10] overloading
        for (int i = 0; i < seatBooked.length; i++) {   // [F06] for loop
            if (!seatBooked[i]) {
                seatBooked[i] = true;
                return i + 1;                           // [F06] return from inside a loop
            }
        }
        throw new SeatUnavailableException("Trip " + tripId + " is full");
    }

    public void releaseSeat(int seatNo) {
        if (seatNo >= 1 && seatNo <= seatBooked.length) {
            seatBooked[seatNo - 1] = false;
        }
    }

    public boolean isSeatAvailable(int seatNo) {
        return seatNo >= 1 && seatNo <= seatBooked.length && !seatBooked[seatNo - 1];
    }

    public int getTotalSeats() {
        return seatBooked.length;
    }

    public int getBookedSeats() {
        int count = 0;                                  // [F02] LOCAL variable: exists only inside this method
        for (boolean taken : seatBooked) {              // [F06] for-each loop ('taken' is scoped to the loop)
            if (taken) {
                count++;
            }
        }
        return count;
    }

    public int getAvailableSeats() {
        return seatBooked.length - getBookedSeats();
    }

    public double getOccupancyPercent() {
        return (double) getBookedSeats() / seatBooked.length * 100;   // [F04] cast int->double  [F03] cast binds before '/', '/' before '*'
    }

    /** Draws a 2 + 2 seat layout: [ 5] = free, [XX] = booked. */
    public void printSeatMap() {
        System.out.println("Seat map for " + tripId + " (" + bus.getBusNumber() + ", "
                + DateFmt.dateTime(departure) + ")   ( [nn] = free, [XX] = booked )");
        for (int i = 0; i < seatBooked.length; i++) {
            String cell = seatBooked[i] ? "[XX]" : String.format("[%2d]", i + 1);
            System.out.print(cell + " ");
            int column = (i + 1) % 4;
            if (column == 2) {
                System.out.print("    ");               // aisle
            } else if (column == 0) {
                System.out.println();
            }
        }
        if (seatBooked.length % 4 != 0) {
            System.out.println();
        }
    }

    // ---------------- time and status ----------------

    public boolean hasDeparted(LocalDateTime now) {
        return !now.isBefore(departure);                // leaving exactly now counts as gone
    }

    /** Still bookable: not cancelled by the operator and not yet departed. */
    public boolean isOpen(LocalDateTime now) {
        return status == TripStatus.SCHEDULED && !hasDeparted(now);
    }

    /** Throws TripClosedException if nobody can book or cancel on this trip any more. */
    public void ensureOpen(LocalDateTime now) throws TripClosedException {     // [V2-3] [V2-2]
        if (status == TripStatus.CANCELLED) {
            throw new TripClosedException(tripId, "has been cancelled by the operator");
        }
        if (hasDeparted(now)) {
            throw new TripClosedException(tripId, "has already departed (" + DateFmt.dateTime(departure) + ")");
        }
    }

    public long minutesToDeparture(LocalDateTime now) {
        return Duration.between(now, departure).toMinutes();
    }

    /** "5h 30m" - for messages. */
    public String timeToDeparture(LocalDateTime now) {
        long minutes = minutesToDeparture(now);
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    public void cancel() {
        this.status = TripStatus.CANCELLED;
    }

    // ---------------- getters ----------------

    public String getTripId() {
        return tripId;
    }

    public Bus getBus() {
        return bus;
    }

    public LocalDateTime getDeparture() {
        return departure;
    }

    public LocalDate getDate() {
        return departure.toLocalDate();
    }

    public TripStatus getStatus() {
        return status;
    }

    /** Fare for one seat; asks the bus, so each bus type answers with its own rule (dynamic binding). */
    public double getFare() {                           // [F16] dynamic binding: AcBus / SleeperBus / OrdinaryBus version runs
        return bus.calculateFare();
    }

    /** One table row - the column headings are printed by BookingService.printTrips(). */
    @Override
    public String toString() {                          // [F19]
        return String.format("%-6s %-10s %-11s %-34s %-21s %2d/%-7d Rs.%.2f",
                tripId, bus.getBusNumber(), bus.getType().getLabel(), bus.getRoute(),
                DateFmt.dateTime(departure), getAvailableSeats(), seatBooked.length, getFare());
    }

    @Override
    public boolean equals(Object obj) {                 // [F19]
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Trip)) {
            return false;
        }
        return tripId.equalsIgnoreCase(((Trip) obj).tripId);
    }

    @Override
    public int hashCode() {
        return tripId.toLowerCase().hashCode();
    }
}
