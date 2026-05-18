package services;

import enums.AccountType;
import exceptions.AccountNotFoundException;
import interfaces.InterestBearing;
import models.Account;
import models.LoanAccount;
import models.User;
import patterns.AccountFactory;
import patterns.BankEventListener;
import repository.AccountRepository;
import repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdminService — provides all administrative operations on accounts and users.
 *
 * Responsibilities:
 *   - Create new bank accounts (using AccountFactory)
 *   - Freeze and unfreeze accounts
 *   - Delete accounts
 *   - Apply interest to all eligible accounts
 *   - Generate bank-wide reports
 *   - List all users and accounts
 *
 * OOP Concepts demonstrated:
 *   - Factory Pattern: account creation delegated to AccountFactory
 *   - Polymorphism: applyInterest() works on any InterestBearing account
 *   - instanceof + casting: safely detecting InterestBearing accounts
 *   - Streams: filtering, grouping, sorting, and summing collections
 *   - Observer Pattern: freeze events fired to BankEventListener
 *
 * Design note:
 *   AdminService never prints menus or reads input. That is AdminController's job.
 *   This service only executes business logic and returns/prints results.
 */
public class AdminService {

    private final AccountRepository accountRepository;
    private final UserRepository    userRepository;
    private final BankEventListener eventListener;

    // ── Constructor ───────────────────────────────────────────────────────────
    public AdminService(AccountRepository accountRepository,
                        UserRepository userRepository,
                        BankEventListener eventListener) {
        this.accountRepository = accountRepository;
        this.userRepository    = userRepository;
        this.eventListener     = eventListener;
    }

    // ── Create Account ────────────────────────────────────────────────────────

    /**
     * Creates a new SAVINGS or CHECKING account and links it to a user.
     *
     * Flow:
     *   1. Find the user who will own this account
     *   2. Call AccountFactory to create the correct Account subclass
     *   3. Link the account number to the user's list
     *   4. Save both the account and the updated user
     *
     * Factory Pattern:
     *   AccountFactory decides which subclass to instantiate based on AccountType.
     *   AdminService does NOT call new SavingsAccount() directly — that would
     *   create tight coupling. If we add a new account type, only AccountFactory changes.
     *
     * @param type           SAVINGS or CHECKING (use createLoanAccount for loans)
     * @param ownerName      the display name for the account
     * @param initialBalance the opening balance
     * @param userId         the ID of the user who will own this account
     * @return the newly created Account
     * @throws AccountNotFoundException if no user with userId exists
     */
    public Account createAccount(AccountType type, String ownerName,
                                 double initialBalance, String userId)
            throws AccountNotFoundException {

        if (type == AccountType.LOAN) {
            throw new IllegalArgumentException(
                    "Use createLoanAccount() for loan accounts — they require extra parameters.");
        }

        // Step 1 — Find the owner
        User owner = findUserOrThrow(userId);

        // Step 2 — Delegate creation to the Factory (Factory Pattern)
        Account account = AccountFactory.createAccount(type, ownerName, initialBalance);

        // Step 3 — Link: tell the user they own this account
        owner.addAccount(account.getAccountNumber());

        // Step 4 — Persist both
        accountRepository.save(account);
        userRepository.save(owner);

        System.out.printf("  ✓ Account created: %s (owner: %s)%n",
                account.getAccountNumber(), ownerName);
        return account;
    }

    /**
     * Creates a new LOAN account and links it to a user.
     *
     * Loans need extra parameters that normal accounts don't have,
     * so they get their own creation method.
     *
     * @param ownerName    display name for the account
     * @param loanAmount   the principal amount of the loan
     * @param interestRate annual interest rate as a decimal (e.g. 0.08 for 8%)
     * @param termMonths   loan term in months
     * @param userId       the ID of the user who will own this loan
     * @return the newly created LoanAccount
     * @throws AccountNotFoundException if user not found
     */
    public LoanAccount createLoanAccount(String ownerName, double loanAmount,
                                         double interestRate, int termMonths, String userId)
            throws AccountNotFoundException {

        User owner = findUserOrThrow(userId);

        // AccountFactory has a separate method for loans
        LoanAccount loan = AccountFactory.createLoanAccount(ownerName, loanAmount, interestRate, termMonths);

        owner.addAccount(loan.getAccountNumber());

        accountRepository.save(loan);
        userRepository.save(owner);

        System.out.printf("  ✓ Loan account created: %s | Amount: $%.2f | Rate: %.1f%% | Term: %d months%n",
                loan.getAccountNumber(), loanAmount, interestRate * 100, termMonths);
        System.out.printf("  Monthly installment: $%.2f%n", loan.getMonthlyInstallment());

        return loan;
    }

