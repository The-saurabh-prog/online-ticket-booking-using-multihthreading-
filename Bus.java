import java.util.HashMap;
import java.util.Map;

public class Bus {
    private final String busId;
    private final String busName;
    private final Map<String, SeatManager> dateToSeatManager;
    private final int totalSeats;
    private final long holdTimeoutSeconds;

    public Bus(String busId, String busName, int totalSeats, long holdTimeoutSeconds) {
        this.busId = busId;
        this.busName = busName;
        this.totalSeats = totalSeats;
        this.holdTimeoutSeconds = holdTimeoutSeconds;
        this.dateToSeatManager = new HashMap<>();
    }

    public String getBusId() {
        return busId;
    }

    public String getBusName() {
        return busName;
    }

    // Thread-safe map access to ensure only one SeatManager per date per bus
    public synchronized SeatManager getSeatManager(String date) {
        return dateToSeatManager.computeIfAbsent(date, 
                k -> new SeatManager(busId, date, totalSeats, holdTimeoutSeconds));
    }

    public synchronized void shutdownAll() {
        for (SeatManager manager : dateToSeatManager.values()) {
            manager.shutdown();
        }
    }
}
