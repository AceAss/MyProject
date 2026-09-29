package com.busbooking.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

import com.busbooking.exception.BookingException;

/**
 * One place for every date/time format used on screen, in tickets and when reading user input.
 */
public final class DateFmt {

    // STRICT + "uuuu" means 31-02-2026 is rejected instead of silently becoming 28-02-2026
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("EEE dd-MM-uuuu HH:mm", Locale.ENGLISH);

    private DateFmt() {
    }

    public static String date(LocalDate date) {
        return date.format(DATE);
    }

    public static String time(LocalTime time) {
        return time.format(TIME);
    }

    public static String dateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME);                  // e.g. "Mon 05-10-2026 20:30"
    }

    /** "05-10-2026" -> LocalDate. Bad input becomes a BookingException (with the parser error as its cause). */
    public static LocalDate parseDate(String text) throws BookingException {
        try {
            return LocalDate.parse(text.trim(), DATE);
        } catch (DateTimeParseException e) {                // [V2-2] try / catch
            throw new BookingException("Invalid date '" + text.trim() + "' - use dd-MM-yyyy, e.g. 05-10-2026", e);
        }
    }

    /** "22:30" -> LocalTime (24-hour clock). */
    public static LocalTime parseTime(String text) throws BookingException {
        try {
            return LocalTime.parse(text.trim(), TIME);
        } catch (DateTimeParseException e) {
            throw new BookingException("Invalid time '" + text.trim() + "' - use HH:mm on a 24-hour clock, e.g. 22:30", e);
        }
    }
}
