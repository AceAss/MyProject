package com.busbooking.model;

/**
 * How much of the fare comes back, depending on how long is left before the bus departs.
 * Constants are listed from the earliest window to the latest, so the first match wins.
 */
public enum RefundTier {                                // [V2-3] [F05] enum with fields + constructor + static method
    EARLY(48, 0.90, "48 hours or more before departure"),
    STANDARD(12, 0.75, "12 to 48 hours before departure"),
    LATE(2, 0.50, "2 to 12 hours before departure"),
    NONE(0, 0.00, "less than 2 hours before departure");

    private final int minHours;                         // window starts at this many hours before departure
    private final double refundRate;
    private final String label;

    RefundTier(int minHours, double refundRate, String label) {
        this.minHours = minHours;
        this.refundRate = refundRate;
        this.label = label;
    }

    public int getMinHours() {
        return minHours;
    }

    public double getRefundRate() {
        return refundRate;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Picks the tier for the time left. Minutes (not whole hours) are compared so that
     * 47 h 59 m is correctly still in the 75 % window.
     */
    public static RefundTier forMinutesLeft(long minutesLeft) {
        for (RefundTier tier : values()) {                          // [F06] for-each over the enum constants
            if (minutesLeft >= tier.minHours * 60L) {
                return tier;                                        // [F06] return from inside a loop
            }
        }
        return NONE;                                                // bus already left (negative minutes)
    }
}
