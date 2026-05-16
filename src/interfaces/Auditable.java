package interfaces;

import java.util.List;

public interface Auditable {
    List<Transaction> getTransactionHistory() ;
    void printStatement() ;
}
