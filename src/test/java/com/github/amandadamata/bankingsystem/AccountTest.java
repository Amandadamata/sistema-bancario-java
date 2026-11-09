package com.github.amandadamata.bankingsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    private AdjustableClock clock;
    private Bank bank;

    @BeforeEach
    void createBank() {
        clock = AdjustableClock.at("2026-10-20T13:00:00Z");
        bank = new Bank(clock);
    }

    @Test
    void sumOfCentsIsExact() {
        SavingsAccount account = newSavings();

        account.deposit(amount("0.10"));
        account.deposit(amount("0.20"));

        // With double, the balance would be 0.30000000000000004.
        assertEquals(amount("0.30"), account.getBalance());
    }

    @Test
    void balanceAlwaysHasTwoDecimalPlaces() {
        SavingsAccount account = newSavings();

        account.deposit(new BigDecimal("150"));

        assertEquals("150.00", account.getBalance().toPlainString());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "0.00", "-10.00", "0.001", "10.005"})
    void invalidAmountsAreRejectedWithoutChangingBalances(BigDecimal invalidAmount) {
        SavingsAccount source = newSavings();
        SavingsAccount destination = newSavings();
        source.deposit(amount("100.00"));

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> source.deposit(invalidAmount)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> source.withdraw(invalidAmount)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> source.transfer(destination, invalidAmount)),
                () -> assertEquals(amount("100.00"), source.getBalance()),
                () -> assertEquals(amount("0.00"), destination.getBalance()),
                () -> assertEquals(1, source.getHistory().size()));
    }

    @Test
    void validWithdrawalUpdatesTheBalance() throws InsufficientFundsException {
        SavingsAccount account = newSavings();
        account.deposit(amount("200.00"));

        account.withdraw(amount("75.25"));

        assertEquals(amount("124.75"), account.getBalance());
    }

    @Test
    void withdrawingTheWholeBalanceIsAllowed() throws InsufficientFundsException {
        SavingsAccount account = newSavings();
        account.deposit(amount("100.00"));

        account.withdraw(amount("100.00"));

        assertEquals(amount("0.00"), account.getBalance());
    }

    @Test
    void withdrawalWithoutFundsKeepsTheBalanceAndShowsWhatIsAvailable() {
        SavingsAccount account = newSavings();
        account.deposit(amount("100.00"));

        InsufficientFundsException error = assertThrows(InsufficientFundsException.class,
                () -> account.withdraw(amount("100.01")));

        assertAll(
                () -> assertEquals(amount("100.00"), account.getBalance()),
                () -> assertTrue(error.getMessage().contains("R$ 100,00"), error.getMessage()));
    }

    @Test
    void checkingAccountCanUseOverdraftUpToTheLimit() throws InsufficientFundsException {
        CheckingAccount account = bank.openCheckingAccount(customer("Customer"), amount("200.00"));
        account.deposit(amount("100.00"));

        account.withdraw(amount("250.00"));

        assertAll(
                () -> assertEquals(amount("-150.00"), account.getBalance()),
                () -> assertEquals(amount("50.00"), account.getAvailableBalance()));
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(amount("50.01")));
        assertEquals(amount("-150.00"), account.getBalance());
    }

    @Test
    void transferCanUseTheOverdraft() throws InsufficientFundsException {
        CheckingAccount source = bank.openCheckingAccount(customer("Source"), amount("100.00"));
        SavingsAccount destination = newSavings();

        source.transfer(destination, amount("100.00"));

        assertAll(
                () -> assertEquals(amount("-100.00"), source.getBalance()),
                () -> assertEquals(amount("100.00"), destination.getBalance()));
        assertThrows(InsufficientFundsException.class,
                () -> source.transfer(destination, amount("0.01")));
    }

    @Test
    void successfulTransferUpdatesBothBalances() throws InsufficientFundsException {
        CheckingAccount source = bank.openCheckingAccount(customer("Source"), amount("0.00"));
        SavingsAccount destination = newSavings();
        source.deposit(amount("300.00"));

        source.transfer(destination, amount("125.50"));

        assertAll(
                () -> assertEquals(amount("174.50"), source.getBalance()),
                () -> assertEquals(amount("125.50"), destination.getBalance()));
    }

    @Test
    void transferWithoutFundsChangesNoAccount() {
        SavingsAccount source = newSavings();
        SavingsAccount destination = newSavings();
        source.deposit(amount("100.00"));
        destination.deposit(amount("50.00"));

        assertThrows(InsufficientFundsException.class,
                () -> source.transfer(destination, amount("100.01")));

        assertAll(
                () -> assertEquals(amount("100.00"), source.getBalance()),
                () -> assertEquals(amount("50.00"), destination.getBalance()),
                () -> assertEquals(1, source.getHistory().size()),
                () -> assertEquals(1, destination.getHistory().size()));
    }

    @Test
    void transferToAnInvalidAccountKeepsTheBalance() {
        SavingsAccount account = newSavings();
        account.deposit(amount("100.00"));

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> account.transfer(account, amount("50.00"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> account.transfer(null, amount("50.00"))),
                () -> assertEquals(amount("100.00"), account.getBalance()));
    }

    @Test
    void historyRecordsTypeAmountBalanceDateAndRelatedAccount() throws InsufficientFundsException {
        CheckingAccount source = bank.openCheckingAccount(customer("Source"), amount("0.00"));
        SavingsAccount destination = newSavings();
        source.deposit(amount("500.00"));
        source.withdraw(amount("120.00"));
        source.transfer(destination, amount("80.00"));
        LocalDateTime now = LocalDateTime.of(2026, 10, 20, 13, 0);

        List<Transaction> history = source.getHistory();

        assertEquals(List.of(
                new Transaction(TransactionType.DEPOSIT, amount("500.00"), amount("500.00"),
                        now, OptionalInt.empty()),
                new Transaction(TransactionType.WITHDRAWAL, amount("120.00"), amount("380.00"),
                        now, OptionalInt.empty()),
                new Transaction(TransactionType.TRANSFER_SENT, amount("80.00"), amount("300.00"),
                        now, OptionalInt.of(destination.getNumber()))),
                history);
        assertEquals(List.of(new Transaction(TransactionType.TRANSFER_RECEIVED, amount("80.00"),
                        amount("80.00"), now, OptionalInt.of(source.getNumber()))),
                destination.getHistory());
    }

    @Test
    void signedAmountTellsCreditFromDebit() throws InsufficientFundsException {
        SavingsAccount account = newSavings();
        account.deposit(amount("10.00"));
        account.withdraw(amount("3.00"));

        List<BigDecimal> amounts = account.getHistory().stream()
                .map(Transaction::signedAmount)
                .toList();

        assertEquals(List.of(amount("10.00"), amount("-3.00")), amounts);
    }

    @Test
    void returnedHistoryCannotBeChanged() {
        SavingsAccount account = newSavings();
        account.deposit(amount("10.00"));

        List<Transaction> history = account.getHistory();

        assertThrows(UnsupportedOperationException.class, history::clear);
        assertEquals(1, account.getHistory().size());
    }

    @Test
    void accountTypesAreIdentifiedCorrectly() {
        Account checking = bank.openCheckingAccount(customer("Checking"), amount("100.00"));
        Account savings = newSavings();

        assertAll(
                () -> assertEquals("Checking Account", checking.getAccountType()),
                () -> assertEquals("Savings Account", savings.getAccountType()));
    }

    @Test
    void accountWithoutHolderIsRejected() {
        assertThrows(NullPointerException.class, () -> bank.openSavingsAccount(null));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-0.01", "10.001"})
    void invalidOverdraftLimitIsRejected(BigDecimal limit) {
        assertThrows(IllegalArgumentException.class,
                () -> bank.openCheckingAccount(customer("Customer"), limit));
    }

    private SavingsAccount newSavings() {
        return bank.openSavingsAccount(customer("Customer"));
    }

    /** Each person in the tests has a different synthetic CPF: a CPF belongs to a single holder. */
    private static Customer customer(String name) {
        String cpf = switch (name) {
            case "Source" -> "135.792.468-28";
            case "Destination" -> "802.461.357-35";
            case "Checking" -> "314.159.265-90";
            case "Savings" -> "271.828.182-05";
            default -> "246.813.579-28";
        };
        return new Customer(name, cpf);
    }

    private static BigDecimal amount(String text) {
        return new BigDecimal(text);
    }
}
