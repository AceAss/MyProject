package com.busbooking.util; // [F21] custom package

import java.util.Scanner;

/**
 * Console input/output helpers (all static). Every read is trimmed and validated.
 */
public class InputHelper {

    private static final Scanner SC = new Scanner(System.in);   // [F08] Scanner for console input
    private static boolean echo = false;            // true => every line read is printed back (see Main: --echo)

    private InputHelper() {
        // utility class - no objects needed
    }

    public static String readLine(String prompt) {
        System.out.print(prompt);
        if (!SC.hasNextLine()) {
            System.out.println();
            System.out.println("Input ended. Exiting.");
            System.exit(0);
        }
        String line = SC.nextLine().trim();             // [F13] trim() on user input
        if (echo) {
            System.out.println(line);                   // a terminal shows what you type by itself; redirected input does not
        }
        return line;
    }

    /** Print every line read. Only useful when input comes from a file, to make the transcript readable. */
    public static void setEcho(boolean on) {
        echo = on;
    }

    public static String readNonEmpty(String prompt) {
        while (true) {                                  // [F06] while loop
            String text = readLine(prompt);
            if (!text.isEmpty()) {
                return text;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    public static int readInt(String prompt) {          // [F10] overloading: (prompt)
        while (true) {
            String line = readLine(prompt);
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    public static int readInt(String prompt, int min, int max) {   // [F10] overloading: (prompt, min, max)
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.printf("Please enter a number between %d and %d.%n", min, max);
        }
    }

    public static double readDouble(String prompt) {
        while (true) {
            String line = readLine(prompt);
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid amount.");
            }
        }
    }

    public static boolean readYesNo(String prompt) {
        while (true) {
            String answer = readLine(prompt).toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            System.out.println("Please answer y or n.");
        }
    }

    public static String readPhone(String prompt) {
        while (true) {
            String phone = readLine(prompt);
            if (phone.matches("\\d{10}")) {
                return phone;
            }
            System.out.println("Phone number must be exactly 10 digits.");
        }
    }

    /** "aisha   rAHMAN" -> "Aisha Rahman". */
    public static String capitalizeWords(String text) {
        String[] words = text.trim().split("\\s+");     // [F13] split
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;                               // [F06] continue
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    /** "3, 4,5" -> {3, 4, 5}. Returns an empty array if anything is not a number. */
    public static int[] parseSeatList(String input) {
        String[] parts = input.split(",");              // [F13] split
        int[] seats = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                seats[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                return new int[0];
            }
        }
        return seats;
    }

    public static void printLine(char symbol, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(symbol);
        }
        System.out.println(sb);
    }

    public static void close() {
        SC.close();
    }
}
