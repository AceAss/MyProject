package com.busbooking.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;

import com.busbooking.model.Booking;

/**
 * Everything that touches the disk:
 *  - the save file  (the whole BookingService written with Java serialization, so bookings survive a restart)
 *  - ticket files   (one printable .txt per booking, e.g. tickets/PNR1001.txt)
 */
public class DataStore {                                // [V2-6] [F09] two constructors

    private final Path dataFile;
    private final Path ticketDir;

    /** Default location: data/busbooking.dat, tickets in data/tickets/. */
    public DataStore() {
        this(Paths.get("data", "busbooking.dat"));      // [F12] constructor chaining
    }

    public DataStore(Path dataFile) {
        this.dataFile = dataFile;
        Path folder = dataFile.getParent();                 // null when the file name has no folder part
        this.ticketDir = (folder == null) ? Paths.get("tickets") : folder.resolve("tickets");
    }

    public Path getDataFile() {
        return dataFile;
    }

    public Path getTicketDir() {
        return ticketDir;
    }

    public boolean exists() {
        return Files.exists(dataFile);
    }

    // ---------------- save / load ----------------

    /**
     * Writes the whole system to the save file. It is written to a temporary file first and then moved over
     * the real one, so a crash half-way through never leaves a damaged save file behind.
     */
    public void save(BookingService service) throws IOException {               // [V2-6] 'throws' + 'finally' for closing the stream
        Path folder = dataFile.getParent();
        if (folder != null) {
            Files.createDirectories(folder);
        }
        Path temp = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");

        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)));
            out.writeObject(service);
        } finally {
            if (out != null) {
                out.close();                            // always closed - even if writeObject() failed half-way
            }
        }
        Files.move(temp, dataFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Reads the save file back. Any problem with the file is reported as an IOException with a readable reason. */
    public BookingService load(Clock clock) throws IOException {                // [V2-6]
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(dataFile)));
            BookingService loaded = (BookingService) in.readObject();
            loaded.afterLoad(clock);
            return loaded;
        } catch (EOFException e) {                                              // file ends too early
            throw new IOException("the file is incomplete (it was cut short)", e);
        } catch (ObjectStreamException e) {                                     // bad header, or the classes changed since it was saved
            throw new IOException("the file is damaged or was saved by a different version of the program", e);
        } catch (ClassNotFoundException | ClassCastException e) {               // both mean "this is not one of our save files"
            throw new IOException("the file is not a valid save file", e);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /** Save, but only warn on failure - used after every change so the caller does not need its own try/catch. */
    public void saveOrWarn(BookingService service) {
        try {
            save(service);
        } catch (IOException e) {
            System.out.println("WARNING: could not save data to " + dataFile + " (" + e.getMessage() + ")");
        }
    }

    /** Renames an unreadable save file to busbooking.dat.corrupt so it is not silently overwritten. */
    public Path moveAside() throws IOException {
        Path aside = dataFile.resolveSibling(dataFile.getFileName() + ".corrupt");
        return Files.move(dataFile, aside, StandardCopyOption.REPLACE_EXISTING);
    }

    // ---------------- ticket files ----------------

    /** Writes (or overwrites) tickets/PNRxxxx.txt with the current text of the ticket. */
    public Path exportTicket(Booking booking) throws IOException {              // [V2-6]
        Files.createDirectories(ticketDir);
        Path file = ticketDir.resolve(booking.getPnr() + ".txt");
        BufferedWriter out = null;
        try {
            out = Files.newBufferedWriter(file);
            out.write(booking.getTicketText());
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return file;
    }

    /** Export, but only warn on failure. Returns true if the file was written. */
    public boolean exportTicketOrWarn(Booking booking) {
        try {
            exportTicket(booking);
            return true;
        } catch (IOException e) {
            System.out.println("WARNING: could not write the ticket file for " + booking.getPnr() + " (" + e.getMessage() + ")");
            return false;
        }
    }

    /** Deletes every exported ticket (PNR*.txt) - used when the data is reset. Returns how many were removed. */
    public int clearTickets() throws IOException {
        if (!Files.isDirectory(ticketDir)) {
            return 0;
        }
        int removed = 0;
        DirectoryStream<Path> files = Files.newDirectoryStream(ticketDir, "PNR*.txt");
        try {
            for (Path file : files) {
                Files.delete(file);
                removed++;
            }
        } finally {
            files.close();
        }
        return removed;
    }
}
