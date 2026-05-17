package models;

import interfaces.Auditable;

import java.io.Serializable;
import java.util.List;

public class Account implements Auditable , Serializable {
    private static final long serialVersionUID = 1L;

    List<Transaction> getTransactionHistory() {
        System.out.println("as");
    };
    void printStatement() {
        System.out.println("s");
    };
}

