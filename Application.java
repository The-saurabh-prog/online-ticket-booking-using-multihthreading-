import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Application {
    public static void main(String[] args) {
        BookingLogger.log("=== Web Ticket Booking System Started ===");

        // Increased seats for better UI visualization
        int totalSeats = 20;
        long holdTimeoutMillis = 3000; // 3 seconds timeout for holding a seat

        BookingSystem bookingSystem = new BookingSystem(totalSeats, holdTimeoutMillis);

        // Start the daemon thread to monitor timeouts
        Thread timeoutMonitorThread = new Thread(new TimeoutMonitor(bookingSystem, 500));
        timeoutMonitorThread.setDaemon(true);
        timeoutMonitorThread.start();

        // Use a thread pool for processing user bookings
        ExecutorService executorService = Executors.newFixedThreadPool(15);

        // Start Web Server
        try {
            WebServer webServer = new WebServer(bookingSystem, executorService);
            webServer.start(8080);
        } catch (Exception e) {
            BookingLogger.log("Failed to start WebServer: " + e.getMessage());
            return;
        }

        BookingLogger.log("Open http://localhost:8080 in your browser.");

        // Start a background simulator to randomly add some competition
        Thread simulatorThread = new Thread(() -> {
            try {
                int botCounter = 1;
                while (!Thread.currentThread().isInterrupted()) {
                    // Random delay between 1 to 5 seconds
                    Thread.sleep(1000 + (long)(Math.random() * 4000));
                    
                    int targetSeat = (int) (Math.random() * totalSeats) + 1;
                    boolean simulateTimeout = Math.random() < 0.3; // 30% chance bot fails to pay
                    
                    UserTask botTask = new UserTask(bookingSystem, "Bot-" + botCounter++, targetSeat, simulateTimeout);
                    executorService.submit(botTask);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        simulatorThread.setDaemon(true);
        simulatorThread.start();
        
        // Let the main thread stay alive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            BookingLogger.log("Application interrupted.");
        }
    }
}
