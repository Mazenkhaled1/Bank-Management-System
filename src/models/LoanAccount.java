package models;

import enums.AccountType;
import enums.TransactionType;
import exceptions.AccountFrozenException;
import exceptions.DailyLimitExceededException;
import exceptions.InsufficientFundsException;
import interfaces.InterestBearing;

public class LoanAccount extends Account implements InterestBearing {

    private static final double DAILY_LIMIT  = 0.0; // No withdrawals on loan accounts
    private final double loanAmount;
    private final double interestRate;      // annual rate
    private final int    termMonths;
    private double       outstandingBalance;

    public LoanAccount(String accountNumber, String ownerName,
                       double loanAmount, double interestRate, int termMonths) {
        super(accountNumber, ownerName, AccountType.LOAN, 0);
        this.loanAmount        = loanAmount;
        this.interestRate      = interestRate;
        this.termMonths        = termMonths;
        this.outstandingBalance = loanAmount;
        // Initial disbursement record
        setBalance(loanAmount);
        recordTransaction(TransactionType.DEPOSIT, loanAmount, "Loan Disbursement");
    }


    public void makeRepayment(double amount) throws InsufficientFundsException {
        if (amount <= 0) throw new IllegalArgumentException("Repayment amount must be positive.");
        if (outstandingBalance <= 0) {
            System.out.println("  Loan is already fully repaid.");
            return;
        }
        double actual = Math.min(amount, outstandingBalance);
        outstandingBalance -= actual;
        setBalance(outstandingBalance);
        recordTransaction(TransactionType.LOAN_REPAYMENT, -actual, "Loan Repayment");
        System.out.printf("  Repayment of $%.2f accepted. Remaining: $%.2f%n", actual, outstandingBalance);
    }


    @Override
    public double calculateInterest() {
        return outstandingBalance * (interestRate / 12);
    }

    @Override
    public void applyInterest() {
        if (outstandingBalance <= 0) return;
        double interest = calculateInterest();
        outstandingBalance += interest;
        setBalance(outstandingBalance);
        recordTransaction(TransactionType.INTEREST, interest, "Monthly Loan Interest");
    }


    public double getMonthlyInstallment() {
        double r = interestRate / 12;
        return (loanAmount * r * Math.pow(1 + r, termMonths)) / (Math.pow(1 + r, termMonths) - 1);
    }


    @Override
    public void withdraw(double amount) throws InsufficientFundsException, AccountFrozenException, DailyLimitExceededException {
        throw new UnsupportedOperationException("Withdrawals are not allowed on loan accounts. Use makeRepayment() instead.");
    }


    @Override
    public double getDailyWithdrawalLimit() { return DAILY_LIMIT; }

    @Override
    public String getAccountSummary() {
        return String.format(
                "  Loan Account     | Owner: %-20s | Outstanding: $%10.2f | Rate: %.1f%% | Term: %d months | Monthly: $%.2f",
                getOwnerName(), outstandingBalance, interestRate * 100, termMonths, getMonthlyInstallment()
        );
    }

    public double getOutstandingBalance() { return outstandingBalance; }
    public double getLoanAmount()         { return loanAmount; }
    public double getInterestRate()       { return interestRate; }
    public int    getTermMonths()         { return termMonths; }
    public boolean isFullyRepaid()        { return outstandingBalance <= 0; }
}