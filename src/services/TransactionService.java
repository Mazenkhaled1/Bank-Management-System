package services;

import exceptions.AccountFrozenException;
import exceptions.AccountNotFoundException;
import exceptions.DailyLimitExceededException;
import exceptions.InsufficientFundsException;
import interfaces.InterestBearing;
import models.Account;
import models.Transaction;
import enums.TransactionType;
import patterns.BankEventListener;
import repository.AccountRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TransactionService — handles all financial operations on accounts.
 *
 * Responsibilities:
 *   - Deposit money into an account
 *   - Withdraw money from an account
 *   - Transfer money between two accounts (with rollback on failure)
 *   - Display account statements
 *   - Filter transaction history
 *
 * OOP Concepts demonstrated:
 *   - Single Responsibility: only handles money movement, nothing else
 *   - Observer Pattern: fires events to BankEventListener after operations
 *   - Dependency Injection: AccountRepository and BankEventListener passed in
 *   - Exception propagation: checked exceptions bubble up to the controller
 *
 * Design note on transfer():
 *   A real bank uses database transactions for atomicity. Here we simulate
 *   it with a manual rollback — if the deposit fails after the withdrawal
 *   succeeds, we reverse the withdrawal to leave both accounts unchanged.
 */
public class TransactionService {

    private final AccountRepository accountRepository;
    private final BankEventListener eventListener; // Observer pattern

    // ── Constructor ───────────────────────────────────────────────────────────
    public TransactionService(AccountRepository accountRepository, BankEventListener eventListener) {
        this.accountRepository = accountRepository;
        this.eventListener     = eventListener;
    }

    // ── Deposit ───────────────────────────────────────────────────────────────

    /**
     * Deposits an amount into an account.
     *
     * Flow:
     *   1. Find account (throws if not found)
     *   2. Call account.deposit() — model handles the business rule
     *   3. Save the updated account to disk
     *   4. Fire observer events (low balance check, large transaction check)
     *
     * @throws AccountNotFoundException if account doesn't exist
     * @throws AccountFrozenException   if account is frozen
     * @throws IllegalArgumentException if amount <= 0
     */
    public void deposit(String accountNumber, double amount)
            throws AccountNotFoundException, AccountFrozenException {

        // Step 1 — Fetch account, fail fast if not found
        Account account = findAccountOrThrow(accountNumber);

        // Step 2 — Delegate to the model (model enforces its own rules)
        account.deposit(amount);

        // Step 3 — Persist the updated state
        accountRepository.save(account);

        // Step 4 — Notify observers
        fireEvents(account, amount);

        System.out.printf("  ✓ Deposited $%.2f into account %s. New balance: $%.2f%n",
                amount, accountNumber, account.getBalance());
    }

    // ── Withdraw ──────────────────────────────────────────────────────────────

    /**
     * Withdraws an amount from an account.
     *
     * All validation (frozen, daily limit, sufficient funds) is handled
     * inside Account.withdraw() — we just let exceptions bubble up.
     *
     * @throws AccountNotFoundException      if account doesn't exist
     * @throws AccountFrozenException        if account is frozen
     * @throws InsufficientFundsException    if balance is too low
     * @throws DailyLimitExceededException   if daily withdrawal limit is hit
     */
    public void withdraw(String accountNumber, double amount)
            throws AccountNotFoundException, AccountFrozenException,
            InsufficientFundsException, DailyLimitExceededException {

        Account account = findAccountOrThrow(accountNumber);

        // account.withdraw() is synchronized — thread-safe
        account.withdraw(amount);

        accountRepository.save(account);

        fireEvents(account, amount);

        System.out.printf("  ✓ Withdrew $%.2f from account %s. New balance: $%.2f%n",
                amount, accountNumber, account.getBalance());
    }

    // ── Transfer ──────────────────────────────────────────────────────────────

