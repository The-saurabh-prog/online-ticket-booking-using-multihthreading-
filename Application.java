import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.Arrays;

public class Application {
    public static void main(String[] args) {
        BookingLogger.log("=== Advanced Bus Ticket Booking System Started ===");

        BookingSystem bookingSystem = new BookingSystem();

        // Initialize multiple buses
        Bus bus1 = new Bus("Express-101", "Express Highway Runner", 20, 10);
        Bus bus2 = new Bus("NightRider-202", "Midnight Sleeper", 15, 10);
        bookingSystem.addBus(bus1);
        bookingSystem.addBus(bus2);

        // Pre-initialize some dates for demonstration
        List<String> dates = Arrays.asList("2026-04-28", "2026-04-29", "2026-04-30");
        for (String date : dates) {
            bus1.getSeatManager(date);
            bus2.getSeatManager(date);
        }

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
                List<Bus> busList = Arrays.asList(bus1, bus2);
                
                while (!Thread.currentThread().isInterrupted()) {
                    // Random delay between 1 to 5 seconds
                    Thread.sleep(1000 + (long)(Math.random() * 4000));
                    
                    Bus randomBus = busList.get((int) (Math.random() * busList.size()));
                    String randomDate = dates.get((int) (Math.random() * dates.size()));
                    
                    // The bots don't know the exact capacity, we just guess 1-15
                    int targetSeat = (int) (Math.random() * 15) + 1;
                    boolean simulateTimeout = Math.random() < 0.3; // 30% chance bot fails to pay
                    
                    UserTask botTask = new UserTask(bookingSystem, randomBus.getBusId(), randomDate, 
                                                    "Bot-" + botCounter++, targetSeat, simulateTimeout);
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
            bookingSystem.shutdownAll();
        }
    }
}
