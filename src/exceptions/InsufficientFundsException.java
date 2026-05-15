package exceptions;

public class InsufficientFundsException extends Exception {
    private final double requested ;
    private final double available ;
    public InsufficientFundsException(double requested ,  double available) {
        super(String.format("Insufficient funds. Requested: $%.2f, Available: $%.2f", requested, available));
        this.requested = requested;
        this.available = available;
    }
    public double getRequested() {
        return requested;
    }
    public double getAvailable() {
        return available;
    }
}
