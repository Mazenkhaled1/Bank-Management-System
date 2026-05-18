package utils;

import java.util.Scanner;


public class ConsoleHelper {

    private final Scanner scanner;

    public ConsoleHelper() {
        this.scanner = new Scanner(System.in);
    }

    // ── Input methods ──────────────────────────────────────────────────────────

    /**
     * Prints a prompt and reads a trimmed String from the user.
     */
    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /**
     * Reads an integer. Re-prompts if the user types something that isn't a number.
     */
    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                return value;
            } catch (NumberFormatException e) {
                System.out.println("  ✗ Please enter a valid number.");
            }
        }
    }

    /**
     * Reads a double. Re-prompts on invalid input.
     */
    public double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double value = Double.parseDouble(scanner.nextLine().trim());
                return value;
            } catch (NumberFormatException e) {
                System.out.println("  ✗ Please enter a valid amount.");
            }
        }
    }

    /**
     * Reads a PIN (treated as a plain String — masking handled by the terminal).
     * We don't echo the input, but Java console masking is limited.
     */
    public String readPin(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    // ── Display helpers ────────────────────────────────────────────────────────

    public void printBanner(String title) {
        int width = 60;
        String border = "=".repeat(width);
        int padding = (width - title.length() - 2) / 2;
        String padded = " ".repeat(padding) + title + " ".repeat(padding);
        System.out.println("\n" + border);
        System.out.println("=" + padded + "=");
        System.out.println(border + "\n");
    }

    public void printLine() {
        System.out.println("-".repeat(60));
    }

    public void printSuccess(String message) {
        System.out.println("\n  ✓ " + message + "\n");
    }

    public void printError(String message) {
        System.out.println("\n  ✗ " + message + "\n");
    }

    public void printInfo(String message) {
        System.out.println("  → " + message);
    }

    /**
     * Must be called once when the application exits to release the Scanner.
     */
    public void close() {
        scanner.close();
    }
}