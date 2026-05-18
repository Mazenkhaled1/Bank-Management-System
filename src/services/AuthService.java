package services;

import exceptions.AccountNotFoundException;
import exceptions.InvalidPinException;
import models.User;
import enums.UserRole;
import repository.UserRepository;
import utils.PinHasher;

import java.util.UUID;

/**
 * AuthService — handles all user authentication operations.
 *
 * Responsibilities:
 *   - Register new users
 *   - Authenticate existing users (login)
 *   - Change PIN
 *   - Unlock locked accounts (admin operation)
 *
 * OOP Concepts demonstrated:
 *   - Encapsulation: all auth logic is in one place, hidden from controllers
 *   - Dependency Injection: UserRepository and PinHasher are passed in (not created here)
 *   - Single Responsibility: this class ONLY handles authentication
 *
 * Design note:
 *   This service never touches Account objects — it only deals with Users.
 *   Account creation is AdminService's job. This keeps each service focused.
 */
public class AuthService {

    private final UserRepository userRepository;

    // ── Constructor (Dependency Injection) ────────────────────────────────────
    // We receive the dependencies we need instead of creating them.
    // This makes the class easier to test and change.
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ── Register ──────────────────────────────────────────────────────────────

    /**
     * Creates a new user account in the system.
     *
     * Flow:
     *   1. Validate inputs are not blank
     *   2. Check the email is not already registered
     *   3. Hash the raw PIN — never store the raw PIN
     *   4. Create the User object with a generated UUID
     *   5. Save to repository (which persists to disk)
     *   6. Return the new User
     *
     * @param name   the user's full name
     * @param email  must be unique across all users
     * @param rawPin the plain-text PIN (will be hashed before storage)
     * @param role   CUSTOMER or ADMIN
     * @return the newly created User
     * @throws IllegalArgumentException if inputs are invalid or email already taken
     */
    public User register(String name, String email, String rawPin, UserRole role) {

        // Step 1 — Validate inputs
        validateNotBlank(name, "Name");
        validateNotBlank(email, "Email");
        validatePin(rawPin);

        // Step 2 — Check email is not already taken
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An account with email '" + email + "' already exists.");
        }

        // Step 3 — Hash the PIN (raw PIN is discarded after this point)
        String hashedPin = PinHasher.hash(rawPin);

        // Step 4 — Create User with a unique ID
        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        User newUser = new User(userId, name, email, hashedPin, role);

        // Step 5 — Persist
        userRepository.save(newUser);

        System.out.printf("  ✓ User registered successfully. ID: %s%n", userId);

        // Step 6 — Return for immediate use (e.g. auto-login after register)
        return newUser;
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Authenticates a user by email and PIN.
     *
     * Flow:
     *   1. Look up user by email (throws if not found)
     *   2. Check if account is locked (throws if locked)
     *   3. Hash the input PIN and compare to stored hash
     *   4. On mismatch: increment failedAttempts, throw InvalidPinException
     *   5. On match: return the authenticated User
     *
     * The User model handles the lockout logic internally (verifyPin increments
     * attempts and locks at 3 failures). AuthService just reads the result.
     *
     * @param email  the user's registered email
     * @param rawPin the plain-text PIN entered by the user
     * @return the authenticated User object
     * @throws AccountNotFoundException if no user with that email exists
     * @throws IllegalStateException    if the account is locked
     * @throws InvalidPinException      if the PIN is wrong (includes attempts remaining)
     */
    public User login(String email, String rawPin)
            throws AccountNotFoundException, InvalidPinException {

        // Step 1 — Find user by email
        // Optional.orElseThrow() cleanly handles the "not found" case
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccountNotFoundException("No account found for email: " + email));

        // Step 2 — Check if locked BEFORE trying the PIN
        // (We don't want to even attempt verification if locked)
        if (user.isLocked()) {
            throw new IllegalStateException(
                    "Account is locked after too many failed attempts. Contact an admin to unlock.");
        }

        // Step 3 — Hash the input and verify
        String hashedAttempt = PinHasher.hash(rawPin);
        boolean pinCorrect = user.verifyPin(hashedAttempt);

        // Step 4 — Handle wrong PIN
        if (!pinCorrect) {
            // Save the updated failedAttempts count to disk
            userRepository.save(user);

            // If the account just got locked, show a specific message
            if (user.isLocked()) {
                throw new InvalidPinException(0); // 0 attempts remaining
            }
            throw new InvalidPinException(user.getAttemptsLeft());
        }

        // Step 5 — Successful login — save the reset failedAttempts (back to 0)
        userRepository.save(user);

        System.out.printf("  ✓ Welcome back, %s!%n", user.getName());
        return user;
    }

    // ── Change PIN ────────────────────────────────────────────────────────────

    /**
     * Changes a user's PIN after verifying the old one.
     *
     * @param userId    the ID of the user changing their PIN
     * @param oldRawPin the current PIN (for verification)
     * @param newRawPin the new PIN to set
     * @throws AccountNotFoundException if user not found
     * @throws InvalidPinException      if old PIN is incorrect
     * @throws IllegalArgumentException if new PIN is invalid
     */
    public void changePin(String userId, String oldRawPin, String newRawPin)
            throws AccountNotFoundException, InvalidPinException {

        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + userId));

        // Verify old PIN first
        String hashedOld = PinHasher.hash(oldRawPin);
        if (!user.verifyPin(hashedOld)) {
            userRepository.save(user); // save the failed attempt increment
            throw new InvalidPinException(user.getAttemptsLeft());
        }

        // Validate new PIN
        validatePin(newRawPin);

        // Apply the change
        String hashedNew = PinHasher.hash(newRawPin);
        user.changePin(hashedNew);
        userRepository.save(user);

        System.out.println("  ✓ PIN changed successfully.");
    }

    // ── Unlock Account (Admin Operation) ─────────────────────────────────────

    /**
     * Unlocks a user account that was locked due to too many failed PIN attempts.
     * This is an admin-only operation — the calling controller is responsible
     * for ensuring only admins can invoke this.
     *
     * @param userId the ID of the user to unlock
     * @throws AccountNotFoundException if user not found
     */
    public void unlockUser(String userId) throws AccountNotFoundException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("User not found: " + userId));

        if (!user.isLocked()) {
            System.out.println("  → Account is not locked. No action needed.");
            return;
        }

        user.unlock();
        userRepository.save(user);

        System.out.printf("  ✓ Account for %s (%s) has been unlocked.%n",
                user.getName(), userId);
    }

    // ── Private Validation Helpers ────────────────────────────────────────────

    private void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }

    private void validatePin(String rawPin) {
        if (rawPin == null || rawPin.length() < 4) {
            throw new IllegalArgumentException("PIN must be at least 4 digits.");
        }
        if (!rawPin.matches("\\d+")) {
            throw new IllegalArgumentException("PIN must contain digits only.");
        }
    }
}