package com.github.amandadamata.bankingsystem;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * An account's data in the format saved and read by {@link BankRepository}.
 *
 * <p>It is a plain snapshot with no behavior: the repository only copies this data to storage
 * and back. {@link Bank} is the one that turns this record into an {@link Account} (and checks
 * that the history is consistent).
 *
 * @param overdraftLimit    required for checking accounts; {@code null} for savings
 * @param lastInterestMonth savings only; {@code null} if it never earned interest
 */
public record StoredAccount(
        int number,
        AccountType type,
        String holderName,
        String holderCpf,
        BigDecimal overdraftLimit,
        BigDecimal balance,
        YearMonth lastInterestMonth,
        List<Transaction> history) {

    public StoredAccount {
        Objects.requireNonNull(type, "The account type is required.");
        Objects.requireNonNull(balance, "The balance is required.");
        history = List.copyOf(Objects.requireNonNull(history, "The history is required."));
    }
}
