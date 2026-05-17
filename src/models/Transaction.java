package models;

import enums.TransactionType;
import enums.TransactionStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.Serializable;

public final class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String transactionId;
    private final String accountNumber;
    private final TransactionType type;
    private final double amount;
    private final double balanceAfter;
    private final LocalDateTime timestamp;
    private final TransactionStatus status;
    private final String note;

    public Transaction(String transactionId, String accountNumber, TransactionType type,
                       double amount, double balanceAfter, TransactionStatus status, String note) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.note = note;
    }


    public String getTransactionId()  { return transactionId; }
    public String getAccountNumber()  { return accountNumber; }
    public TransactionType getType()  { return type; }
    public double getAmount()         { return amount; }
    public double getBalanceAfter()   { return balanceAfter; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public TransactionStatus getStatus() { return status; }
    public String getNote()           { return note; }

    @Override
    public String toString() {
        return String.format("| %-12s | %-20s | %+10.2f | %10.2f | %s |",
                transactionId, type.getDisplayName(), amount, balanceAfter,
                timestamp.format(FORMATTER));
    }
}