package com.github.amandadamata.bankingsystem;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Entry point for banking operations.
 *
 * <p>The bank opens accounts, controls the numbering and runs deposits, withdrawals, transfers
 * and interest. Every operation follows the same order:
 * <ol>
 *   <li>keep the state of the accounts involved;</li>
 *   <li>apply the business rule in memory;</li>
 *   <li>save all the accounts involved in a single repository operation;</li>
 *   <li>if saving fails, put the accounts back in the kept state.</li>
 * </ol>
 * This way, memory and storage never diverge, and a transfer is never saved halfway.
 */
public final class Bank {

    static final int FIRST_ACCOUNT_NUMBER = 1000;

    private final Map<Integer, Account> accounts = new LinkedHashMap<>();
    private final Clock clock;
    private final BankRepository repository;
    private int nextNumber = FIRST_ACCOUNT_NUMBER;

    /** In-memory bank only: the data is lost when the program closes. */
    public Bank(Clock clock) {
        this(clock, BankRepository.withoutPersistence());
    }

    private Bank(Clock clock, BankRepository repository) {
        this.clock = Objects.requireNonNull(clock, "The clock is required.");
        this.repository = Objects.requireNonNull(repository, "The repository is required.");
    }

    /**
     * Creates a bank with the accounts already saved in the repository.
     *
     * @throws PersistenceFailureException if the data cannot be read or is inconsistent
     */
    public static Bank load(Clock clock, BankRepository repository) {
        Bank bank = new Bank(clock, repository);
        for (StoredAccount stored : repository.load()) {
            Account account = bank.restore(stored);
            if (bank.accounts.putIfAbsent(account.getNumber(), account) != null) {
                throw new PersistenceFailureException("Duplicate account number: " + account.getNumber());
            }
            bank.nextNumber = Math.max(bank.nextNumber, account.getNumber() + 1);
        }
        return bank;
    }

    public CheckingAccount openCheckingAccount(Customer holder, BigDecimal overdraftLimit) {
        ensureCpfBelongsToTheSameHolder(holder);
        return registerNewAccount(new CheckingAccount(nextNumber, holder, overdraftLimit, clock));
    }

    public SavingsAccount openSavingsAccount(Customer holder) {
        ensureCpfBelongsToTheSameHolder(holder);
        return registerNewAccount(new SavingsAccount(nextNumber, holder, clock));
    }

    public void deposit(int accountNumber, BigDecimal amount) {
        Account account = accountRequired(accountNumber);
        List<AccountState> before = captureStates(List.of(account));
        account.deposit(amount);
        saveOrUndo(List.of(account), before);
    }

    public void withdraw(int accountNumber, BigDecimal amount) throws InsufficientFundsException {
        Account account = accountRequired(accountNumber);
        List<AccountState> before = captureStates(List.of(account));
        account.withdraw(amount);
        saveOrUndo(List.of(account), before);
    }

    /** Saves source and destination together: either both sides are saved, or neither is. */
    public void transfer(int sourceNumber, int destinationNumber, BigDecimal amount)
            throws InsufficientFundsException {
        Account source = accountRequired(sourceNumber);
        Account destination = accountRequired(destinationNumber);
        if (source == destination) {
            throw new IllegalArgumentException("You can't transfer to the same account.");
        }
        List<Account> involved = List.of(source, destination);
        List<AccountState> before = captureStates(involved);
        source.transfer(destination, amount);
        saveOrUndo(involved, before);
    }

    /**
     * @return the amount credited (zero if the balance is too small)
     * @throws IllegalArgumentException if the account is not a savings account
     * @throws IllegalStateException if this month's interest was already applied
     */
    public BigDecimal applyInterest(int accountNumber) {
        if (!(accountRequired(accountNumber) instanceof SavingsAccount savings)) {
            throw new IllegalArgumentException("This operation is only available for savings accounts.");
        }
        List<AccountState> before = captureStates(List.of(savings));
        BigDecimal interest = savings.applyInterest();
        saveOrUndo(List.of(savings), before);
        return interest;
    }

    public Optional<Account> findAccountByNumber(int number) {
        return Optional.ofNullable(accounts.get(number));
    }

    /** Accounts in opening order. The returned list cannot be changed. */
    public List<Account> listAccounts() {
        return List.copyOf(accounts.values());
    }

    private static List<AccountState> captureStates(List<Account> involved) {
        List<AccountState> states = new ArrayList<>();
        for (Account account : involved) {
            states.add(account.captureState());
        }
        return states;
    }

    /**
     * Saves the changed accounts. If saving fails, the repository saved nothing, so the
     * accounts go back to their previous state and memory stays the same as storage.
     *
     * <p>If a business rule rejects the operation (for example, insufficient funds), this
     * method is never called: the rejection happens before anything changes.
     */
    private void saveOrUndo(List<Account> involved, List<AccountState> previousStates) {
        try {
            repository.save(involved.stream().map(Account::toStored).toList());
        } catch (RuntimeException failure) {
            for (int i = 0; i < involved.size(); i++) {
                involved.get(i).restoreState(previousStates.get(i));
            }
            throw new PersistenceFailureException(
                    "Could not save the operation; it was undone and nothing changed.", failure);
        }
    }

    /**
     * The number is only used once the account has been created and saved; a rejected
     * opening leaves no gaps in the numbering.
     */
    private <T extends Account> T registerNewAccount(T account) {
        try {
            repository.save(List.of(account.toStored()));
        } catch (RuntimeException failure) {
            throw new PersistenceFailureException(
                    "Could not save the new account; it was not created.", failure);
        }
        accounts.put(account.getNumber(), account);
        nextNumber++;
        return account;
    }

    /** A CPF identifies a single holder: it cannot appear with two different names. */
    private void ensureCpfBelongsToTheSameHolder(Customer holder) {
        Objects.requireNonNull(holder, "The account holder is required.");
        for (Account account : accounts.values()) {
            Customer existing = account.getHolder();
            if (existing.getCpf().equals(holder.getCpf())
                    && !existing.getName().equalsIgnoreCase(holder.getName())) {
                throw new IllegalArgumentException(
                        "This CPF is already registered to another holder.");
            }
        }
    }

    private Account accountRequired(int number) {
        return findAccountByNumber(number)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + number));
    }

    /** Rebuilds a saved account and checks that the data makes sense. */
    private Account restore(StoredAccount stored) {
        try {
            Customer holder = new Customer(stored.holderName(), stored.holderCpf());
            Account account = switch (stored.type()) {
                case CHECKING -> new CheckingAccount(
                        stored.number(), holder, stored.overdraftLimit(), clock);
                case SAVINGS -> {
                    SavingsAccount savings = new SavingsAccount(stored.number(), holder, clock);
                    savings.restoreLastInterestMonth(stored.lastInterestMonth());
                    yield savings;
                }
            };
            account.restoreHistory(stored.history(), stored.balance());
            return account;
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new PersistenceFailureException("Invalid data in account " + stored.number()
                    + ": " + invalid.getMessage(), invalid);
        }
    }
}
