package exceptions;

public class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String AccountNumber) {
        super("Acount Not Found" +  AccountNumber);
    }
}
