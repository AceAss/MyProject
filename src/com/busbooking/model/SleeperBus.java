package com.busbooking.model;

import java.time.LocalTime;

/** AC sleeper bus with fewer (30) berths. */
public class SleeperBus extends Bus {                   // [F14]

    private static final long serialVersionUID = 1L;

    private static final double BERTH_CHARGE = 150.0;
    private static final int SLEEPER_SEATS = 30;

    public SleeperBus(String busNumber, Route route, LocalTime departureTime, Driver driver) {
        super(busNumber, BusType.AC_SLEEPER, route, departureTime, driver, SLEEPER_SEATS);  // [F15]
    }

    @Override
    public double calculateFare() {                     // [F16]
        return getRoute().getDistanceKm() * getType().getRatePerKm() + BERTH_CHARGE;
    }

    @Override
    public String getAmenities() {                      // [F16]
        return "AC, Flat berths, Blanket & pillow, Reading light, Charging point";
    }
}
