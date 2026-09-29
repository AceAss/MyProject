package com.busbooking.app; // [F21] custom package

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;

import com.busbooking.exception.BookingException;
import com.busbooking.exception.InvalidCouponException;
import com.busbooking.exception.PaymentFailedException;
import com.busbooking.exception.SeatUnavailableException;
import com.busbooking.model.Booking;
import com.busbooking.model.Bus;
import com.busbooking.model.Coupon;
import com.busbooking.model.Discountable;
import com.busbooking.model.FareBreakdown;
import com.busbooking.model.Passenger;
import com.busbooking.model.Payable;
import com.busbooking.model.RefundTier;
import com.busbooking.model.Trip;
import com.busbooking.service.BookingService; // [F21] import from another custom package
import com.busbooking.service.DataStore;
import com.busbooking.service.FareCalculator;
import com.busbooking.util.DateFmt;
import com.busbooking.util.InputHelper;

/**
 * Bus Ticket Booking System - console entry point.
 *
 * Normal run:   java -cp out com.busbooking.app.Main
 * Options (all optional):
 *   --now 2026-10-05T19:30   freeze "now" so every refund window can be shown without waiting for real time
 *   --data some/folder/x.dat use another save file (its tickets go in some/folder/tickets/)
 *   --echo                   print each line typed - makes transcripts from redirected input readable
 */
public class Main {

    private static BookingService service;
    private static DataStore store;
    private static Clock clock = Clock.systemDefaultZone();
    private static Path dataFile = Paths.get("data", "busbooking.dat");
    private static final int MAX_PAYMENT_ATTEMPTS = 3;

    public static void main(String[] args) {
        parseArgs(args);
        store = new DataStore(dataFile);
        try {
            service = openService();
        } catch (BookingException e) {                  // [V2-2] try / catch
            System.out.println("Could not start: " + e.getMessage());
            return;
        }
        printBanner();

        int choice;
        do {                                            // [F06] do-while: menu shows at least once
            printMenu();
            choice = InputHelper.readInt("Enter your choice: ", 0, 7);
            try {
                switch (choice) {                       // [F06] switch
                    case 1:
                        viewTrips();
                        break;                          // [F06] break
                    case 2:
                        searchTrips();
                        break;
                    case 3:
                        viewSeatMap();
                        break;
                    case 4:
                        bookTicket();
                        break;
                    case 5:
                        cancelTicket();
                        break;
                    case 6:
                        myBookings();
                        break;
                    case 7:
                        new AdminMenu(service, store).run();
                        break;
                    case 0:
                        store.saveOrWarn(service);
                        System.out.println("Data saved. Thank you for travelling with us. Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (BookingException e) {              // [V2-2] one safety net: any rule violation is reported, not crashed on
                System.out.println("Error: " + e.getMessage());
            }
        } while (choice != 0);

        InputHelper.close();
    }

    /** Reads --now, --data and --echo from the command line (all optional). */
    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String flag = args[i];
            boolean hasValue = i + 1 < args.length;
            if (flag.equals("--echo")) {
                InputHelper.setEcho(true);
            } else if (flag.equals("--now") && hasValue) {
                i++;
                try {
                    LocalDateTime pretend = LocalDateTime.parse(args[i]);
                    ZoneId zone = ZoneId.systemDefault();
                    clock = Clock.fixed(pretend.atZone(zone).toInstant(), zone);
                    System.out.println("[Demo mode] clock frozen at " + DateFmt.dateTime(pretend));
                } catch (DateTimeParseException e) {
                    System.out.println("Ignoring --now (use e.g. 2026-10-05T19:30). Using the real clock.");
                }
            } else if (flag.equals("--data") && hasValue) {
                i++;
                dataFile = Paths.get(args[i]);
            } else {
                System.out.println("Ignoring unknown option '" + flag
                        + "'  (options: --now yyyy-MM-ddTHH:mm | --data file | --echo)");
            }
        }
    }

