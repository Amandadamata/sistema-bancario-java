package com.github.amandadamata.bankingsystem;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * State and operations shared by every bank account.
 *
 * <p>Operations that change the balance are not public: only {@link Bank} calls them, and it
 * also saves the result. This way, no transaction happens without being persisted.
 */
public abstract class Account {

    private final int number;
    private final Customer holder;
    private final Clock clock;
    private final List<Transaction> history = new ArrayList<>();
    private BigDecimal balance = BigDecimal.ZERO.setScale(Money.SCALE);

    /**
     * Accounts are opened by {@link Bank}, which controls the numbering. That is why the
     * constructor is not public.
     */
    protected Account(int number, Customer holder, Clock clock) {
        if (number <= 0) {
            throw new IllegalArgumentException("The account number must be positive.");
        }
        this.number = number;
        this.holder = Objects.requireNonNull(holder, "The account holder is required.");
        this.clock = Objects.requireNonNull(clock, "The clock is required.");
    }

    void deposit(BigDecimal amount) {
        BigDecimal validatedAmount = Money.validatePositiveAmount(
                amount, "The deposit amount must be greater than zero.");
        register(TransactionType.DEPOSIT, validatedAmount, OptionalInt.empty());
    }

    void withdraw(BigDecimal amount) throws InsufficientFundsException {
        BigDecimal validatedAmount = Money.validatePositiveAmount(
                amount, "The withdrawal amount must be greater than zero.");
        ensureAvailableBalance(validatedAmount, "withdrawal");
        register(TransactionType.WITHDRAWAL, validatedAmount, OptionalInt.empty());
    }

    /**
     * Transfers an amount to another account.
     *
     * <p>All checks run before any balance changes, so a rejected transfer leaves both
     * accounts untouched.
     *
     * @throws InsufficientFundsException when the source account does not have enough available balance
     * @throws IllegalArgumentException when the destination or the amount is invalid
     */
    void transfer(Account destination, BigDecimal amount) throws InsufficientFundsException {
        if (destination == null) {
            throw new IllegalArgumentException("Invalid destination account.");
        }
        if (destination == this) {
            throw new IllegalArgumentException("You can't transfer to the same account.");
        }
        BigDecimal validatedAmount = Money.validatePositiveAmount(
                amount, "The transfer amount must be greater than zero.");
        ensureAvailableBalance(validatedAmount, "transfer");

        register(TransactionType.TRANSFER_SENT, validatedAmount, OptionalInt.of(destination.number));
        destination.register(TransactionType.TRANSFER_RECEIVED, validatedAmount, OptionalInt.of(number));
    }

    /** Lets subclasses record their own credits, such as savings interest. */
    protected final void addCredit(BigDecimal amount, TransactionType type) {
        if (!type.isCredit()) {
            throw new IllegalArgumentException("The given type is not a credit: " + type);
        }
        register(type, Money.validatePositiveAmount(amount, "The credit must be greater than zero."),
                OptionalInt.empty());
    }

    /** Balance that can be used for withdrawals and transfers. */
    protected BigDecimal calculateAvailableBalance() {
        return balance;
    }

    protected final Clock getClock() {
        return clock;
    }

    private void ensureAvailableBalance(BigDecimal amount, String operation)
            throws InsufficientFundsException {
        BigDecimal available = calculateAvailableBalance();
        if (amount.compareTo(available) > 0) {
            throw new InsufficientFundsException("Insufficient balance. Available for " + operation
                    + ": " + Money.format(available));
        }
    }

    private void register(TransactionType type, BigDecimal amount, OptionalInt relatedAccount) {
        balance = type.isCredit() ? balance.add(amount) : balance.subtract(amount);
        history.add(new Transaction(
                type, amount, balance, LocalDateTime.now(clock), relatedAccount));
    }

    /** Keeps the current state so the operation can be undone if saving fails. */
    AccountState captureState() {
        return new AccountState(balance, history.size(), null);
    }

    /** Returns to the captured state, discarding the transactions made after it. */
    void restoreState(AccountState state) {
        balance = state.balance();
        history.subList(state.historySize(), history.size()).clear();
    }

    /**
     * Restores a history read from storage, checking every entry.
     *
     * <p>The starting balance is zero; each entry must lead exactly to the balance recorded in
     * it, and the last balance must match the balance stored for the account. Any difference
     * means the data is corrupted or was changed outside the application.
     *
     * @throws PersistenceFailureException if the history is not consistent
     */
    void restoreHistory(List<Transaction> entries, BigDecimal storedBalance) {
        BigDecimal calculatedBalance = BigDecimal.ZERO.setScale(Money.SCALE);
        for (Transaction entry : entries) {
            calculatedBalance = calculatedBalance.add(entry.signedAmount());
            if (calculatedBalance.compareTo(entry.resultingBalance()) != 0) {
                throw new PersistenceFailureException("Inconsistent data in account " + number
                        + ": an entry doesn't match the recorded balance.");
            }
        }
        if (calculatedBalance.compareTo(storedBalance) != 0) {
            throw new PersistenceFailureException("Inconsistent data in account " + number
                    + ": the saved balance doesn't match the history.");
        }
        history.clear();
        history.addAll(entries);
        balance = calculatedBalance.setScale(Money.SCALE);
    }

    /** Converts the account to the format saved by the repository. */
    abstract StoredAccount toStored();

    public abstract String getAccountType();

    public int getNumber() {
        return number;
    }

    public Customer getHolder() {
        return holder;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public final BigDecimal getAvailableBalance() {
        return calculateAvailableBalance();
    }

    /** History in chronological order. The returned list cannot be changed. */
    public List<Transaction> getHistory() {
        return List.copyOf(history);
    }
}
