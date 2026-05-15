package enums;

public enum AccountType {

    SAVINGS("Savings Account") ,
    CHECKING("Checking Account") ,
    LOAN("Loan Account");

    private final String displayName ;
    AccountType(String displayName)
    {
        this.displayName = displayName ;
    }
    public String getDisplayName()
    {
        return displayName ;
    }
}
