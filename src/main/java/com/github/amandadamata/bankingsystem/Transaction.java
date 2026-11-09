package com.github.amandadamata.bankingsystem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Immutable statement entry.
 *
 * @param type             kind of transaction
 * @param amount           always positive; the sign comes from the type
 * @param resultingBalance account balance right after the transaction
 * @param dateTime         moment the transaction was recorded
 * @param relatedAccount   number of the other account in transfers, when there is one
 */
public record Transaction(
        TransactionType type,
        BigDecimal amount,
        BigDecimal resultingBalance,
        LocalDateTime dateTime,
        OptionalInt relatedAccount) {

    public Transaction {
        Objects.requireNonNull(type, "The type is required.");
        Objects.requireNonNull(amount, "The amount is required.");
        Objects.requireNonNull(resultingBalance, "The resulting balance is required.");
        Objects.requireNonNull(dateTime, "The date and time are required.");
        Objects.requireNonNull(relatedAccount, "Use OptionalInt.empty() when there is no other account.");
    }

    /** Signed amount: positive for credits and negative for debits. */
    public BigDecimal signedAmount() {
        return type.isCredit() ? amount : amount.negate();
    }
}
