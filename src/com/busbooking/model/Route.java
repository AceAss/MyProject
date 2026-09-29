package com.busbooking.model;

import java.io.Serializable;

/**
 * A journey between two cities.
 */
public class Route implements Serializable {                                   // [F01] encapsulated

    private static final long serialVersionUID = 1L;

    private String source;
    private String destination;
    private int distanceKm;

    public Route() {                                    // [F09] default constructor
        this("Unknown", "Unknown", 0);
    }

    public Route(String source, String destination, int distanceKm) {   // [F09] parameterised
        this.source = source;
        this.destination = destination;
        this.distanceKm = distanceKm;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(int distanceKm) {
        this.distanceKm = distanceKm;
    }

    @Override
    public String toString() {
        return source + " -> " + destination + " (" + distanceKm + " km)";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Route)) {
            return false;
        }
        Route other = (Route) obj;
        return source.equalsIgnoreCase(other.source)    // [F13] String comparison
                && destination.equalsIgnoreCase(other.destination);
    }

    @Override
    public int hashCode() {
        return (source.toLowerCase() + destination.toLowerCase()).hashCode();
    }
}
