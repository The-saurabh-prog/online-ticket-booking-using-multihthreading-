import java.util.Random;

public class UserTask implements Runnable {
    private final BookingSystem bookingSystem;
    private final String busId;
    private final String date;
    private final String userName;
    private final int seatNumberToBook;
    private final boolean simulateTimeout;
    private final Random random = new Random();

    public UserTask(BookingSystem bookingSystem, String busId, String date, String userName, int seatNumberToBook, boolean simulateTimeout) {
        this.bookingSystem = bookingSystem;
        this.busId = busId;
        this.date = date;
        this.userName = userName;
        this.seatNumberToBook = seatNumberToBook;
        this.simulateTimeout = simulateTimeout;
    }

    @Override
    public void run() {
        Bus bus = bookingSystem.getBus(busId);
        if (bus == null) return;
        SeatManager seatManager = bus.getSeatManager(date);

        // Try to select the seat
        boolean isSelected = seatManager.selectSeat(seatNumberToBook, userName);

        if (isSelected) {
            try {
                if (simulateTimeout) {
                    BookingLogger.log(userName + " is taking too long to pay for Seat-" + seatNumberToBook + "...");
                    Thread.sleep(15000); // Sleep longer than the 10 second timeout
                } else {
                    BookingLogger.log(userName + " is processing payment for Seat-" + seatNumberToBook + "...");
                    Thread.sleep(500 + random.nextInt(1000)); // Normal delay
                    seatManager.confirmBooking(seatNumberToBook, userName);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                seatManager.deselectSeat(seatNumberToBook, userName);
            }
        }
    }
}
