import java.util.Random;

public class UserTask implements Runnable {
    private final BookingSystem bookingSystem;
    private final String userName;
    private final int seatNumberToBook;
    private final boolean simulateTimeout;
    private final Random random = new Random();

    public UserTask(BookingSystem bookingSystem, String userName, int seatNumberToBook, boolean simulateTimeout) {
        this.bookingSystem = bookingSystem;
        this.userName = userName;
        this.seatNumberToBook = seatNumberToBook;
        this.simulateTimeout = simulateTimeout;
    }

    @Override
    public void run() {
        BookingLogger.log(userName + " is attempting to book Seat-" + seatNumberToBook);

        // Try to hold the seat
        boolean isHeld = bookingSystem.tryHoldSeat(seatNumberToBook, userName);

        if (isHeld) {
            try {
                // Simulate time taken for payment process
                if (simulateTimeout) {
                    BookingLogger.log(userName + " is taking too long to pay for Seat-" + seatNumberToBook + "...");
                    Thread.sleep(4000); // Sleep longer than the timeout
                } else {
                    BookingLogger.log(userName + " is processing payment for Seat-" + seatNumberToBook + "...");
                    Thread.sleep(500 + random.nextInt(1000)); // Normal payment delay
                }

                // If not simulating a timeout, try to confirm booking
                if (!simulateTimeout) {
                    bookingSystem.confirmBooking(seatNumberToBook, userName);
                } else {
                    // Let the timeout monitor handle the expiration
                    // We don't confirm here
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                BookingLogger.log(userName + "'s transaction was interrupted.");
                bookingSystem.cancelHold(seatNumberToBook, userName);
            }
        } else {
            // Seat is already booked, tryHoldSeat already logged the failure
        }
    }
}