    // ── Freeze / Unfreeze ─────────────────────────────────────────────────────

    /**
     * Freezes an account — all deposit and withdrawal operations on it will
     * throw AccountFrozenException until it is unfrozen.
     *
     * Observer Pattern:
     *   After freezing, we notify the event listener so the notification
     *   service can alert the relevant parties.
     *
     * @throws AccountNotFoundException if account doesn't exist
     */
    public void freezeAccount(String accountNumber) throws AccountNotFoundException {
        Account account = findAccountOrThrow(accountNumber);

        if (account.isFrozen()) {
            System.out.println("  → Account is already frozen.");
            return;
        }

        account.freeze();
        accountRepository.save(account);

        // Fire observer event (Observer Pattern)
        eventListener.onAccountFrozen(accountNumber);

        System.out.printf("  ✓ Account %s has been frozen.%n", accountNumber);
    }

    /**
     * Unfreezes a previously frozen account.
     *
     * @throws AccountNotFoundException if account doesn't exist
     */
    public void unfreezeAccount(String accountNumber) throws AccountNotFoundException {
        Account account = findAccountOrThrow(accountNumber);

        if (!account.isFrozen()) {
            System.out.println("  → Account is not frozen. No action needed.");
            return;
        }

        account.unfreeze();
        accountRepository.save(account);

        System.out.printf("  ✓ Account %s has been unfrozen.%n", accountNumber);
    }

    // ── Delete Account ────────────────────────────────────────────────────────

    /**
     * Permanently deletes an account and removes it from the owner's list.
     *
     * @param accountNumber the account to delete
     * @param userId        the ID of the user who owns this account
     * @throws AccountNotFoundException if account or user not found
     */
    public void deleteAccount(String accountNumber, String userId)
            throws AccountNotFoundException {

        // Verify account exists
        findAccountOrThrow(accountNumber);

        // Find the owner and remove the link
        User owner = findUserOrThrow(userId);
        owner.removeAccount(accountNumber);

        // Delete from repository (removes from HashMap and persists)
        accountRepository.delete(accountNumber);
        userRepository.save(owner); // save the updated account list

        System.out.printf("  ✓ Account %s deleted and unlinked from user %s.%n",
                accountNumber, userId);
    }

    // ── Apply Interest ────────────────────────────────────────────────────────

    /**
     * Applies monthly interest to every account that implements InterestBearing.
     *
     * This method perfectly demonstrates two OOP concepts working together:
     *
     *   1. Polymorphism via instanceof:
     *      We loop through ALL accounts (Account objects). We check which ones
     *      are InterestBearing (SavingsAccount and LoanAccount are; CheckingAccount is not).
     *
     *   2. Polymorphism via interface:
     *      Once we cast to InterestBearing, we call applyInterest().
     *      For SavingsAccount this ADDS interest to balance.
     *      For LoanAccount this ADDS interest to outstandingBalance.
     *      Same method call, completely different behavior.
     *
     * Streams usage:
     *   - filter: keep only InterestBearing accounts
     *   - map: cast Account → InterestBearing safely
     *   - forEach: apply interest and save
     */
    public void applyMonthlyInterest() {
        List<Account> allAccounts = accountRepository.findAll();

        // Stream pipeline:
        // 1. filter: only keep accounts that implement InterestBearing
        // 2. map: cast each to InterestBearing (safe because we just filtered)
        // 3. forEach: call applyInterest() on each (polymorphic call)
        long count = allAccounts.stream()
                .filter(account -> account instanceof InterestBearing)
                .map(account -> (InterestBearing) account)
                .peek(ib -> {
                    // peek lets us call applyInterest() AND save the account in one stream
                    ib.applyInterest();
                    accountRepository.save((Account) ib); // save the updated balance
                })
                .count(); // triggers the stream (streams are lazy — nothing runs until a terminal op)

        System.out.printf("  ✓ Monthly interest applied to %d account(s).%n", count);
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    /**
     * Generates and prints a comprehensive bank-wide report.
     *
     * Uses Java Streams extensively:
     *   - mapToDouble + sum()      → total assets
     *   - Collectors.groupingBy()  → accounts grouped by type
     *   - Collectors.counting()    → count per group
     *   - filter + count()         → frozen account count
     *   - sorted + limit()         → top 5 accounts by balance
     */
    public void generateReport() {
        List<Account> allAccounts = accountRepository.findAll();
        List<User> allUsers       = userRepository.findAll();

        if (allAccounts.isEmpty()) {
            System.out.println("  No accounts found in the system.");
            return;
        }

        System.out.println("\n" + "=".repeat(65));
        System.out.println("                    BANK REPORT");
        System.out.println("=".repeat(65));

        // ── Total assets: sum all balances using mapToDouble ──────────────────
        double totalAssets = allAccounts.stream()
                .mapToDouble(Account::getBalance)
                .sum();
        System.out.printf("  Total Assets:        $%,.2f%n", totalAssets);
        System.out.printf("  Total Accounts:      %d%n", allAccounts.size());
        System.out.printf("  Total Users:         %d%n", allUsers.size());

        // ── Accounts by type: group then count ────────────────────────────────
        // Collectors.groupingBy groups accounts by their AccountType enum value
        // Collectors.counting() counts how many are in each group
        Map<String, Long> byType = allAccounts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getAccountType().getDisplayName(), // key: type name
                        Collectors.counting()                      // value: count
                ));
        System.out.println("\n  Accounts by Type:");
        byType.forEach((type, count) ->
                System.out.printf("    %-20s %d%n", type + ":", count));

