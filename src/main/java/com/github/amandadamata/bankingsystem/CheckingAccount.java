package com.github.amandadamata.bankingsystem;

import java.math.BigDecimal;
import java.time.Clock;

public final class CheckingAccount extends Account {

    private final BigDecimal overdraftLimit;

    CheckingAccount(int number, Customer holder, BigDecimal overdraftLimit, Clock clock) {
        super(number, holder, clock);
        this.overdraftLimit = Money.validateNonNegativeAmount(overdraftLimit,
                "The overdraft limit must be zero or greater.");
    }

    /** The overdraft lets the holder spend beyond the balance, up to the limit, leaving it negative. */
    @Override
    protected BigDecimal calculateAvailableBalance() {
        return getBalance().add(overdraftLimit);
    }

    @Override
    StoredAccount toStored() {
        return new StoredAccount(getNumber(), AccountType.CHECKING, getHolder().getName(),
                getHolder().getCpf().digits(), overdraftLimit, getBalance(), null, getHistory());
    }

    @Override
    public String getAccountType() {
        return "Checking Account";
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
}
