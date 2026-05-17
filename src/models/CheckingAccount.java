package models;

import enums.AccountType;
import enums.TransactionType;
import exceptions.AccountFrozenException;
import exceptions.DailyLimitExceededException;
import exceptions.InsufficientFundsException;

public class CheckingAccount extends Account {


    private static final double DAILY_LIMIT = 5000.0 ;
    private static final double OVERDRAFT_FEE = 35.0 ;
    private final double overdraftLimit ;

    public CheckingAccount(String accountNumber, String ownerName, double initialBalance ,double overdraftLimit )
    {
        super (accountNumber , ownerName , AccountType.CHECKING , initialBalance );
        this.overdraftLimit = overdraftLimit ;
    }

    @Override
    public synchronized void withdraw(double amount)
       throws InsufficientFundsException, AccountFrozenException, DailyLimitExceededException
    {
        if(amount <= getBalance())
        {
            super.withdraw(amount) ;
        }else if(amount <= getBalance() + overdraftLimit )
        {
            double overdraftUsed  = amount - getBalance() ; // -500
            super.withdraw(getBalance()) ; // - 1000
            setBalance(getBalance() - overdraftLimit ); // -> 0 - 500 =
            chargeOverdraftFee() ; //  -500 - (-35) = -535 final result + record the transaction
            System.out.printf("  ⚠ Overdraft used: $%.2f. Fee charged: $%.2f%n", overdraftUsed, OVERDRAFT_FEE);

        }else
        {
            throw new InsufficientFundsException(amount, getBalance() + overdraftLimit);
        }
    }

    private void chargeOverdraftFee()
    {
        setBalance(getBalance() - OVERDRAFT_FEE) ;
        recordTransaction(TransactionType.WITHDRAWAL, -OVERDRAFT_FEE, "Overdraft Fee");
    }


    @Override
    public double getDailyWithdrawalLimit() { return DAILY_LIMIT; }

    @Override
    public double calculateInterest() { return 0; }

    @Override
    public String getAccountSummary() {
        return String.format(
                "  Checking Account | Owner: %-20s | Balance: $%10.2f | Overdraft Limit: $%.2f",
                getOwnerName(), getBalance(), overdraftLimit
        );
    }

    public double getOverdraftLimit() { return overdraftLimit; }
}
