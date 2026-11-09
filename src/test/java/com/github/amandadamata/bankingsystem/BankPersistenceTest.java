package com.github.amandadamata.bankingsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bank rules related to saving, using an in-memory repository that can fail on purpose.
 * The tests with a real SQLite file are in {@code persistence.SqliteRepositoryTest}.
 */
class BankPersistenceTest {

    private static final Customer ANA = new Customer("Ana", "246.813.579-28");
    private static final Customer BRUNO = new Customer("Bruno", "135.792.468-28");

    private AdjustableClock clock;
    private ControlledRepository repository;
    private Bank bank;

    @BeforeEach
    void createBank() {
        clock = AdjustableClock.at("2026-10-20T13:00:00Z");
        repository = new ControlledRepository();
        bank = Bank.load(clock, repository);
    }

    @Test
    void eachOperationSavesTheChangedAccounts() throws InsufficientFundsException {
        CheckingAccount account = bank.openCheckingAccount(ANA, amount("100.00"));

        bank.deposit(account.getNumber(), amount("50.00"));
        bank.withdraw(account.getNumber(), amount("20.00"));

        StoredAccount savedAccount = repository.saved.get(account.getNumber());
        assertAll(
                () -> assertEquals(amount("30.00"), savedAccount.balance()),
                () -> assertEquals(2, savedAccount.history().size()),
                () -> assertEquals("24681357928", savedAccount.holderCpf()));
    }

    @Test
    void failedDepositSaveUndoesTheChangeInMemory() {
        SavingsAccount account = bank.openSavingsAccount(ANA);
        repository.failOnNextSave = true;

        assertThrows(PersistenceFailureException.class,
                () -> bank.deposit(account.getNumber(), amount("100.00")));

        assertAll(
                () -> assertEquals(amount("0.00"), account.getBalance()),
                () -> assertTrue(account.getHistory().isEmpty()),
                () -> assertEquals(amount("0.00"), repository.saved.get(account.getNumber()).balance()));
    }

    @Test
    void failedTransferSaveUndoesBothAccounts() throws InsufficientFundsException {
        SavingsAccount source = bank.openSavingsAccount(ANA);
        SavingsAccount destination = bank.openSavingsAccount(BRUNO);
        bank.deposit(source.getNumber(), amount("100.00"));
        repository.failOnNextSave = true;

        assertThrows(PersistenceFailureException.class,
                () -> bank.transfer(source.getNumber(), destination.getNumber(), amount("40.00")));

        assertAll(
                () -> assertEquals(amount("100.00"), source.getBalance()),
                () -> assertEquals(1, source.getHistory().size()),
                () -> assertEquals(amount("0.00"), destination.getBalance()),
                () -> assertTrue(destination.getHistory().isEmpty()));
    }

    @Test
    void transferSavesSourceAndDestinationInOneCall() throws InsufficientFundsException {
        SavingsAccount source = bank.openSavingsAccount(ANA);
        SavingsAccount destination = bank.openSavingsAccount(BRUNO);
        bank.deposit(source.getNumber(), amount("100.00"));
        repository.calls.clear();

        bank.transfer(source.getNumber(), destination.getNumber(), amount("40.00"));

        assertEquals(List.of(List.of(source.getNumber(), destination.getNumber())), repository.calls);
    }

    @Test
    void failedInterestSaveKeepsTheMonthAvailable() {
        SavingsAccount account = bank.openSavingsAccount(ANA);
        bank.deposit(account.getNumber(), amount("1000.00"));
        repository.failOnNextSave = true;

        assertThrows(PersistenceFailureException.class, () -> bank.applyInterest(account.getNumber()));
        BigDecimal interest = bank.applyInterest(account.getNumber());

        assertAll(
                () -> assertEquals(amount("5.00"), interest),
                () -> assertEquals(amount("1005.00"), account.getBalance()));
    }

    @Test
    void failedNewAccountSaveCreatesNoAccountAndKeepsTheNumber() {
        repository.failOnNextSave = true;

        assertThrows(PersistenceFailureException.class, () -> bank.openSavingsAccount(ANA));
        Account account = bank.openSavingsAccount(ANA);

        assertAll(
                () -> assertEquals(1000, account.getNumber()),
                () -> assertEquals(1, bank.listAccounts().size()));
    }

