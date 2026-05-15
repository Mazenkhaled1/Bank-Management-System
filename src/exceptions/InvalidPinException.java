package exceptions;

public class InvalidPinException extends Exception {

    private final int attemptsRemaining ;

    public InvalidPinException(int attemptsRemaining) {
        super("Invalid PIN. Attempts remaining: " + attemptsRemaining );
        this.attemptsRemaining = attemptsRemaining ;
    }
    public int attemptsRemaining()
    {
        return attemptsRemaining ;
    }

}
