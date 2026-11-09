package com.github.amandadamata.bankingsystem;

import java.util.Objects;

/**
 * Validated and normalized CPF (the Brazilian taxpayer ID), kept as its 11 digits.
 *
 * <p>The check is mathematical: it confirms the length and the two check digits. It does not
 * prove identity, ownership or registration status with the Brazilian tax authority.
 *
 * <p>For privacy, {@link #toString()} and the error messages never show the full number.
 */
public final class Cpf {

    private static final int SIZE = 11;

    private final String digits;

    private Cpf(String digits) {
        this.digits = digits;
    }

    /**
     * Validates and normalizes a CPF typed with or without punctuation ("246.813.579-28" or "24681357928", a synthetic CPF).
     *
     * @throws IllegalArgumentException with a message that does not repeat the number entered
     */
    public static Cpf of(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("The CPF is required.");
        }
        String withoutPunctuation = text.strip().replace(".", "").replace("-", "").replace(" ", "");
        if (!withoutPunctuation.chars().allMatch(c -> c >= '0' && c <= '9')) {
            throw new IllegalArgumentException("The CPF can only contain digits, dots and a hyphen.");
        }
        if (withoutPunctuation.length() != SIZE) {
            throw new IllegalArgumentException("The CPF must have 11 digits.");
        }
        if (withoutPunctuation.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("Invalid CPF: all digits are the same.");
        }
        if (checkDigit(withoutPunctuation, 9) != withoutPunctuation.charAt(9) - '0'
                || checkDigit(withoutPunctuation, 10) != withoutPunctuation.charAt(10) - '0') {
            throw new IllegalArgumentException("Invalid CPF: the check digits don't match.");
        }
        return new Cpf(withoutPunctuation);
    }

    /**
     * Calculates the check digit from the first {@code quantity} digits.
     *
     * <p>Each digit is multiplied by a decreasing weight, starting at {@code quantity + 1}.
     * If the remainder of the sum divided by 11 is less than 2, the digit is 0; otherwise it is
     * 11 minus the remainder.
     */
    static int checkDigit(String digits, int quantity) {
        int sum = 0;
        for (int i = 0; i < quantity; i++) {
            int weight = quantity + 1 - i;
            sum += (digits.charAt(i) - '0') * weight;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    /** The 11 digits without punctuation; this is the format stored in the database. */
    public String digits() {
        return digits;
    }

    /** Display format: 000.000.000-00. */
    public String formatted() {
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "."
                + digits.substring(6, 9) + "-" + digits.substring(9);
    }

    /** Only the last two digits stay visible: ***.***.***-00. */
    public String masked() {
        return "***.***.***-" + digits.substring(9);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Cpf cpf && digits.equals(cpf.digits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(digits);
    }

    @Override
    public String toString() {
        return masked();
    }
}
