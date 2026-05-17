package models;

import enums.AccountType;
import enums.TransactionType;
import interfaces.InterestBearing;

public class SavingsAccount extends Account implements InterestBearing {
    private static final double DAILY_LIMIT        = 3000.0;
    private static final double INTEREST_RATE      = 0.035;
    private static final double MIN_BALANCE        = 100.0;

    public SavingsAccount(String accountNumber, String ownerName, double initialBalance) {
        super(accountNumber ,ownerName ,AccountType.SAVINGS, initialBalance ) ;
    }
    @Override
    public double calculateInterest() { // calculated the interset for the savings
        return getBalance() * INTEREST_RATE ;
    }

    @Override
    public void applyInterest()
    {
        double interest = calculateInterest();
        setBalance (getBalance() + interest) ;
        recordTransaction(TransactionType.INTEREST, interest, "Monthly Interest Applied");
        System.out.printf("  Interest of $%.2f applied to account %s.%n", interest, getAccountNumber());
    }

    @Override
    public double getDailyWithdrawalLimit() { return DAILY_LIMIT; }

    @Override
    public String getAccountSummary() {
        return String.format(
                "  Savings Account | Owner: %-20s | Balance: $%10.2f | Rate: %.1f%% p.a. | Min Balance: $%.2f",
                getOwnerName(), getBalance(), INTEREST_RATE * 100, MIN_BALANCE
        );
    }

    public double getMinimumBalance() { return MIN_BALANCE; }
    public double getInterestRate()   { return INTEREST_RATE; }



}
