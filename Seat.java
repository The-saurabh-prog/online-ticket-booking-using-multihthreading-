gitpublic class Seat {
    private final int seatNumber;
    private SeatStatus status;
    private String holderName;
    private long holdExpirationTime;

    public Seat(int seatNumber) {
        this.seatNumber = seatNumber;
        this.status = SeatStatus.AVAILABLE;
        this.holderName = null;
        this.holdExpirationTime = 0;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public long getHoldExpirationTime() {
        return holdExpirationTime;
    }

    public void setHoldExpirationTime(long holdExpirationTime) {
        this.holdExpirationTime = holdExpirationTime;
    }
}
