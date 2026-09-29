package com.busbooking.model;

import java.io.Serializable;
import java.time.LocalTime;

import com.busbooking.util.DateFmt;

/**
 * The VEHICLE: number plate, category, standard route, usual departure time, driver and seat count.
 * A Bus holds no bookings - the seats of one particular day belong to a Trip (this bus on one date).
 * Each kind of bus decides its own fare and amenities.
 */
public abstract class Bus implements Serializable {                             // [F17] abstract class  [F01] encapsulated

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_SEATS = 40;         // [F02] final constant
    private static int totalBuses = 0;                  // [F11] static counter

    private final String busNumber;
    private final BusType type;
    private Route route;
    private LocalTime departureTime;                    // the usual daily departure; a Trip may override it
    private Driver driver;
    private final int totalSeats;

    public Bus(String busNumber, BusType type, Route route, LocalTime departureTime, Driver driver) {   // [F09]
        this(busNumber, type, route, departureTime, driver, DEFAULT_SEATS);                             // [F12]
    }

    public Bus(String busNumber, BusType type, Route route, LocalTime departureTime, Driver driver, int totalSeats) {
        this.busNumber = busNumber;                     // [F12] this resolves shadowing
        this.type = type;
        this.route = route;
        this.departureTime = departureTime;
        this.driver = driver;
        this.totalSeats = totalSeats;
        totalBuses++;
    }

    /** Base fare for ONE seat on this bus. */
    public abstract double calculateFare();             // [F17] abstract method

    /** Comma-separated list of on-board facilities. */
    public abstract String getAmenities();              // [F17] abstract method

    // ---------------- getters / setters ----------------

    public String getBusNumber() {
        return busNumber;
    }

    public BusType getType() {
        return type;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public static int getTotalBuses() {                 // [F11] static method
        return totalBuses;
    }

    public static void restoreTotalBuses(int total) {
        totalBuses = total;
    }

    @Override
    public String toString() {                          // [F19]
        return String.format("%-10s %-12s %-34s daily %s  %2d seats  Rs.%.2f",
                busNumber, type.getLabel(), route, DateFmt.time(departureTime), totalSeats, calculateFare());
    }

    @Override
    public boolean equals(Object obj) {                 // [F19]
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Bus)) {
            return false;
        }
        return busNumber.equalsIgnoreCase(((Bus) obj).busNumber);
    }

    @Override
    public int hashCode() {
        return busNumber.toLowerCase().hashCode();
    }
}
