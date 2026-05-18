package services;

import exceptions.AccountNotFoundException;
import models.Account;
import models.User;
import repository.AccountRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ATMService — customer-facing helper service for the ATM menu.
 *
 * Responsibilities:
 *   - Verify that a logged-in user actually owns the account they're trying to use
 *   - Fetch all accounts belonging to the currently logged-in user
 *
 * Why this service exists (important design reason):
 *   Without this class, ATMController would need to access UserRepository and
 *   AccountRepository directly to do ownership checks. That violates the rule
 *   that controllers should only talk to services, not repositories.
 *
 *   Also, the ownership check is a security rule — it belongs in the service
 *   layer, not in the UI layer. If ATMController handled it, someone could
 *   accidentally bypass it in a future code change.
 *
 * OOP Concepts demonstrated:
 *   - Single Responsibility: this service only handles ATM-specific concerns
 *   - Encapsulation: ownership logic is hidden from the controller
 *   - Streams: collecting account objects from a list of account numbers
 *   - Dependency Injection: AccountRepository passed via constructor
 */
public class ATMService {

    private final AccountRepository accountRepository;

    // ── Constructor ───────────────────────────────────────────────────────────
    public ATMService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // ── Ownership Check ───────────────────────────────────────────────────────

    /**
     * Verifies that the logged-in user owns the given account number.
     *
     * This MUST be called before every ATM operation (deposit, withdraw,
     * transfer, view statement). Without it, any user could operate on
     * any account just by typing its number.
     *
     * Flow:
     *   1. Get the user's list of account numbers (owned accounts)
     *   2. Check if the requested account number is in that list
     *   3. If not → throw SecurityException with a clear message
     *
     * Why SecurityException?
     *   This is not a business rule violation (like insufficient funds) —
     *   it is a security violation. Using a different exception type makes
     *   it clear to the caller that something potentially malicious happened.
     *
     * @param user          the currently logged-in user
     * @param accountNumber the account number the user wants to access
     * @throws SecurityException if the user does not own this account
     */
    public void checkOwnership(User user, String accountNumber) {
        // user.getAccountNumbers() returns an unmodifiable list (encapsulation in User)
        boolean ownsAccount = user.getAccountNumbers().contains(accountNumber);

        if (!ownsAccount) {
            throw new SecurityException(
                    "Access denied. Account " + accountNumber +
                            " does not belong to user " + user.getName() + "."
            );
        }
    }

    // ── Fetch User's Accounts ─────────────────────────────────────────────────

    /**
     * Returns a list of all Account objects belonging to the logged-in user.
     *
     * Flow:
     *   1. Get the user's list of account numbers (strings)
     *   2. For each account number, fetch the Account from the repository
     *   3. Skip any that aren't found (defensive programming — the account
     *      might have been deleted by an admin while the user was logged in)
     *   4. Return the list of Account objects
     *
     * Stream pipeline explained:
     *   user.getAccountNumbers()          → List<String> of account numbers
     *   .stream()                         → convert list to stream
     *   .map(accountRepository::findById) → each number → Optional<Account>
     *   .filter(Optional::isPresent)      → keep only Optionals that have a value
     *   .map(Optional::get)               → unwrap Optional → Account
     *   .collect(Collectors.toList())     → gather results into a List<Account>
     *
     * @param user the logged-in user
     * @return list of Account objects the user owns (may be empty if none exist)
     */
    public List<Account> getMyAccounts(User user) {
        return user.getAccountNumbers()
                .stream()
                .map(accountRepository::findById)   // String → Optional<Account>
                .filter(opt -> opt.isPresent())     // keep only found accounts
                .map(opt -> opt.get())              // unwrap Optional
                .collect(Collectors.toList());
    }

    /**
     * Displays all accounts belonging to the logged-in user in a formatted table.
     * Uses getAccountSummary() — a polymorphic call defined as abstract in Account.
     *
     * @param user the logged-in user
     */
    public void displayMyAccounts(User user) {
        List<Account> myAccounts = getMyAccounts(user);

        if (myAccounts.isEmpty()) {
            System.out.println("  You have no accounts linked to your profile.");
            return;
        }

        System.out.println("\n" + "=".repeat(85));
        System.out.printf("  MY ACCOUNTS — %s%n", user.getName());
        System.out.println("=".repeat(85));

        // getAccountSummary() is abstract — each subclass provides its own format.
        // This is polymorphism: same method call, different output per type.
        myAccounts.forEach(account -> {
            System.out.println(account.getAccountSummary());
            System.out.printf("    Status: %s%n", account.isFrozen() ? "🔒 FROZEN" : "✓ Active");
            System.out.println("    " + "-".repeat(80));
        });

        System.out.println("=".repeat(85) + "\n");
    }

    /**
     * Checks if a specific account number belongs to the user, returning a boolean
     * instead of throwing. Useful for menu validation before prompting.
     *
     * @param user          the logged-in user
     * @param accountNumber the account to check
     * @return true if the user owns the account, false otherwise
     */
    public boolean ownsAccount(User user, String accountNumber) {
        return user.getAccountNumbers().contains(accountNumber);
    }

    /**
     * Fetches a single account that the user owns, combining ownership check
     * and retrieval in one step. Used by ATMController when it needs the
     * Account object itself (e.g. to display the balance).
     *
     * @param user          the logged-in user
     * @param accountNumber the account to retrieve
     * @return the Account object
     * @throws SecurityException        if the user doesn't own this account
     * @throws AccountNotFoundException if the account doesn't exist in the repository
     */
    public Account getOwnedAccount(User user, String accountNumber)
            throws AccountNotFoundException {

        // Security check first — always before any data access
        checkOwnership(user, accountNumber);

        return accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }
}