    @Test
    void operationRejectedByARuleIsNeverSaved() {
        SavingsAccount account = bank.openSavingsAccount(ANA);
        repository.calls.clear();

        assertThrows(InsufficientFundsException.class, () -> bank.withdraw(account.getNumber(), amount("0.01")));

        assertTrue(repository.calls.isEmpty());
    }

    @Test
    void operationOnAMissingAccountIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bank.deposit(9999, amount("10.00")));

        assertAll(
                () -> assertEquals("Account not found: 9999", error.getMessage()),
                () -> assertTrue(repository.calls.isEmpty()));
    }

    @Test
    void loadRestoresAccountsHistoryNumberingAndInterest() throws InsufficientFundsException {
        CheckingAccount checking = bank.openCheckingAccount(ANA, amount("200.00"));
        SavingsAccount savings = bank.openSavingsAccount(BRUNO);
        bank.deposit(savings.getNumber(), amount("1000.00"));
        bank.applyInterest(savings.getNumber());
        bank.transfer(checking.getNumber(), savings.getNumber(), amount("150.00"));

        Bank restarted = Bank.load(clock, repository);

        Account loadedChecking = restarted.findAccountByNumber(checking.getNumber()).orElseThrow();
        Account loadedSavings = restarted.findAccountByNumber(savings.getNumber()).orElseThrow();
        assertAll(
                () -> assertEquals(amount("-150.00"), loadedChecking.getBalance()),
                () -> assertEquals(amount("50.00"), loadedChecking.getAvailableBalance()),
                () -> assertEquals(checking.getHistory(), loadedChecking.getHistory()),
                () -> assertEquals(amount("1155.00"), loadedSavings.getBalance()),
                () -> assertEquals(savings.getHistory(), loadedSavings.getHistory()),
                () -> assertThrows(IllegalStateException.class,
                        () -> restarted.applyInterest(savings.getNumber())),
                () -> assertEquals(1002, restarted.openSavingsAccount(ANA).getNumber()));
    }

    @Test
    void loadRejectsABalanceThatDoesNotMatchTheHistory() {
        bank.openSavingsAccount(ANA);
        bank.deposit(1000, amount("10.00"));
        StoredAccount original = repository.saved.get(1000);
        repository.saved.put(1000, new StoredAccount(1000, AccountType.SAVINGS, "Ana", "24681357928",
                null, amount("999.00"), null, original.history()));

        PersistenceFailureException error = assertThrows(PersistenceFailureException.class,
                () -> Bank.load(clock, repository));

        assertTrue(error.getMessage().contains("account 1000"), error.getMessage());
    }

    @Test
    void loadRejectsAnInvalidCpf() {
        repository.saved.put(1000, new StoredAccount(1000, AccountType.SAVINGS, "Ana", "11111111111",
                null, amount("0.00"), null, List.of()));

        assertThrows(PersistenceFailureException.class, () -> Bank.load(clock, repository));
    }

    @Test
    void cpfCannotBelongToTwoHolders() {
        bank.openSavingsAccount(ANA);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bank.openSavingsAccount(new Customer("Someone Else", "246.813.579-28")));
        Account anasSecondAccount = bank.openCheckingAccount(new Customer("ana", "24681357928"), amount("0"));

        assertAll(
                () -> assertEquals("This CPF is already registered to another holder.", error.getMessage()),
                () -> assertEquals(1001, anasSecondAccount.getNumber()));
    }

    private static BigDecimal amount(String text) {
        return new BigDecimal(text);
    }

    /** Keeps the accounts in a map and can simulate a failure on the next save. */
    private static final class ControlledRepository implements BankRepository {
        private final Map<Integer, StoredAccount> saved = new LinkedHashMap<>();
        private final List<List<Integer>> calls = new ArrayList<>();
        private boolean failOnNextSave;

        @Override
        public List<StoredAccount> load() {
            return List.copyOf(saved.values());
        }

        @Override
        public void save(List<StoredAccount> accounts) {
            if (failOnNextSave) {
                failOnNextSave = false;
                throw new PersistenceFailureException("Simulated failure.");
            }
            calls.add(accounts.stream().map(StoredAccount::number).toList());
            accounts.forEach(account -> saved.put(account.number(), account));
        }
    }
}
