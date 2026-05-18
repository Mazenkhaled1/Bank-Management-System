package patterns;

import enums.AccountType;
import models.Account;
import models.CheckingAccount;
import models.LoanAccount;
import models.SavingsAccount;

import java.util.UUID;

public class AccountFactory {

    private AccountFactory() {}

    public static Account createAccount(AccountType type, String ownerName, double initialBalance) {
        String accountNumber = generateAccountNumber(type);
        return switch (type) {
            case SAVINGS  -> new SavingsAccount(accountNumber, ownerName, initialBalance);
            case CHECKING -> new CheckingAccount(accountNumber, ownerName, initialBalance, 1000.0);
            case LOAN     -> throw new IllegalArgumentException(
                    "Use createLoanAccount() for loan accounts.");
        };
    }

    public static LoanAccount createLoanAccount(String ownerName, double loanAmount, double interestRate, int termMonths) {
        String accountNumber = generateAccountNumber(AccountType.LOAN);
        return new LoanAccount(accountNumber, ownerName, loanAmount, interestRate, termMonths);
    }

    private static String generateAccountNumber(AccountType type) {
        String prefix = switch (type) {
            case SAVINGS  -> "SAV";
            case CHECKING -> "CHK";
            case LOAN     -> "LNS";
        };
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}