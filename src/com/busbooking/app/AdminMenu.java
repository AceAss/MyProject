package com.busbooking.app;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

import com.busbooking.exception.BookingException;
import com.busbooking.model.AcBus;
import com.busbooking.model.Booking;
import com.busbooking.model.Bus;
import com.busbooking.model.Driver;
import com.busbooking.model.OrdinaryBus;
import com.busbooking.model.Route;
import com.busbooking.model.SleeperBus;
import com.busbooking.model.Trip;
import com.busbooking.service.BookingService;
import com.busbooking.service.DataStore;
import com.busbooking.util.DateFmt;
import com.busbooking.util.InputHelper;

/**
 * Operator menu, protected by a password. Every change is saved to disk straight away.
 * (The password is a constant to keep the project simple; a real system would never store it in the source.)
 */
public class AdminMenu {                                // [V2-5] [F01] encapsulated

    private static final String ADMIN_PASSWORD = "admin123";    // [F02] final constant
    private static final int MAX_LOGIN_ATTEMPTS = 3;

    private final BookingService service;
    private final DataStore store;

    public AdminMenu(BookingService service, DataStore store) {
        this.service = service;                         // [F12] this resolves shadowing
        this.store = store;
    }

    /** Asks for the password, then shows the admin menu until the operator goes back. */
    public void run() {
        if (!login()) {
            return;
        }
        int choice;
        do {
            printMenu();
            choice = InputHelper.readInt("Admin choice: ", 0, 8);
            try {
                switch (choice) {                       // [F06] switch
                    case 1:
                        addBus();
                        break;
                    case 2:
                        addTrip();
                        break;
                    case 3:
                        cancelTrip();
                        break;
                    case 4:
                        generateSchedule();
                        break;
                    case 5:
                        service.printRevenueReport();
                        break;
                    case 6:
                        resetData();
                        break;
                    case 7:
                        service.printPeople();
                        break;
                    case 8:
                        service.printAllBookings();
                        service.printStatistics();
                        break;
                    default:
                        break;                          // 0 = leave the menu
                }
            } catch (BookingException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } while (choice != 0);
        System.out.println("Leaving admin mode.");
    }

    private boolean login() {
        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            String typed = InputHelper.readLine("Admin password: ");
            if (typed.equals(ADMIN_PASSWORD)) {         // [F13] String equals
                System.out.println("Access granted.");
                return true;
            }
            System.out.println("Wrong password. Attempts left: " + (MAX_LOGIN_ATTEMPTS - attempt));
        }
        System.out.println("Too many wrong attempts - back to the main menu.");
        return false;
    }

    private void printMenu() {
        System.out.println();
        System.out.println("---------------- ADMIN MENU ---------------");
        System.out.println(" 1. Add a bus");
        System.out.println(" 2. Add a trip");
        System.out.println(" 3. Cancel a trip (refunds every passenger in full)");
        System.out.println(" 4. Generate schedule for the next N days");
        System.out.println(" 5. Revenue report");
        System.out.println(" 6. Reset to sample data");
        System.out.println(" 7. Drivers and passengers");
        System.out.println(" 8. All bookings and statistics");
        System.out.println(" 0. Back to main menu");
        System.out.println("-------------------------------------------");
    }

    // ---------------- 1. add a bus ----------------

    private void addBus() throws BookingException {
        int kind = InputHelper.readInt("Bus type  1) Ordinary  2) AC Seater  3) AC Sleeper: ", 1, 3);
        String number = InputHelper.readNonEmpty("Bus number (format KA01-4407): ").toUpperCase();
        if (!number.matches("[A-Z]{2}\\d{2}-\\d{4}")) {             // [F13] String method: matches (regular expression)
            throw new BookingException("Bus number must look like KA01-4407");
        }
        if (service.hasBus(number)) {
            throw new BookingException("Bus " + number + " is already registered");
        }
        String from = InputHelper.capitalizeWords(InputHelper.readNonEmpty("From city: "));
        String to = InputHelper.capitalizeWords(InputHelper.readNonEmpty("To city: "));
        int km = InputHelper.readInt("Distance in km (1-3000): ", 1, 3000);
        LocalTime departure = askTime("Usual daily departure time (HH:mm): ");
        Driver driver = chooseDriver();

        Route route = new Route(from, to, km);
        Bus bus;                                        // parent-type reference; the object is one of the three subclasses
        switch (kind) {
            case 1:
                bus = new OrdinaryBus(number, route, departure, driver);
                break;
            case 2:
                bus = new AcBus(number, route, departure, driver);
                break;
            default:
                bus = new SleeperBus(number, route, departure, driver);
        }
        service.addBus(bus);
        store.saveOrWarn(service);
        System.out.println("Bus added:");
        System.out.println("  " + bus);
        System.out.println("It has no trips yet - use 'Add a trip' or 'Generate schedule' to put it on the road.");
    }

