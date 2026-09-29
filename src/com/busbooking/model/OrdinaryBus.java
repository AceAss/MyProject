package com.busbooking.model;

import java.time.LocalTime;

/** Non-AC pushback bus. */
public class OrdinaryBus extends Bus {                  // [F14] Bus subclass

    private static final long serialVersionUID = 1L;

    public OrdinaryBus(String busNumber, Route route, LocalTime departureTime, Driver driver) {
        super(busNumber, BusType.ORDINARY, route, departureTime, driver);   // [F15]
    }

    @Override
    public double calculateFare() {                     // [F16] overriding
        return getRoute().getDistanceKm() * getType().getRatePerKm();
    }

    @Override
    public String getAmenities() {                      // [F16]
        return "Pushback seats, Charging point";
    }
}
