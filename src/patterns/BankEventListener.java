package patterns;

public interface BankEventListener {

   void onLowBalance(String accountNumber, double balance);
   void onAccountFrozen(String accountNumber);
   void onLargeTransaction(String accountNumber, double amount);
}