    /**
     * Transfers money from one account to another.
     *
     * This is the most complex operation. Here is the full flow:
     *
     *   1. Fetch both accounts (fail fast if either not found)
     *   2. Withdraw from source account
     *   3. Deposit into target account
     *      → If deposit fails: ROLLBACK — re-deposit the amount into source
     *   4. Save both accounts to disk
     *   5. Fire observer events on both accounts
     *
     * Why rollback matters:
     *   Suppose withdraw() succeeds but deposit() throws AccountFrozenException.
     *   Without rollback, the source loses money but target gains nothing.
     *   The rollback re-deposits into the source to leave both accounts unchanged.
     *
     * @throws AccountNotFoundException    if either account doesn't exist
     * @throws AccountFrozenException      if source is frozen
     * @throws InsufficientFundsException  if source has insufficient balance
     * @throws DailyLimitExceededException if source hits daily limit
     * @throws IllegalArgumentException    if source and target are the same account
     */
    public void transfer(String sourceAccountNumber, String targetAccountNumber, double amount)
            throws AccountNotFoundException, AccountFrozenException,
            InsufficientFundsException, DailyLimitExceededException {

        // Guard: cannot transfer to the same account
        if (sourceAccountNumber.equals(targetAccountNumber)) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }

        // Step 1 — Fetch both accounts upfront
        Account source = findAccountOrThrow(sourceAccountNumber);
        Account target = findAccountOrThrow(targetAccountNumber);

        // Step 2 — Withdraw from source
        // This records a TRANSFER_OUT transaction inside the model
        source.withdraw(amount);

        // Step 3 — Deposit into target
        // If this fails, we roll back the source withdrawal
        try {
            target.deposit(amount);
        } catch (AccountFrozenException e) {
            // ── ROLLBACK ──────────────────────────────────────────────────────
            // Target is frozen, so we reverse the source withdrawal.
            // We call deposit() directly to avoid re-triggering daily limit logic.
            try {
                source.deposit(amount); // restore source balance
            } catch (AccountFrozenException rollbackException) {
                // This should never happen (source was not frozen moments ago)
                // but we must handle it to satisfy the compiler
                System.err.println("  [CRITICAL] Rollback failed for account: " + sourceAccountNumber);
            }
            throw e; // re-throw the original exception to the controller
        }

        // Step 4 — Both operations succeeded — persist both accounts
        accountRepository.save(source);
        accountRepository.save(target);

        // Step 5 — Fire events on both accounts
        fireEvents(source, amount);
        fireEvents(target, amount);

        System.out.printf(
                "  ✓ Transferred $%.2f from %s to %s.%n",
                amount, sourceAccountNumber, targetAccountNumber
        );
        System.out.printf("  Source new balance: $%.2f%n", source.getBalance());
        System.out.printf("  Target new balance: $%.2f%n", target.getBalance());
    }

    // ── Statement / History ───────────────────────────────────────────────────

    /**
     * Prints the full transaction statement for an account.
     * Delegates to Account.printStatement() which is defined in the Auditable interface.
     *
     * @throws AccountNotFoundException if account doesn't exist
     */
    public void printStatement(String accountNumber) throws AccountNotFoundException {
        Account account = findAccountOrThrow(accountNumber);
        account.printStatement(); // polymorphic — each subclass formats it the same way
    }

    /**
     * Returns a filtered list of transactions for an account.
     * Uses Java Streams to filter by TransactionType.
     *
     * Example usage: get all DEPOSIT transactions for a savings account.
     *
     * @param accountNumber the account to query
     * @param type          the transaction type to filter by
     * @return list of matching transactions (may be empty)
     * @throws AccountNotFoundException if account doesn't exist
     */
    public List<Transaction> getTransactionsByType(String accountNumber, TransactionType type)
            throws AccountNotFoundException {

        Account account = findAccountOrThrow(accountNumber);

        // Stream pipeline: get all → filter by type → collect to list
        return account.getTransactionHistory()
                .stream()
                .filter(txn -> txn.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Returns the current balance of an account.
     *
     * @throws AccountNotFoundException if account doesn't exist
     */
    public double getBalance(String accountNumber) throws AccountNotFoundException {
        return findAccountOrThrow(accountNumber).getBalance();
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Fetches an account by number or throws a descriptive exception.
     * Used by every public method — avoids repeating the same Optional logic.
     */
    private Account findAccountOrThrow(String accountNumber) throws AccountNotFoundException {
        return accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    /**
     * Fires all relevant observer events after a transaction.
     * The listener decides whether the conditions are actually met.
     *
     * Observer Pattern:
     *   TransactionService (subject) tells BankEventListener (observer) what happened.
     *   NotificationService (concrete observer) decides what to do with it.
     *   TransactionService never prints alerts itself — that's the observer's job.
     */
    private void fireEvents(Account account, double amount) {
        // Check if balance is low
        eventListener.onLowBalance(account.getAccountNumber(), account.getBalance());

        // Check if this was a large transaction
        eventListener.onLargeTransaction(account.getAccountNumber(), amount);
    }
}