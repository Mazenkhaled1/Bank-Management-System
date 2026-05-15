package exceptions;

public class AccountFrozenException extends Exception {

    public AccountFrozenException(String AccountNumber) {
        super("Account" + AccountNumber + "Is Frozen Please Contact Support ");

    }
}
