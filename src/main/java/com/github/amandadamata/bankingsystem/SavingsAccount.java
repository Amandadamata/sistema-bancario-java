package com.github.amandadamata.bankingsystem;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Savings account with simplified monthly interest.
 *
 * <p>Teaching rule: 0.5% of the current balance, at most once per calendar month, with banker's
 * rounding to cents. It does not reproduce the official Brazilian savings rule (anniversary
 * date, TR and Selic).
 */
public final class SavingsAccount extends Account {

    static final BigDecimal MONTHLY_INTEREST_RATE = new BigDecimal("0.005");

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MM/yyyy");

    private YearMonth lastInterestMonth;

    SavingsAccount(int number, Customer holder, Clock clock) {
        super(number, holder, clock);
    }

    /**
     * Credits this month's interest.
     *
     * @return the amount credited; zero when the balance is too small to earn a cent
     * @throws IllegalStateException when this month's interest was already applied
     */
    BigDecimal applyInterest() {
        YearMonth currentMonth = YearMonth.now(getClock());
        if (currentMonth.equals(lastInterestMonth)) {
            throw new IllegalStateException("Interest for " + currentMonth.format(MONTH_FORMAT)
                    + " has already been applied to this account.");
        }

        BigDecimal interest = getBalance()
                .multiply(MONTHLY_INTEREST_RATE)
                .setScale(Money.SCALE, Money.ROUNDING);
        if (interest.signum() > 0) {
            addCredit(interest, TransactionType.INTEREST);
            lastInterestMonth = currentMonth;
        }
        return interest;
    }

    /** The month of the last interest payment is also part of the state that must be undone. */
    @Override
    AccountState captureState() {
        AccountState base = super.captureState();
        return new AccountState(base.balance(), base.historySize(), lastInterestMonth);
    }

    @Override
    void restoreState(AccountState state) {
        super.restoreState(state);
        lastInterestMonth = state.lastInterestMonth();
    }

    void restoreLastInterestMonth(YearMonth month) {
        lastInterestMonth = month;
    }

    @Override
    StoredAccount toStored() {
        return new StoredAccount(getNumber(), AccountType.SAVINGS, getHolder().getName(),
                getHolder().getCpf().digits(), null, getBalance(), lastInterestMonth, getHistory());
    }

    @Override
    public String getAccountType() {
        return "Savings Account";
    }
}
