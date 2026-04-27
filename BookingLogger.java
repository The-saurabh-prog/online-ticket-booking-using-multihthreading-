import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BookingLogger {
    private static final String LOG_FILE = "booking_log.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Object lock = new Object(); // Ensure thread-safe logging

    public static void log(String message) {
        String timestampedMessage = "[" + LocalDateTime.now().format(formatter) + "] [" 
                + Thread.currentThread().getName() + "] " + message;
        
        // Print to console
        System.out.println(timestampedMessage);
        
        // Write to file (synchronized to avoid race conditions across threads)
        synchronized (lock) {
            try (FileWriter fw = new FileWriter(LOG_FILE, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println(timestampedMessage);
            } catch (IOException e) {
                System.err.println("Failed to write to log file: " + e.getMessage());
            }
        }
    }
}
