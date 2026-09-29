package com.busbooking.model;

import java.time.LocalTime;

/** Air-conditioned seater bus. */
public class AcBus extends Bus {                        // [F14]

    private static final long serialVersionUID = 1L;

    private static final double AC_SURCHARGE = 60.0;    // [F02] final constant

    public AcBus(String busNumber, Route route, LocalTime departureTime, Driver driver) {
        super(busNumber, BusType.AC_SEATER, route, departureTime, driver);  // [F15]
    }

    @Override
    public double calculateFare() {                     // [F16]
        // [F03] precedence: distance * rate is computed first, then the surcharge is added
        return getRoute().getDistanceKm() * getType().getRatePerKm() + AC_SURCHARGE;
    }

    @Override
    public String getAmenities() {                      // [F16]
        return "AC, Reclining seats, Charging point, Water bottle";
    }
}
