import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BookingSystem {
    private final Map<String, Bus> buses;

    public BookingSystem() {
        this.buses = new HashMap<>();
    }

    public void addBus(Bus bus) {
        buses.put(bus.getBusId(), bus);
    }

    public Collection<Bus> getBuses() {
        return buses.values();
    }

    public Bus getBus(String busId) {
        return buses.get(busId);
    }

    public void shutdownAll() {
        for (Bus bus : buses.values()) {
            bus.shutdownAll();
        }
    }
}