    private LocalTime askTime(String prompt) {
        while (true) {
            String text = InputHelper.readNonEmpty(prompt);
            try {
                return DateFmt.parseTime(text);
            } catch (BookingException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /** Pick one of the registered drivers, or register a new one. */
    private Driver chooseDriver() throws BookingException {
        Driver[] drivers = service.getDrivers();
        System.out.println("Drivers:");
        for (int i = 0; i < drivers.length; i++) {
            System.out.printf("%2d. %s (%d yrs experience)%n", i + 1, drivers[i].getName(), drivers[i].getExperienceYears());
        }
        System.out.println(" 0. Register a new driver");
        int pick = InputHelper.readInt("Choose driver: ", 0, drivers.length);
        if (pick > 0) {
            return drivers[pick - 1];
        }
        String name = InputHelper.capitalizeWords(InputHelper.readNonEmpty("Driver name: "));
        String phone = InputHelper.readPhone("Driver phone (10 digits): ");
        int age = InputHelper.readInt("Age (21-65): ", 21, 65);
        String licence = InputHelper.readNonEmpty("Licence number: ").toUpperCase();
        int experience = InputHelper.readInt("Years of experience (0-45): ", 0, 45);
        Driver driver = new Driver(name, phone, age, licence, experience);
        service.addPerson(driver);
        return driver;
    }

    // ---------------- 2. add a trip ----------------

    private void addTrip() throws BookingException {
        Bus[] fleet = service.getBuses();
        System.out.println();
        for (int i = 0; i < fleet.length; i++) {
            System.out.printf("%2d. %s%n", i + 1, fleet[i]);
        }
        Bus bus = fleet[InputHelper.readInt("Choose bus (number from the list): ", 1, fleet.length) - 1];
        LocalDate date = DateFmt.parseDate(InputHelper.readLine("Travel date (dd-MM-yyyy): "));
        String timeText = InputHelper.readLine("Departure time HH:mm (blank = the bus's usual "
                + DateFmt.time(bus.getDepartureTime()) + "): ");

        Trip trip;
        if (timeText.isEmpty()) {
            trip = service.addTrip(bus, date);                          // two-argument overload: usual time
        } else {
            trip = service.addTrip(bus, date, DateFmt.parseTime(timeText));   // three-argument overload: special time
        }
        store.saveOrWarn(service);
        System.out.println("Trip added:");
        service.printTrips(new Trip[] {trip});
    }

    // ---------------- 3. cancel a trip ----------------

    private void cancelTrip() throws BookingException {
        String dateText = InputHelper.readLine("Show trips for date (dd-MM-yyyy; blank = all upcoming): ");
        Trip[] options = dateText.isEmpty()
                ? service.getUpcomingTrips()
                : service.searchTrips("", "", DateFmt.parseDate(dateText));
        if (options.length == 0) {
            System.out.println("No upcoming trips to cancel.");
            return;
        }
        service.printTrips(options);
        System.out.println();

        Trip trip = service.findTrip(InputHelper.readLine("Trip ID to cancel: "));
        trip.ensureOpen(service.now());                 // fail early: already departed / already cancelled
        Booking[] affected = service.confirmedBookingsOn(trip);
        double total = 0.0;
        for (Booking b : affected) {
            total += b.getPayableAmount();
        }
        System.out.printf("Trip %s has %d confirmed booking(s); Rs.%.2f will be refunded in full.%n",
                trip.getTripId(), affected.length, total);
        if (!InputHelper.readYesNo("Cancel the whole trip and refund everyone? (y/n): ")) {
            System.out.println("Nothing was changed.");
            return;
        }

        Booking[] refunded = service.cancelTrip(trip);
        for (Booking b : refunded) {
            System.out.printf("  %-8s %-16s refunded Rs.%.2f%n", b.getPnr(), b.getPassenger().getName(), b.getRefundAmount());
            store.exportTicketOrWarn(b);                // the ticket file now says TRIP CANCELLED
        }
        store.saveOrWarn(service);
        System.out.printf("Trip %s cancelled. %d passenger(s) refunded, Rs.%.2f in total.%n",
                trip.getTripId(), refunded.length, total);
    }

    // ---------------- 4. generate schedule ----------------

    private void generateSchedule() throws BookingException {
        int days = InputHelper.readInt("Schedule how many days from today (1-" + BookingService.MAX_SCHEDULE_DAYS + ")? ",
                1, BookingService.MAX_SCHEDULE_DAYS);
        int created = service.generateSchedule(days);
        store.saveOrWarn(service);
        System.out.println(created + " trip(s) created"
                + (created == 0 ? " - every bus already has a trip on each of those days." : "."));
    }

    // ---------------- 6. reset ----------------

    private void resetData() throws BookingException {
        System.out.println("WARNING: this deletes ALL bookings, passengers, added buses and trips, and the exported ticket files,");
        System.out.println("         and puts the original sample data back.");
        if (!InputHelper.readYesNo("Really reset everything? (y/n): ")) {
            System.out.println("Nothing was changed.");
            return;
        }
        service.resetToSampleData();
        try {
            int removed = store.clearTickets();
            System.out.println(removed + " ticket file(s) deleted.");
        } catch (IOException e) {
            System.out.println("WARNING: could not delete the old ticket files (" + e.getMessage() + ")");
        }
        store.saveOrWarn(service);
        System.out.println("Reset complete: " + service.getBuses().length + " buses, "
                + service.getUpcomingTrips().length + " trips open for booking.");
    }
}
