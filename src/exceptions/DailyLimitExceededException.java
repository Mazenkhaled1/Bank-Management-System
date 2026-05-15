package exceptions;

public class DailyLimitExceededException extends Exception {

    public DailyLimitExceededException(double limit ) {
        super(String.format("Daily limit of $%.2f exceeded" , limit));
    }



}
