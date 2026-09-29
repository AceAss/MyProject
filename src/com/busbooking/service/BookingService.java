package com.busbooking.service; // [F21] custom package

import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;

import com.busbooking.exception.BookingException;
import com.busbooking.exception.InvalidCouponException;
import com.busbooking.exception.SeatUnavailableException;
import com.busbooking.model.AcBus;
import com.busbooking.model.Booking; // [F21] import from another custom package
import com.busbooking.model.BookingStatus;
import com.busbooking.model.Bus;
import com.busbooking.model.Coupon;
import com.busbooking.model.Discountable;
import com.busbooking.model.Driver;
import com.busbooking.model.FareBreakdown;
import com.busbooking.model.OrdinaryBus;
import com.busbooking.model.Passenger;
import com.busbooking.model.Person;
import com.busbooking.model.RefundTier;
import com.busbooking.model.Route;
import com.busbooking.model.SleeperBus;
import com.busbooking.model.Trip;
import com.busbooking.model.TripStatus;
import com.busbooking.util.DateFmt;
import com.busbooking.util.InputHelper;

/**
 * All business operations: buses, trips, people, coupons, reservations, cancellations, reports.
 * Every rule violation is reported by throwing a BookingException (or one of its subclasses).
 */
public class BookingService implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int MAX_SEATS_PER_BOOKING = 6;
    public static final int SAMPLE_SCHEDULE_DAYS = 7;
    public static final int MAX_SCHEDULE_DAYS = 60;

    // "what time is it now?" - injectable so refunds can be demonstrated. transient = NOT written to the save file;
    // DataStore hands the current clock back in through afterLoad().
    private transient Clock clock;

    // [F07] arrays of objects (grown with Arrays.copyOf when full)
    private Bus[] buses = new Bus[10];
    private int busCount = 0;
    private Trip[] trips = new Trip[50];
    private int tripCount = 0;
    private int tripCounter = 0;                        // numbers the trip IDs: T001, T002, ...
    private Booking[] bookings = new Booking[10];
    private int bookingCount = 0;
    private Person[] people = new Person[10];           // holds Drivers AND Passengers
    private int peopleCount = 0;
    private Coupon[] coupons = new Coupon[5];
    private int couponCount = 0;

    public BookingService() {                           // [F09] constructor overloading
        this(Clock.systemDefaultZone());                // [F12]
    }

    public BookingService(Clock clock) {
        this.clock = clock;
    }

    // ---------------- save / load support ----------------

    /**
     * Called by DataStore right after a save file has been read: re-attaches the clock and repairs the
     * static counters (constructors are not run when objects are read back, so they were never incremented).
     */
    public void afterLoad(Clock newClock) {                                     // [V2-6]
        this.clock = newClock;
        int lastPassenger = 0;
        for (int i = 0; i < peopleCount; i++) {
            if (people[i] instanceof Passenger) {
                lastPassenger = Math.max(lastPassenger, numberPart(((Passenger) people[i]).getPassengerId(), 1));
            }
        }
        int lastPnr = 0;
        for (int i = 0; i < bookingCount; i++) {
            lastPnr = Math.max(lastPnr, numberPart(bookings[i].getPnr(), 3));
        }
        Person.restoreTotalPeople(peopleCount);
        Passenger.restoreCounter(lastPassenger);
        Bus.restoreTotalBuses(busCount);
        Booking.restoreCounter(lastPnr);
    }

    /** "PNR1004" with skip = 3 gives 1004; "P002" with skip = 1 gives 2. Unreadable text counts as 0. */
    private static int numberPart(String id, int skip) {
        try {
            return Integer.parseInt(id.substring(skip));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int getBookingCount() {
        return bookingCount;
    }

    public int getTripCount() {
        return tripCount;
    }

    public Bus[] getBuses() {
        return Arrays.copyOf(buses, busCount);
    }

    public Driver[] getDrivers() {
        Driver[] result = new Driver[peopleCount];
        int found = 0;
        for (int i = 0; i < peopleCount; i++) {
            if (people[i] instanceof Driver) {
                result[found++] = (Driver) people[i];
            }
        }
        return Arrays.copyOf(result, found);
    }

    public boolean hasBus(String number) {
        for (int i = 0; i < busCount; i++) {
            if (buses[i].getBusNumber().equalsIgnoreCase(number.trim())) {
                return true;
            }
        }
        return false;
    }

    // ---------------- clock ----------------

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // ---------------- setup ----------------

    public void addBus(Bus bus) throws BookingException {
        for (int i = 0; i < busCount; i++) {
            if (buses[i].equals(bus)) {                 // [F19] equals() used
                throw new BookingException("Bus " + bus.getBusNumber() + " is already registered");
            }
        }
        if (busCount == buses.length) {
            buses = Arrays.copyOf(buses, buses.length * 2);
        }
        buses[busCount++] = bus;
    }

    public void addPerson(Person person) throws BookingException {
        for (int i = 0; i < peopleCount; i++) {
            if (people[i].equals(person)) {
                throw new BookingException("A " + person.getRole().toLowerCase()
                        + " with phone " + person.getPhone() + " is already registered");
            }
        }
        if (peopleCount == people.length) {
            people = Arrays.copyOf(people, people.length * 2);
        }
        people[peopleCount++] = person;
    }

    public void addCoupon(Coupon coupon) throws BookingException {
        for (int i = 0; i < couponCount; i++) {
            if (coupons[i].equals(coupon)) {
                throw new BookingException("Coupon " + coupon.getCode() + " already exists");
            }
        }
        if (couponCount == coupons.length) {
            coupons = Arrays.copyOf(coupons, coupons.length * 2);
        }
        coupons[couponCount++] = coupon;
    }

    /** Drivers, the fleet, coupons - and a week of trips generated from today. */
    public void loadSampleData() throws BookingException {
        Driver d1 = new Driver("Ramesh Gowda", "9845012345", 42, "KA0120110012345", 15);
        Driver d2 = new Driver("Suresh Kumar", "9845023456", 38, "KA0120120023456", 11);
        Driver d3 = new Driver("Anwar Pasha", "9845034567", 45, "KA0120100034567", 18);
        Driver d4 = new Driver("Mahesh Naik", "9845045678", 36, "KA0120130045678", 9);
        Driver d5 = new Driver("Venkatesh Rao", "9845056789", 50, "KA0120090056789", 22);
        Driver d6 = new Driver("Imran Khan", "9845067890", 40, "KA0120110067890", 13);
        Driver[] drivers = {d1, d2, d3, d4, d5, d6};
        for (Driver d : drivers) {
            addPerson(d);
        }

        // Bus reference holding Ordinary/Ac/Sleeper objects: upcasting
        addBus(new OrdinaryBus("KA01-1101", new Route("Bengaluru", "Mysuru", 145), LocalTime.of(6, 30), d1));
        addBus(new AcBus("KA01-2202", new Route("Bengaluru", "Chennai", 350), LocalTime.of(22, 0), d2));
        addBus(new SleeperBus("KA01-3303", new Route("Bengaluru", "Hyderabad", 570), LocalTime.of(20, 30), d3));
        addBus(new OrdinaryBus("KA01-1104", new Route("Bengaluru", "Mangaluru", 350), LocalTime.of(21, 15), d4));
        addBus(new SleeperBus("KA01-3305", new Route("Bengaluru", "Goa", 560), LocalTime.of(19, 0), d5));
        addBus(new AcBus("KA01-2206", new Route("Bengaluru", "Coimbatore", 365), LocalTime.of(23, 0), d6));

        LocalDate today = today();
        addCoupon(new Coupon("WELCOME10", 0.10, 100.0, today.plusDays(90)));
        addCoupon(new Coupon("FESTIVE20", 0.20, 250.0, today.plusDays(60)));
        addCoupon(new Coupon("SUMMER25", 0.25, 300.0, today.minusDays(30)));   // already expired - shows InvalidCouponException

        generateSchedule(SAMPLE_SCHEDULE_DAYS);
    }

    // ---------------- trips ----------------

    /** Schedule a bus on a date at its usual departure time. */
    public Trip addTrip(Bus bus, LocalDate date) throws BookingException {                  // [V2-1] [F10] overloading
        return addTrip(bus, date, bus.getDepartureTime());
    }

    /** Schedule a bus on a date at a special departure time. */
    public Trip addTrip(Bus bus, LocalDate date, LocalTime time) throws BookingException {
        if (!LocalDateTime.of(date, time).isAfter(now())) {
            throw new BookingException("Cannot schedule a trip in the past ("
                    + DateFmt.dateTime(LocalDateTime.of(date, time)) + ")");
        }
        if (hasTrip(bus, date)) {
            throw new BookingException("Bus " + bus.getBusNumber() + " already has a trip on " + DateFmt.date(date));
        }
        return insertTrip(bus, date, time);
    }

    /**
     * Creates the trips for today and the following days-1 days: one per bus per day, at the bus's usual time.
     * Departures that are already in the past and days that already have a trip are skipped,
     * so running it twice does no harm. Returns how many trips were created.
     */
    public int generateSchedule(int days) throws BookingException {
        if (days < 1 || days > MAX_SCHEDULE_DAYS) {
            throw new BookingException("Number of days must be between 1 and " + MAX_SCHEDULE_DAYS);
        }
        int created = 0;
        LocalDate start = today();
        for (int d = 0; d < days; d++) {
            LocalDate date = start.plusDays(d);
            for (int b = 0; b < busCount; b++) {
                Bus bus = buses[b];
                boolean alreadyScheduled = hasTrip(bus, date);
                boolean alreadyLeft = !LocalDateTime.of(date, bus.getDepartureTime()).isAfter(now());
                if (alreadyScheduled || alreadyLeft) {
                    continue;                           // [F06] continue: nothing to create for this bus/day
                }
                insertTrip(bus, date, bus.getDepartureTime());
                created++;
            }
        }
        return created;
    }

    private Trip insertTrip(Bus bus, LocalDate date, LocalTime time) {
        tripCounter++;
        Trip trip = new Trip(String.format("T%03d", tripCounter), bus, date, time);
        if (tripCount == trips.length) {
            trips = Arrays.copyOf(trips, trips.length * 2);
        }
        trips[tripCount++] = trip;
        return trip;
    }

    private boolean hasTrip(Bus bus, LocalDate date) {
        for (int i = 0; i < tripCount; i++) {
            if (trips[i].getBus().equals(bus) && trips[i].getDate().equals(date)) {
                return true;
            }
        }
        return false;
    }

    // ---------------- looking things up ----------------

    public Bus findBusByNumber(String number) throws BookingException {
        String wanted = number.trim();
        for (int i = 0; i < busCount; i++) {
            if (buses[i].getBusNumber().equalsIgnoreCase(wanted)) {     // [F13] String comparison
                return buses[i];
            }
        }
        throw new BookingException("No bus with number '" + wanted + "'");
    }

    public Trip findTrip(String tripId) throws BookingException {
        String wanted = tripId.trim();
        for (int i = 0; i < tripCount; i++) {
            if (trips[i].getTripId().equalsIgnoreCase(wanted)) {
                return trips[i];
            }
        }
        throw new BookingException("No trip with ID '" + wanted + "'");
    }

    /** Every trip that can still be booked (scheduled and not yet departed), earliest departure first. */
    public Trip[] getUpcomingTrips() {
        return searchRoute("", "", LocalDate.MIN, LocalDate.MAX);
    }

    /** Open trips whose source AND destination contain the given texts (blank = any). */
    public Trip[] searchTrips(String source, String destination) {          // [V2-1] [F10] overloading
        return searchRoute(source, destination, LocalDate.MIN, LocalDate.MAX);
    }

    /** Same, but only for trips leaving on one particular date. */
    public Trip[] searchTrips(String source, String destination, LocalDate date) {   // [F10] overloading
        return searchRoute(source, destination, date, date);
    }

    /** Open trips matching a single keyword in source, destination, bus type or bus number. */
    public Trip[] searchTrips(String keyword) {                             // [F10] overloading
        String key = keyword.trim().toLowerCase();
        LocalDateTime now = now();
        Trip[] result = new Trip[tripCount];
        int found = 0;
        for (int i = 0; i < tripCount; i++) {
            Bus bus = trips[i].getBus();
            Route r = bus.getRoute();
            boolean match = r.getSource().toLowerCase().contains(key)           // [F13] contains()
                    || r.getDestination().toLowerCase().contains(key)
                    || bus.getType().getLabel().toLowerCase().contains(key)
                    || bus.getBusNumber().toLowerCase().contains(key);
            if (match && trips[i].isOpen(now)) {
                result[found++] = trips[i];
            }
        }
        Trip[] open = Arrays.copyOf(result, found);
        Arrays.sort(open, Trip.BY_DEPARTURE);
        return open;
    }

    /** Shared by the overloads above: route text + a first/last day (inclusive). */
    private Trip[] searchRoute(String source, String destination, LocalDate firstDay, LocalDate lastDay) {
        String src = source.trim().toLowerCase();
        String dst = destination.trim().toLowerCase();
        LocalDateTime now = now();
        Trip[] result = new Trip[tripCount];
        int found = 0;
        for (int i = 0; i < tripCount; i++) {
            Route r = trips[i].getBus().getRoute();
            LocalDate day = trips[i].getDate();
            boolean routeMatches = r.getSource().toLowerCase().contains(src)
                    && r.getDestination().toLowerCase().contains(dst);
            boolean dayMatches = !day.isBefore(firstDay) && !day.isAfter(lastDay);
            if (routeMatches && dayMatches && trips[i].isOpen(now)) {
                result[found++] = trips[i];
            }
        }
        Trip[] open = Arrays.copyOf(result, found);
        Arrays.sort(open, Trip.BY_DEPARTURE);                                   // default order: earliest first
        return open;
    }

    /** Re-orders a list of trips, e.g. {@code sortTrips(list, Trip.BY_FARE)}. */
    public void sortTrips(Trip[] list, Comparator<Trip> order) {                // [V2-1]
        Arrays.sort(list, order);
    }

    public void printTrips(Trip[] list) {
        System.out.println();
        System.out.printf("%-6s %-10s %-11s %-34s %-21s %-10s %s%n",
                "Trip", "Bus No", "Type", "Route", "Departure", "Free/Total", "Fare/seat");
        InputHelper.printLine('-', 112);
        for (Trip trip : list) {
            System.out.println(trip);                   // calls Trip.toString(); getFare() is resolved at run time
        }
    }

    // ---------------- people ----------------

    public boolean isRegistered(String phone) {
        for (int i = 0; i < peopleCount; i++) {
            if (people[i] instanceof Passenger && people[i].getPhone().equals(phone)) {
                return true;
            }
        }
        return false;
    }

    public Passenger findPassenger(String phone) throws BookingException {
        for (int i = 0; i < peopleCount; i++) {
            if (people[i] instanceof Passenger && people[i].getPhone().equals(phone)) {
                return (Passenger) people[i];           // downcast Person -> Passenger
            }
        }
        throw new BookingException("No passenger registered with phone " + phone);
    }

    /** Lists drivers and passengers through Person references: each object answers with its own describe(). */
    public void printPeople() {
        System.out.println();
        for (int i = 0; i < peopleCount; i++) {
            Person p = people[i];                       // [F16] parent reference holding a child object
            System.out.println(p.describe());           // [F16] dynamic binding: Driver's or Passenger's version runs
        }
        InputHelper.printLine('-', 80);
        System.out.println("People registered: " + Person.getTotalPeople());
    }

    // ---------------- coupons ----------------

    /** Looks the code up (any letter case) and checks it has not expired. */
    public Coupon findValidCoupon(String code) throws InvalidCouponException {
        String wanted = code.trim();
        for (int i = 0; i < couponCount; i++) {
            if (coupons[i].getCode().equalsIgnoreCase(wanted)) {
                coupons[i].validateOn(today());         // throws InvalidCouponException if expired
                return coupons[i];
            }
        }
        throw new InvalidCouponException(wanted.toUpperCase(), "is not a valid coupon code");
    }

    // ---------------- reservations ----------------

    /** Reserve the given seat numbers, no coupon. */
    public Booking reserve(Trip trip, Passenger passenger, int[] seats) throws BookingException {   // [F10] overloading
        return reserve(trip, passenger, seats, Coupon.NONE);
    }

    /** Reserve any 'count' free seats automatically, no coupon. */
    public Booking reserve(Trip trip, Passenger passenger, int count) throws BookingException {     // [F10] overloading
        return reserve(trip, passenger, count, Coupon.NONE);
    }

    /**
     * Reserve the given seat numbers. Either ALL seats are reserved or none:
     * if any seat is invalid or taken, the seats already grabbed are released and the exception is passed on.
     */
    public Booking reserve(Trip trip, Passenger passenger, int[] seats, Coupon coupon) throws BookingException {   // [F10]
        trip.ensureOpen(now());                         // TripClosedException if the bus has left / trip cancelled
        coupon.validateOn(today());                     // InvalidCouponException if the coupon has expired
        checkSeatCount(seats.length);
        for (int i = 0; i < seats.length; i++) {
            try {
                trip.bookSeat(seats[i]);
            } catch (SeatUnavailableException e) {      // [V2-2] catch
                for (int j = 0; j < i; j++) {           // undo the seats already taken
                    trip.releaseSeat(seats[j]);
                }
                throw e;                                // [V2-2] re-throw: the caller decides what to tell the user
            }
        }
        return buildBooking(trip, passenger, seats, coupon);
    }

    /** Reserve any 'count' free seats automatically. */
    public Booking reserve(Trip trip, Passenger passenger, int count, Coupon coupon) throws BookingException {    // [F10]
        trip.ensureOpen(now());
        coupon.validateOn(today());
        checkSeatCount(count);
        if (count > trip.getAvailableSeats()) {
            throw new SeatUnavailableException("Only " + trip.getAvailableSeats()
                    + " seat(s) left on trip " + trip.getTripId());
        }
        int[] seats = new int[count];
        for (int i = 0; i < count; i++) {
            seats[i] = trip.bookSeat();                 // auto-assign overload
        }
        return buildBooking(trip, passenger, seats, coupon);
    }

    private void checkSeatCount(int count) throws BookingException {
        if (count < 1 || count > MAX_SEATS_PER_BOOKING) {
            throw new BookingException("You can book 1 to " + MAX_SEATS_PER_BOOKING + " seats at a time");
        }
    }

    private Booking buildBooking(Trip trip, Passenger passenger, int[] seats, Coupon coupon) {
        Discountable concession = passenger;            // [F18] interface reference to a Passenger object
        Discountable promo = coupon;                    // [F18] interface reference to a Coupon object
        FareBreakdown fare = FareCalculator.breakdown(trip.getFare(), seats.length, concession, promo);
        return new Booking(passenger, trip, seats, fare, coupon.getCode(), now());
    }

    /** Give the seats of an unpaid booking back to the trip. */
    public void rollback(Booking booking) {
        for (int seat : booking.getSeatNumbers()) {
            booking.getTrip().releaseSeat(seat);
        }
    }

    public void saveBooking(Booking booking) {
        if (bookingCount == bookings.length) {
            bookings = Arrays.copyOf(bookings, bookings.length * 2);
        }
        bookings[bookingCount++] = booking;
    }

    public Booking findBooking(String pnr) throws BookingException {
        for (int i = 0; i < bookingCount; i++) {
            if (bookings[i].getPnr().equalsIgnoreCase(pnr.trim())) {
                return bookings[i];
            }
        }
        throw new BookingException("No booking found for PNR '" + pnr.trim() + "'");
    }

    public Booking[] findBookingsByPhone(String phone) {
        Booking[] result = new Booking[bookingCount];
        int found = 0;
        for (int i = 0; i < bookingCount; i++) {
            if (bookings[i].getPassenger().getPhone().equals(phone)) {
                result[found++] = bookings[i];
            }
        }
        return Arrays.copyOf(result, found);
    }

    // ---------------- cancellation and refunds ----------------

    /**
     * Which refund window a cancellation made right now falls into.
     * Throws if the ticket is already cancelled or the bus has already left.
     */
    public RefundTier refundTierFor(Booking booking) throws BookingException {          // [V2-3]
        if (booking.getStatus() == BookingStatus.TRIP_CANCELLED) {
            throw new BookingException("Ticket " + booking.getPnr() + " needs no cancelling: the operator cancelled the trip"
                    + String.format(" and you were refunded Rs.%.2f in full", booking.getRefundAmount()));
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingException("Ticket " + booking.getPnr() + " cannot be cancelled: it is already "
                    + booking.getStatus());
        }
        booking.getTrip().ensureOpen(now());            // TripClosedException: "can't cancel a bus that has left"
        return RefundTier.forMinutesLeft(booking.getTrip().minutesToDeparture(now()));
    }

    /** Cancels a booking, frees its seats and returns the refund in rupees. */
    public double cancelBooking(Booking booking) throws BookingException {
        RefundTier tier = refundTierFor(booking);       // all the checks happen here, before anything is changed
        double refund = booking.isPaid() ? FareCalculator.refundAmount(booking.getPayableAmount(), tier) : 0.0;
        rollback(booking);
        booking.markCancelled(refund);
        return refund;
    }

    // ---------------- admin operations ----------------

    /** Confirmed bookings on one trip, in booking order. */
    public Booking[] confirmedBookingsOn(Trip trip) {
        Booking[] result = new Booking[bookingCount];
        int found = 0;
        for (int i = 0; i < bookingCount; i++) {
            if (bookings[i].getTrip().equals(trip) && bookings[i].getStatus() == BookingStatus.CONFIRMED) {
                result[found++] = bookings[i];
            }
        }
        return Arrays.copyOf(result, found);
    }

    /**
     * The operator cancels a whole trip: the trip is closed and EVERY confirmed booking on it is refunded in full.
     * Returns the bookings that were refunded. Throws TripClosedException (and changes nothing) if the bus
     * has already left or the trip was already cancelled.
     */
    public Booking[] cancelTrip(Trip trip) throws BookingException {           // [V2-5]
        trip.ensureOpen(now());
        Booking[] affected = confirmedBookingsOn(trip);
        trip.cancel();
        for (Booking booking : affected) {
            double fullRefund = booking.isPaid() ? booking.getPayableAmount() : 0.0;
            booking.markTripCancelled(fullRefund);
        }
        return affected;
    }

    /** Wipes EVERYTHING (buses, trips, bookings, people, coupons, counters) and loads the sample data again. */
    public void resetToSampleData() throws BookingException {                  // [V2-5]
        buses = new Bus[10];
        busCount = 0;
        trips = new Trip[50];
        tripCount = 0;
        tripCounter = 0;
        bookings = new Booking[10];
        bookingCount = 0;
        people = new Person[10];
        peopleCount = 0;
        coupons = new Coupon[5];
        couponCount = 0;
        Person.restoreTotalPeople(0);
        Passenger.restoreCounter(0);
        Bus.restoreTotalBuses(0);
        Booking.restoreCounter(0);
        loadSampleData();
    }

    private int indexOfBus(Bus bus) {
        for (int i = 0; i < busCount; i++) {
            if (buses[i].equals(bus)) {
                return i;
            }
        }
        return -1;
    }

    // ---------------- reports ----------------

    public void printAllBookings() {
        System.out.println();
        if (bookingCount == 0) {
            System.out.println("No bookings yet.");
            return;
        }
        for (int i = 0; i < bookingCount; i++) {
            System.out.println(bookings[i]);
        }
    }

    public void printStatistics() {
        int confirmed = 0;
        int cancelled = 0;
        int tripCancelled = 0;
        double revenue = 0.0;
        double refunded = 0.0;
        double feesKept = 0.0;

        for (int i = 0; i < bookingCount; i++) {
            Booking b = bookings[i];
            if (b.getStatus() == BookingStatus.CONFIRMED) {
                confirmed++;
                revenue += b.getPayableAmount();
                continue;                               // [F06] continue: the refund totals below are only for cancelled tickets
            }
            if (b.getStatus() == BookingStatus.CANCELLED) {
                cancelled++;
            } else {
                tripCancelled++;
            }
            refunded += b.getRefundAmount();
            feesKept += b.getPayableAmount() - b.getRefundAmount();
        }

        LocalDateTime now = now();
        int openTrips = 0;
        int totalSeats = 0;
        int bookedSeats = 0;
        for (Trip trip : trips) {
            if (trip == null) {
                break;                                  // [F06] break: the rest of the array is unused
            }
            if (trip.isOpen(now)) {
                openTrips++;
                totalSeats += trip.getTotalSeats();
                bookedSeats += trip.getBookedSeats();
            }
        }
        double occupancy = totalSeats == 0 ? 0.0 : (double) bookedSeats / totalSeats * 100;   // [F04] cast

        System.out.println();
        InputHelper.printLine('=', 50);
        System.out.println("            SYSTEM STATISTICS");
        InputHelper.printLine('=', 50);
        System.out.printf("Buses in fleet         : %d%n", busCount);
        System.out.printf("Trips open for booking : %d (of %d scheduled)%n", openTrips, tripCount);
        System.out.printf("Bookings confirmed     : %d%n", confirmed);
        System.out.printf("Cancelled by passenger : %d%n", cancelled);
        System.out.printf("Cancelled with trip    : %d%n", tripCancelled);
        System.out.printf("Ticket revenue         : Rs.%.2f%n", revenue);
        System.out.printf("Refunds paid out       : Rs.%.2f%n", refunded);
        System.out.printf("Cancellation fees kept : Rs.%.2f%n", feesKept);
        System.out.printf("Seats booked / total   : %d / %d (open trips)%n", bookedSeats, totalSeats);
        System.out.printf("Overall occupancy      : %.1f%%%n", occupancy);
        System.out.printf("Objects created        : %d buses, %d people, %d booking attempts%n",
                Bus.getTotalBuses(), Person.getTotalPeople(), Booking.getTotalBookingsCreated());   // static methods
        InputHelper.printLine('=', 50);
    }

    /** Money report: one row per bus, then totals split into sales, refunds and discounts given. */
    public void printRevenueReport() {                                          // [V2-5]
        LocalDateTime now = now();
        int[] tripsPerBus = new int[busCount];
        int[] bookingsPerBus = new int[busCount];       // confirmed bookings only
        int[] seatsPerBus = new int[busCount];          // seats in confirmed bookings
        double[] grossPerBus = new double[busCount];
        double[] refundsPerBus = new double[busCount];

        int cancelledTrips = 0;
        int departedTrips = 0;
        for (int i = 0; i < tripCount; i++) {
            int bus = indexOfBus(trips[i].getBus());
            if (bus >= 0) {
                tripsPerBus[bus]++;
            }
            if (trips[i].getStatus() == TripStatus.CANCELLED) {
                cancelledTrips++;
            } else if (trips[i].hasDeparted(now)) {
                departedTrips++;
            }
        }

        int confirmed = 0;
        int byPassenger = 0;
        int withTrip = 0;
        int seatsSold = 0;
        double gross = 0.0;
        double passengerRefunds = 0.0;
        double tripRefunds = 0.0;
        double concessions = 0.0;
        double couponSavings = 0.0;
        for (int i = 0; i < bookingCount; i++) {
            Booking b = bookings[i];
            int bus = indexOfBus(b.getTrip().getBus());
            gross += b.getPayableAmount();
            if (bus >= 0) {
                grossPerBus[bus] += b.getPayableAmount();
                refundsPerBus[bus] += b.getRefundAmount();
            }
            switch (b.getStatus()) {
                case CONFIRMED:
                    confirmed++;
                    seatsSold += b.getFare().getSeats();
                    concessions += b.getFare().getConcessionAmount();
                    couponSavings += b.getFare().getCouponAmount();
                    if (bus >= 0) {
                        bookingsPerBus[bus]++;
                        seatsPerBus[bus] += b.getFare().getSeats();
                    }
                    break;
                case CANCELLED:
                    byPassenger++;
                    passengerRefunds += b.getRefundAmount();
                    break;
                default:                                // TRIP_CANCELLED
                    withTrip++;
                    tripRefunds += b.getRefundAmount();
            }
        }
        double refunds = passengerRefunds + tripRefunds;

        System.out.println();
        InputHelper.printLine('=', 106);
        System.out.println("                                          REVENUE REPORT");
        InputHelper.printLine('=', 106);
        System.out.println("As of " + DateFmt.dateTime(now));
        System.out.println();
        System.out.printf("%-10s %-34s %5s %8s %5s %12s %13s %12s%n",
                "Bus No", "Route", "Trips", "Bookings", "Seats", "Gross (Rs.)", "Refunds (Rs.)", "Net (Rs.)");
        InputHelper.printLine('-', 106);
        for (int i = 0; i < busCount; i++) {
            System.out.printf("%-10s %-34s %5d %8d %5d %12.2f %13.2f %12.2f%n",
                    buses[i].getBusNumber(), buses[i].getRoute(), tripsPerBus[i], bookingsPerBus[i], seatsPerBus[i],
                    grossPerBus[i], refundsPerBus[i], grossPerBus[i] - refundsPerBus[i]);
        }
        InputHelper.printLine('-', 106);
        System.out.printf("%-10s %-34s %5d %8d %5d %12.2f %13.2f %12.2f%n",
                "TOTAL", "", tripCount, confirmed, seatsSold, gross, refunds, gross - refunds);
        System.out.println();
        System.out.printf("%-26s: %d  (%d cancelled by operator, %d already departed)%n",
                "Trips scheduled", tripCount, cancelledTrips, departedTrips);
        System.out.printf("%-26s: %d confirmed, %d cancelled by passenger, %d cancelled with the trip%n",
                "Bookings", confirmed, byPassenger, withTrip);
        System.out.printf("%-26s: Rs.%.2f%n", "Gross ticket sales", gross);
        System.out.printf("%-26s: Rs.%.2f  (passengers Rs.%.2f, cancelled trips Rs.%.2f)%n",
                "Refunds paid", refunds, passengerRefunds, tripRefunds);
        System.out.printf("%-26s: Rs.%.2f%n", "NET REVENUE", gross - refunds);
        System.out.printf("%-26s: concessions Rs.%.2f, coupons Rs.%.2f%n",
                "Discounts on live tickets", concessions, couponSavings);
        InputHelper.printLine('=', 106);
    }
}
