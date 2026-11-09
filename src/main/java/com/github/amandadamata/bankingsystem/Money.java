package com.github.amandadamata.bankingsystem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Shared money rules: amounts in Brazilian reais with exactly two decimal places.
 *
 * <p>{@link BigDecimal} stores decimal values exactly, unlike {@code double}, which uses
 * binary fractions and builds up errors such as {@code 0.1 + 0.2 = 0.30000000000000004}.
 */
public final class Money {

    /** Number of decimal places used for cents. */
    public static final int SCALE = 2;

    /** Banker's rounding: on a tie (x.xx5), round to the even cent. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private static final Pattern INPUT_FORMAT = Pattern.compile("\\d+([.,]\\d{1,2})?");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Money() {
    }

    /**
     * Converts text typed by the user into a money amount.
     *
     * <p>Accepts a comma or a dot as the decimal separator ("100,50" or "100.50") and up to two
     * decimal places. Thousands separators are not accepted because they would make "1.234"
     * ambiguous.
     *
     * @throws IllegalArgumentException when the text is not in an accepted format
     */
    public static BigDecimal parse(String text) {
        String amount = text == null ? "" : text.trim();
        if (!INPUT_FORMAT.matcher(amount).matches()) {
            throw new IllegalArgumentException(
                    "Invalid amount. Use numbers with up to two decimal places (e.g. 100.50).");
        }
        return new BigDecimal(amount.replace(',', '.')).setScale(SCALE);
    }

    /** Formats an amount in Brazilian style, for example {@code R$ 1.234,56}. */
    public static String format(BigDecimal amount) {
        DecimalFormat formatter = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR));
        formatter.setRoundingMode(ROUNDING);
        return "R$ " + formatter.format(Objects.requireNonNull(amount, "The amount is required."));
    }

    /**
     * Validates a positive money amount with at most two decimal places.
     *
     * <p>Amounts with more places are rejected instead of silently rounded: an operation of
     * R$ 10,005 must not become R$ 10,00 or R$ 10,01 without the user knowing.
     */
    static BigDecimal validatePositiveAmount(BigDecimal amount, String message) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(message);
        }
        return validateScale(amount);
    }

    static BigDecimal validateNonNegativeAmount(BigDecimal amount, String message) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException(message);
        }
        return validateScale(amount);
    }

    private static BigDecimal validateScale(BigDecimal amount) {
        BigDecimal withoutExtraZeros = amount.stripTrailingZeros();
        if (withoutExtraZeros.scale() > SCALE) {
            throw new IllegalArgumentException("The amount can have at most two decimal places.");
        }
        return amount.setScale(SCALE);
    }
}
