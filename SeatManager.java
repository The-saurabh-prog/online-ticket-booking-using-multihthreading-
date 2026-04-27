import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SeatManager {
    private final String busId;
    private final String date;
    private final List<Seat> seats;
    private final long holdTimeoutSeconds;
    private final ScheduledExecutorService scheduler;

    public SeatManager(String busId, String date, int totalSeats, long holdTimeoutSeconds) {
        this.busId = busId;
        this.date = date;
        this.seats = new ArrayList<>();
        for (int i = 1; i <= totalSeats; i++) {
            seats.add(new Seat(i));
        }
        this.holdTimeoutSeconds = holdTimeoutSeconds;
        // We use ScheduledExecutorService to precisely release seats
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public boolean selectSeat(int seatNumber, String userName) {
        if (seatNumber <= 0 || seatNumber > seats.size()) {
            return false;
        }

        Seat seat = seats.get(seatNumber - 1);

        synchronized (seat) {
            while (seat.getStatus() == SeatStatus.SELECTED) {
                try {
                    seat.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            if (seat.getStatus() == SeatStatus.AVAILABLE) {
                seat.setStatus(SeatStatus.SELECTED);
                seat.setHolderName(userName);
                seat.setHoldExpirationTime(System.currentTimeMillis() + (holdTimeoutSeconds * 1000));
                BookingLogger.log(userName + " selected Seat-" + seatNumber + " on " + busId + " (" + date + "). Locked for " + holdTimeoutSeconds + " seconds.");
                
                // Schedule timeout
                scheduleTimeout(seat, userName);
                return true;
            } else {
                return false;
            }
        }
    }

    private void scheduleTimeout(Seat seat, String userName) {
        scheduler.schedule(() -> {
            synchronized (seat) {
                // If the seat is STILL selected by the SAME user, release it due to timeout
                if (seat.getStatus() == SeatStatus.SELECTED && userName.equals(seat.getHolderName())) {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setHolderName(null);
                    seat.setHoldExpirationTime(0);
                    BookingLogger.log("Seat-" + seat.getSeatNumber() + " released due to timeout. (" + busId + " / " + date + ")");
                    seat.notifyAll(); // Wake up threads waiting for this seat
                }
            }
        }, holdTimeoutSeconds, TimeUnit.SECONDS);
    }

    public boolean confirmBooking(int seatNumber, String userName) {
        Seat seat = seats.get(seatNumber - 1);
        synchronized (seat) {
            if (seat.getStatus() == SeatStatus.SELECTED && userName.equals(seat.getHolderName())) {
                seat.setStatus(SeatStatus.BOOKED);
                seat.setHolderName(null);
                seat.setHoldExpirationTime(0);
                BookingLogger.log(userName + " confirmed booking for Seat-" + seatNumber + " on " + busId + " (" + date + ")");
                seat.notifyAll();
                return true;
            }
            return false;
        }
    }

    public boolean deselectSeat(int seatNumber, String userName) {
        Seat seat = seats.get(seatNumber - 1);
        synchronized (seat) {
            if (seat.getStatus() == SeatStatus.SELECTED && userName.equals(seat.getHolderName())) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHolderName(null);
                seat.setHoldExpirationTime(0);
                BookingLogger.log(userName + " deselected Seat-" + seatNumber + " on " + busId + " (" + date + ")");
                seat.notifyAll();
                return true;
            }
            return false;
        }
    }

    public boolean cancelBooking(int seatNumber, String userName) {
        Seat seat = seats.get(seatNumber - 1);
        synchronized (seat) {
            if (seat.getStatus() == SeatStatus.BOOKED && userName.equals(seat.getHolderName())) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHolderName(null);
                seat.setHoldExpirationTime(0);
                BookingLogger.log(userName + " cancelled their booking for Seat-" + seatNumber + " on " + busId + " (" + date + ")");
                seat.notifyAll();
                return true;
            }
            return false;
        }
    }

    public void resetAllSeats() {
        BookingLogger.log("Resetting all seats for " + busId + " on " + date);
        for (Seat seat : seats) {
            synchronized (seat) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHolderName(null);
                seat.setHoldExpirationTime(0);
                seat.notifyAll();
            }
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
