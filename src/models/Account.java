package models;

import enums.AccountType;
import enums.TransactionStatus;
import enums.TransactionType;
import exceptions.AccountFrozenException;
import exceptions.DailyLimitExceededException;
import exceptions.InsufficientFundsException;
import interfaces.Auditable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class   Account implements Auditable , Serializable {
    private static final long serialVersionUID = 1L;

    private final String accountNumber;
    private final String ownerName;
    private final AccountType accountType;
    private final LocalDate createdAt;

    private double balance;
    private boolean frozen;
    private double dailyWithdrawnAmount;
    private LocalDate lastWithdrawalDate;

    protected static final double DEFAULT_DAILY_LIMIT = 5000.0;
    private final List<Transaction> transactionHistory;


    protected Account(String accountNumber, String ownerName, AccountType accountType, double initialBalance) {
        this.accountNumber      = accountNumber;
        this.ownerName          = ownerName;
        this.accountType        = accountType;
        this.balance            = initialBalance;
        this.frozen             = false;
        this.createdAt          = LocalDate.now();
        this.dailyWithdrawnAmount = 0;
        this.lastWithdrawalDate = LocalDate.now();
        this.transactionHistory = new ArrayList<>();
    }


    public abstract double getDailyWithdrawalLimit();
    public abstract double calculateInterest();
    public abstract String getAccountSummary();


    public synchronized void deposit(double amount) throws AccountFrozenException {
        validateNotFrozen();
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive.");

        this.balance += amount;
        recordTransaction(TransactionType.DEPOSIT, amount, "Deposit");
    }

    public synchronized void withdraw(double amount)
            throws InsufficientFundsException, AccountFrozenException, DailyLimitExceededException {
        validateNotFrozen();
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive.");

        resetDailyLimitIfNewDay();

        if (dailyWithdrawnAmount + amount > getDailyWithdrawalLimit()) {
            throw new DailyLimitExceededException(getDailyWithdrawalLimit());
        }
        if (amount > balance) {
            throw new InsufficientFundsException(amount, balance);
        }

        this.balance -= amount;
        this.dailyWithdrawnAmount += amount;
        this.lastWithdrawalDate = LocalDate.now();
        recordTransaction(TransactionType.WITHDRAWAL, -amount, "Withdrawal");
    }


    protected void recordTransaction(TransactionType type, double amount, String note) {
        String id = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction transaction = new Transaction(id, accountNumber, type, amount, balance, TransactionStatus.SUCCESS, note);
        transactionHistory.add(transaction);
    }


    private void validateNotFrozen() throws AccountFrozenException {
        if (frozen) throw new AccountFrozenException(accountNumber);
    }

    private void resetDailyLimitIfNewDay() {
        if (!LocalDate.now().equals(lastWithdrawalDate)) {
            dailyWithdrawnAmount = 0;
        }
    }


    // ── Auditable implementation ───────────────────────────────────────────────
    @Override
    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    @Override
    public void printStatement() {
        System.out.println("\n" + "=".repeat(85));
        System.out.printf("  ACCOUNT STATEMENT — %s (%s)%n", ownerName, accountNumber);
        System.out.println("=".repeat(85));
        System.out.printf("| %-12s | %-20s | %10s | %10s | %-19s |%n",
                "TXN ID", "TYPE", "AMOUNT", "BALANCE", "TIMESTAMP");
        System.out.println("-".repeat(85));
        if (transactionHistory.isEmpty()) {
            System.out.println("  No transactions found.");
        } else {
            transactionHistory.forEach(System.out::println);
        }
        System.out.println("=".repeat(85));
        System.out.printf("  Current Balance: $%.2f%n", balance);
        System.out.println("=".repeat(85) + "\n");
    }



    public String getAccountNumber()  { return accountNumber; }
    public String getOwnerName()      { return ownerName; }
    public AccountType getAccountType() { return accountType; }
    public double getBalance()        { return balance; }
    public boolean isFrozen()         { return frozen; }
    public LocalDate getCreatedAt()   { return createdAt; }


    public void freeze()   { this.frozen = true; }
    public void unfreeze() { this.frozen = false; }


    protected void setBalance(double balance) { this.balance = balance; }

    @Override
    public String toString() {
        return String.format("[%s] %s — $%.2f %s",
                accountType.getDisplayName(), accountNumber, balance,
                frozen ? "(FROZEN)" : "");
    }


}