    /** Loads the save file if there is one; otherwise (or if it is unreadable) starts from the sample data. */
    private static BookingService openService() throws BookingException {
        if (store.exists()) {
            try {
                BookingService loaded = store.load(clock);
                System.out.println("Saved data loaded from " + store.getDataFile() + " ("
                        + loaded.getBookingCount() + " booking(s), " + loaded.getTripCount() + " trip(s)).");
                return loaded;
            } catch (IOException e) {                   // [V2-6] a damaged file must not stop the program from starting
                String reason = (e.getMessage() == null) ? e.toString() : e.getMessage();
                System.out.println("Could not read " + store.getDataFile() + ": " + reason);
                try {
                    System.out.println("The unreadable file was kept as " + store.moveAside() + ".");
                } catch (IOException moveError) {
                    System.out.println("(it could not be renamed either: " + moveError.getMessage() + ")");
                }
            }
        }
        BookingService fresh = new BookingService(clock);
        fresh.loadSampleData();
        store.saveOrWarn(fresh);
        System.out.println("Sample data loaded and saved to " + store.getDataFile() + ".");
        return fresh;
    }

    private static void printBanner() {
        InputHelper.printLine('=', 60);
        System.out.println("          BUS TICKET BOOKING SYSTEM  (V2)");
        System.out.println("     REVA University | B.Sc. (BSTCs) | Java OOP");
        InputHelper.printLine('=', 60);
        System.out.println("Today is " + DateFmt.dateTime(service.now()));
        if (service.getUpcomingTrips().length == 0) {
            System.out.println("No upcoming trips - the saved schedule has run out. An admin can create more (Admin mode, option 4).");
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("---------------- MAIN MENU ----------------");
        System.out.println(" 1. View upcoming trips");
        System.out.println(" 2. Search trips (route, date or keyword)");
        System.out.println(" 3. View seat map of a trip");
        System.out.println(" 4. Book tickets");
        System.out.println(" 5. Cancel a ticket");
        System.out.println(" 6. My bookings (by phone number)");
        System.out.println(" 7. Admin mode (password)");
        System.out.println(" 0. Exit");
        System.out.println("-------------------------------------------");
    }

    // ---------------- browsing ----------------

    /** Asks how to order a list of trips; returns the matching comparator. */
    private static Comparator<Trip> askSortOrder() {
        int pick = InputHelper.readInt("Sort by  1) departure time  2) fare, cheapest first: ", 1, 2);
        return pick == 2 ? Trip.BY_FARE : Trip.BY_DEPARTURE;
    }

    private static void showTrips(Trip[] results) {
        if (results.length == 0) {
            System.out.println("No upcoming trips found.");
            return;
        }
        service.sortTrips(results, askSortOrder());
        System.out.println();
        System.out.println(results.length + " trip(s) found:");
        service.printTrips(results);
    }

    private static void viewTrips() {
        showTrips(service.getUpcomingTrips());
    }

    private static void searchTrips() throws BookingException {
        int mode = InputHelper.readInt("Search by  1) route and/or date  2) keyword (bus type, bus no., city): ", 1, 2);
        Trip[] results;
        if (mode == 2) {
            String keyword = InputHelper.readLine("Keyword: ");
            results = service.searchTrips(keyword);                     // one-argument overload
        } else {
            String from = InputHelper.readLine("From (city; blank = any): ");
            String to = InputHelper.readLine("To   (city; blank = any): ");
            String dateText = InputHelper.readLine("Date (dd-MM-yyyy; blank = any date): ");
            if (dateText.isEmpty()) {
                results = service.searchTrips(from, to);                // two-argument overload
            } else {
                results = service.searchTrips(from, to, DateFmt.parseDate(dateText));   // three-argument overload
            }
        }
        showTrips(results);
    }

    private static void viewSeatMap() throws BookingException {
        Trip trip = service.findTrip(InputHelper.readLine("Enter trip ID (e.g. T001): "));
        Bus bus = trip.getBus();
        System.out.println();
        service.printTrips(new Trip[] {trip});
        System.out.println("Amenities : " + bus.getAmenities());       // [F16] runs the subclass version
        System.out.println("Driver    : " + bus.getDriver().getName());
        System.out.printf("Occupancy : %.1f%%%n", trip.getOccupancyPercent());
        System.out.println();
        trip.printSeatMap();
    }

    // ---------------- booking ----------------

    private static void bookTicket() throws BookingException {
        String dateText = InputHelper.readLine("Travel date (dd-MM-yyyy; blank = all upcoming trips): ");
        Trip[] options = dateText.isEmpty()
                ? service.getUpcomingTrips()
                : service.searchTrips("", "", DateFmt.parseDate(dateText));
        if (options.length == 0) {
            System.out.println("No trips available.");
            return;
        }
        service.printTrips(options);
        System.out.println();

        Trip trip = service.findTrip(InputHelper.readLine("Enter trip ID to book: "));
        trip.ensureOpen(service.now());                 // TripClosedException if it has left / was cancelled
        if (trip.getAvailableSeats() == 0) {
            throw new SeatUnavailableException("Sorry, this trip is full.");
        }
        System.out.println();
        trip.printSeatMap();
        System.out.println();

        // ---- passenger details ----
        String phone = InputHelper.readPhone("Passenger phone (10 digits): ");
        Passenger passenger;
        if (service.isRegistered(phone)) {
            passenger = service.findPassenger(phone);
            System.out.println("Welcome back, " + passenger.getName() + "!");
        } else {
            String name = InputHelper.capitalizeWords(InputHelper.readNonEmpty("Passenger name: "));
            int age = InputHelper.readInt("Age: ", 1, 120);
            boolean student = false;
            if (age < 60) {
                student = InputHelper.readYesNo("Student with a valid ID (10% concession)? (y/n): ");
            }
            passenger = new Passenger(name, phone, age, student);
            service.addPerson(passenger);
        }

        // ---- seats and coupon (asked for first, then reserved together so the fare can be worked out) ----
        String seatInput = InputHelper.readLine("Seat numbers (e.g. 3,4) or AUTO: ");
        boolean auto = seatInput.equalsIgnoreCase("auto");
        int count = auto ? InputHelper.readInt("How many seats? ", 1, BookingService.MAX_SEATS_PER_BOOKING) : 0;
        int[] chosen = auto ? new int[0] : InputHelper.parseSeatList(seatInput);
        if (!auto && chosen.length == 0) {
            throw new BookingException("Seat numbers must be whole numbers separated by commas, e.g. 3,4");
        }
        Coupon coupon = askCoupon();

        Booking booking = auto
                ? service.reserve(trip, passenger, count, coupon)
                : service.reserve(trip, passenger, chosen, coupon);

        // ---- from here the seats are HELD. 'finally' guarantees they are released unless the booking is confirmed ----
        boolean confirmed = false;
        try {
            printFareSummary(booking);
            collectPayment(booking);
            service.saveBooking(booking);
            confirmed = true;
        } catch (PaymentFailedException e) {            // [V2-2] catch a specific subclass
            System.out.println("Payment failed: " + e.getMessage());
        } finally {                                     // [V2-2] finally: runs on success, on failure AND on any unexpected error
            if (!confirmed) {
                service.rollback(booking);
                System.out.println("Seats released - booking not made.");
            }
        }

        if (confirmed) {
            System.out.println();
            System.out.println("Payment received. Booking confirmed!");
            System.out.print(booking.getTicketText());
            if (store.exportTicketOrWarn(booking)) {
                System.out.println("Ticket file: " + store.getTicketDir().resolve(booking.getPnr() + ".txt"));
            }
            store.saveOrWarn(service);
        }
    }

    /** Keeps asking until the coupon is valid or the person presses Enter for "no coupon". */
    private static Coupon askCoupon() {
        while (true) {
            String code = InputHelper.readLine("Coupon code (blank = none): ");
            if (code.isEmpty()) {
                return Coupon.NONE;
            }
            try {
                Coupon coupon = service.findValidCoupon(code);
                System.out.println("Coupon applied: " + coupon);
                return coupon;
            } catch (InvalidCouponException e) {        // [V2-2] [V2-4]
                System.out.println(e.getMessage() + ". Try again, or press Enter to continue without a coupon.");
            }
        }
    }

    /** One aligned "label : value" line of the fare summary. */
    private static void summaryLine(String label, String value) {
        System.out.printf("%-21s: %s%n", label, value);                  // [F08] printf with a width
    }

    private static void printFareSummary(Booking booking) {
        FareBreakdown fare = booking.getFare();
        Discountable concession = booking.getPassenger();               // [F18] accessed through the interface
        System.out.println();
        summaryLine("Seats reserved", booking.getSeatsAsString());
        summaryLine("Fare", String.format("Rs.%.2f  (%d x Rs.%.2f)", fare.getBaseFare(), fare.getSeats(), fare.getFarePerSeat()));
        if (concession.getDiscountRate() > 0) {
            summaryLine(String.format("Concession (%.0f%%)", concession.getDiscountRate() * 100),
                    String.format("-Rs.%.2f", fare.getConcessionAmount()));
        }
        if (fare.getCouponAmount() > 0) {
            summaryLine("Coupon " + booking.getCouponCode(), String.format("-Rs.%.2f", fare.getCouponAmount()));
        }
        summaryLine(String.format("GST %.0f%%", FareCalculator.GST_RATE * 100), String.format("+Rs.%.2f", fare.getGst()));
        if (Math.abs(fare.getRoundOff()) >= 0.005) {
            summaryLine("Round off", String.format("%+.2f", fare.getRoundOff()));
        }
        summaryLine("TOTAL TO PAY", String.format("Rs.%.2f", fare.getTotal()));
    }

    /** Up to 3 tries. Throws PaymentFailedException if the money never covers the amount due. */
    private static void collectPayment(Booking booking) throws PaymentFailedException {
        Payable payable = booking;                      // [F18] interface reference -> Booking object
        int attempts = 0;
        while (attempts < MAX_PAYMENT_ATTEMPTS) {
            double tendered = InputHelper.readDouble("Amount to pay (Rs.): ");
            try {
                payable.pay(tendered);
                double change = tendered - payable.getPayableAmount();
                if (change > 0) {
                    summaryLine("Change returned", String.format("Rs.%.2f", change));
                }
                return;                                 // paid - leave the method
            } catch (PaymentFailedException e) {
                attempts++;
                System.out.printf("%s. Attempts left: %d%n", e.getMessage(), MAX_PAYMENT_ATTEMPTS - attempts);
            }
        }
        throw new PaymentFailedException("Payment not received after " + MAX_PAYMENT_ATTEMPTS + " attempts");   // [V2-2] throw
    }

    // ---------------- cancelling and looking up ----------------

    private static void cancelTicket() throws BookingException {
        Booking booking = service.findBooking(InputHelper.readLine("Enter PNR to cancel: "));
        RefundTier tier = service.refundTierFor(booking);               // throws if already cancelled or the bus has left
        double refund = FareCalculator.refundAmount(booking.getPayableAmount(), tier);

        System.out.println(booking);
        System.out.printf("Bus departs in %s (%s).%n",
                booking.getTrip().timeToDeparture(service.now()), tier.getLabel());
        System.out.printf("Refund if cancelled now: %.0f%% = Rs.%.2f%n", tier.getRefundRate() * 100, refund);
        if (!InputHelper.readYesNo("Cancel this ticket? (y/n): ")) {
            System.out.println("Cancellation aborted.");
            return;
        }
        double actual = service.cancelBooking(booking);
        System.out.printf("Ticket %s cancelled. Refund: Rs.%.2f%n", booking.getPnr(), actual);
        store.exportTicketOrWarn(booking);              // the ticket file now says CANCELLED and shows the refund
        store.saveOrWarn(service);
    }

    private static void myBookings() {
        String phone = InputHelper.readPhone("Phone number used for booking: ");
        Booking[] mine = service.findBookingsByPhone(phone);
        if (mine.length == 0) {
            System.out.println("No bookings found for this number.");
            return;
        }
        System.out.println();
        for (Booking b : mine) {
            System.out.println(b);
        }
    }
}
