package interfaces;

import exceptions.AccountFrozenException;
import exceptions.InsufficientFundsException;

public interface Transferable {

     void transfer(String targetAccountNumber , double amount)
          throws AccountFrozenException, InsufficientFundsException ;
}
