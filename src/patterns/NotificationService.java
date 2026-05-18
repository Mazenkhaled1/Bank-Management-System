package patterns;


public class NotificationService implements BankEventListener {

    private static final double LOW_BALANCE_THRESHOLD   = 200.0;
    private static final double LARGE_TRANSACTION_LIMIT = 2000.0;

    @Override
    public void onLowBalance(String accountNumber, double balance) {
        if (balance < LOW_BALANCE_THRESHOLD) {
            System.out.printf("%n  ⚠  ALERT: Account %s has a low balance of $%.2f%n%n",
                    accountNumber, balance);
        }
    }

    @Override
    public void onAccountFrozen(String accountNumber) {
        System.out.printf("%n  🔒 ALERT: Account %s has been FROZEN.%n%n", accountNumber);
    }

    @Override
    public void onLargeTransaction(String accountNumber, double amount) {
        if (amount >= LARGE_TRANSACTION_LIMIT) {
            System.out.printf("%n  💰 NOTICE: Large transaction of $%.2f on account %s.%n%n",
                    amount, accountNumber);
        }
    }
}