package models;

import enums.UserRole;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int  MAX_ATTEMPTS     = 3;

    private final String   userId;
    private final String   name;
    private final String   email;
    private final UserRole role;
    private       String   hashedPin;
    private       int      failedAttempts;
    private       boolean  locked;

    private final List<String> accountNumbers; // linked account IDs

    public User(String userId, String name, String email, String hashedPin, UserRole role) {
        this.userId         = userId;
        this.name           = name;
        this.email          = email;
        this.hashedPin      = hashedPin;
        this.role           = role;
        this.failedAttempts = 0;
        this.locked         = false;
        this.accountNumbers = new ArrayList<>();
    }



    public boolean verifyPin(String hashedAttempt) {
        if (locked) return false;
        if (this.hashedPin.equals(hashedAttempt)) {
            failedAttempts = 0;
            return true;
        }
        failedAttempts++;
        if (failedAttempts >= MAX_ATTEMPTS) {
            locked = true;
        }
        return false;
    }

    public void changePin(String newHashedPin) {
        this.hashedPin = newHashedPin;
    }

    public void unlock() {
        this.locked         = false;
        this.failedAttempts = 0;
    }


    public void addAccount(String accountNumber) {
        if (!accountNumbers.contains(accountNumber)) {
            accountNumbers.add(accountNumber);
        }
    }

    public void removeAccount(String accountNumber) {
        accountNumbers.remove(accountNumber);
    }

    public List<String> getAccountNumbers() {
        return Collections.unmodifiableList(accountNumbers);
    }


    public String   getUserId()        { return userId; }
    public String   getName()          { return name; }
    public String   getEmail()         { return email; }
    public UserRole getRole()          { return role; }
    public boolean  isLocked()         { return locked; }
    public int      getFailedAttempts(){ return failedAttempts; }
    public int      getAttemptsLeft()  { return Math.max(0, MAX_ATTEMPTS - failedAttempts); }

    @Override
    public String toString() {
        return String.format("User[%s | %s | %s | %s]", userId, name, email, role);
    }
}