        // ── Frozen account count ──────────────────────────────────────────────
        long frozenCount = allAccounts.stream()
                .filter(Account::isFrozen)
                .count();
        System.out.printf("%n  Frozen Accounts:     %d%n", frozenCount);

        // ── Top 5 accounts by balance ─────────────────────────────────────────
        // sorted() with Comparator.comparingDouble().reversed() = descending order
        System.out.println("\n  Top 5 Accounts by Balance:");
        allAccounts.stream()
                .sorted(Comparator.comparingDouble(Account::getBalance).reversed())
                .limit(5)
                .forEach(a -> System.out.printf(
                        "    %-20s %-20s $%,.2f%n",
                        a.getAccountNumber(), a.getOwnerName(), a.getBalance()));

        // ── Frozen accounts list ──────────────────────────────────────────────
        if (frozenCount > 0) {
            System.out.println("\n  Frozen Account List:");
            allAccounts.stream()
                    .filter(Account::isFrozen)
                    .forEach(a -> System.out.printf(
                            "    %s (%s)%n", a.getAccountNumber(), a.getOwnerName()));
        }

        System.out.println("=".repeat(65) + "\n");
    }

    /**
     * Lists all accounts in the system with a formatted summary.
     * Uses the polymorphic getAccountSummary() — each subclass formats its own line.
     */
    public void listAllAccounts() {
        List<Account> accounts = accountRepository.findAll();

        if (accounts.isEmpty()) {
            System.out.println("  No accounts found.");
            return;
        }

        System.out.println("\n" + "=".repeat(85));
        System.out.println("  ALL ACCOUNTS");
        System.out.println("=".repeat(85));

        // getAccountSummary() is abstract in Account — polymorphic call here
        accounts.stream()
                .sorted(Comparator.comparing(Account::getAccountNumber))
                .forEach(a -> System.out.println(a.getAccountSummary()));

        System.out.println("=".repeat(85) + "\n");
    }

    /**
     * Lists all registered users in the system.
     */
    public void listAllUsers() {
        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            System.out.println("  No users found.");
            return;
        }

        System.out.println("\n" + "=".repeat(65));
        System.out.println("  ALL USERS");
        System.out.println("=".repeat(65));
        System.out.printf("  %-12s %-20s %-25s %-8s %-6s%n",
                "USER ID", "NAME", "EMAIL", "ROLE", "LOCKED");
        System.out.println("-".repeat(65));

        // Stream: sort alphabetically by name, then print each
        users.stream()
                .sorted(Comparator.comparing(User::getName))
                .forEach(u -> System.out.printf(
                        "  %-12s %-20s %-25s %-8s %-6s%n",
                        u.getUserId(), u.getName(), u.getEmail(),
                        u.getRole(), u.isLocked() ? "YES" : "no"));

        System.out.println("=".repeat(65) + "\n");
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Account findAccountOrThrow(String accountNumber) throws AccountNotFoundException {
        return accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private User findUserOrThrow(String userId) throws AccountNotFoundException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + userId));
    }
}