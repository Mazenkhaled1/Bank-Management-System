package enums;

public enum TransactionType {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    TRANSFER_IN("Transfer_In"),
    TRANSFER_OUT("Transfer_Out"),
    INTEREST("Insert"),
    LOAN_REPAYMENT("Loan_Payment");

    private final String  displayName ;

    TransactionType(String displayName)
    {
        this.displayName = displayName ;
    }
    public String displayName()
    {
        return  displayName ;
    }
}
