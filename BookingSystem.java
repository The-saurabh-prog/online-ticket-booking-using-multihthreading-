import java.util.ArrayList;
import java.util.List;

public class BookingSystem {
    private final List<Seat> seats;
    private final long holdTimeoutMillis;

    public BookingSystem(int totalSeats, long holdTimeoutMillis) {
        this.seats = new ArrayList<>();
        for (int i = 1; i <= totalSeats; i++) {
            seats.add(new Seat(i));
        }
        this.holdTimeoutMillis = holdTimeoutMillis;
        BookingLogger.log("BookingSystem initialized with " + totalSeats + " seats.");
    }

    public List<Seat> getSeats() {
        return seats;
    }

    /**
     * Tries to hold a seat. If the seat is currently HELD, the thread will wait.
     * @return true if successfully held, false if the seat is BOOKED.
     */
    public boolean tryHoldSeat(int seatNumber, String userName) {
        if (seatNumber <= 0 || seatNumber > seats.size()) {
            BookingLogger.log(userName + " failed: Invalid seat number " + seatNumber);
            return false;
        }

        Seat seat = seats.get(seatNumber - 1);

        synchronized (seat) {
            // Wait if the seat is currently being held by someone else
            while (seat.getStatus() == SeatStatus.HELD) {
                BookingLogger.log(userName + " is waiting for Seat-" + seatNumber + " to be released or confirmed.");
                try {
                    seat.wait(); // Release the lock and wait to be notified
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            // Once out of the loop, check if it's available
            if (seat.getStatus() == SeatStatus.AVAILABLE) {
                seat.setStatus(SeatStatus.HELD);
                seat.setHolderName(userName);
                seat.setHoldExpirationTime(System.currentTimeMillis() + holdTimeoutMillis);
                BookingLogger.log(userName + " temporarily held Seat-" + seatNumber);
                return true;
            } else {
                // It must be BOOKED
                BookingLogger.log(userName + " failed: Seat-" + seatNumber + " is already booked.");
                return false;
            }
        }
    }

    /**
     * Confirms the booking of a held seat.
     * @return true if successfully confirmed.
     */
    public boolean confirmBooking(int seatNumber, String userName) {
        Seat seat = seats.get(seatNumber - 1);

        synchronized (seat) {
            if (seat.getStatus() == SeatStatus.HELD && userName.equals(seat.getHolderName())) {
                seat.setStatus(SeatStatus.BOOKED);
                seat.setHolderName(null);
                seat.setHoldExpirationTime(0);
                BookingLogger.log(userName + " successfully booked Seat-" + seatNumber);
                
                // Wake up all threads waiting for this seat
                seat.notifyAll();
                return true;
            }
            BookingLogger.log(userName + " failed to confirm Seat-" + seatNumber);
            return false;
        }
    }

    /**
     * Cancels a hold and makes the seat available again.
     */
    public void cancelHold(int seatNumber, String userName) {
        Seat seat = seats.get(seatNumber - 1);

        synchronized (seat) {
            if (seat.getStatus() == SeatStatus.HELD && userName.equals(seat.getHolderName())) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHolderName(null);
                seat.setHoldExpirationTime(0);
                BookingLogger.log(userName + " cancelled hold on Seat-" + seatNumber);
                
                // Notify waiting threads that the seat is available
                seat.notifyAll();
            }
        }
    }
}
