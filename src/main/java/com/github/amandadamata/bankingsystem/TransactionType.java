package com.github.amandadamata.bankingsystem;

/** Kinds of transaction recorded in an account's history. */
public enum TransactionType {
    DEPOSIT("Deposit", true),
    WITHDRAWAL("Withdrawal", false),
    TRANSFER_SENT("Transfer sent", false),
    TRANSFER_RECEIVED("Transfer received", true),
    INTEREST("Interest", true);

    private final String description;
    private final boolean credit;

    TransactionType(String description, boolean credit) {
        this.description = description;
        this.credit = credit;
    }

    public String getDescription() {
        return description;
    }

    /** Whether the transaction increases the balance (credit) or reduces it (debit). */
    public boolean isCredit() {
        return credit;
    }
}
