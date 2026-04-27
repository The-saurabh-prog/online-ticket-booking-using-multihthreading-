public class TimeoutMonitor implements Runnable {
    private final BookingSystem bookingSystem;
    private final long checkIntervalMillis;

    public TimeoutMonitor(BookingSystem bookingSystem, long checkIntervalMillis) {
        this.bookingSystem = bookingSystem;
        this.checkIntervalMillis = checkIntervalMillis;
    }

    @Override
    public void run() {
        BookingLogger.log("TimeoutMonitor started.");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                long currentTime = System.currentTimeMillis();

                for (Seat seat : bookingSystem.getSeats()) {
                    synchronized (seat) {
                        if (seat.getStatus() == SeatStatus.HELD && currentTime > seat.getHoldExpirationTime()) {
                            String holder = seat.getHolderName();
                            seat.setStatus(SeatStatus.AVAILABLE);
                            seat.setHolderName(null);
                            seat.setHoldExpirationTime(0);
                            
                            BookingLogger.log("TimeoutMonitor: Hold expired for Seat-" + seat.getSeatNumber() 
                                    + " (Held by " + holder + "). Seat is now AVAILABLE.");
                            
                            // Wake up threads waiting on this seat
                            seat.notifyAll();
                        }
                    }
                }
                
                // Sleep before checking again
                Thread.sleep(checkIntervalMillis);
            }
        } catch (InterruptedException e) {
            BookingLogger.log("TimeoutMonitor stopped.");
            Thread.currentThread().interrupt();
        }
    }
}
