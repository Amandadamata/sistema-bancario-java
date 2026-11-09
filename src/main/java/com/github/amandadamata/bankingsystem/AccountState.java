package com.github.amandadamata.bankingsystem;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Copy of an account's mutable state, taken before an operation.
 *
 * <p>If saving fails, {@link Bank} puts the account back in this state, so memory stays
 * the same as what is stored (the <i>memento</i> pattern).
 */
record AccountState(BigDecimal balance, int historySize, YearMonth lastInterestMonth) {
}
