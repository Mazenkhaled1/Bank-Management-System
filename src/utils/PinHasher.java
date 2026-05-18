package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing PINs using SHA-256.
 * Raw PINs are NEVER stored — only the hash is saved.
 *
 * OOP Concept: Utility class with only static methods.
 *              Constructor is private — you never instantiate this class.
 */
public final class PinHasher {

    // Private constructor — prevents anyone from doing: new PinHasher()
    private PinHasher() {}

    /**
     * Hashes a raw PIN string using SHA-256.
     * The same input always produces the same output (deterministic).
     *
     * @param rawPin the plain-text PIN entered by the user
     * @return a 64-character hexadecimal hash string
     */
    public static String hash(String rawPin) {
        try {
            // MessageDigest is Java's built-in cryptographic hashing class
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Convert the PIN string to bytes, then hash those bytes
            byte[] hashBytes = digest.digest(rawPin.getBytes());

            // Convert the byte array to a readable hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                // Each byte becomes 2 hex characters (e.g. 255 → "ff")
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0'); // pad single digits
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to exist in all Java implementations
            // so this exception will never actually be thrown
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies a raw PIN against a stored hash.
     *
     * @param rawPin      the plain-text PIN to check
     * @param storedHash  the previously stored hash
     * @return true if the PIN matches the hash
     */
    public static boolean verify(String rawPin, String storedHash) {
        return hash(rawPin).equals(storedHash);
    }
}