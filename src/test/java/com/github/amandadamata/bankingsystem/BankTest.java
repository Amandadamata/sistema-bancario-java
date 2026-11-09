package com.github.amandadamata.bankingsystem;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BankTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-20T13:00:00Z"), ZoneOffset.UTC);

    @Test
    void accountsGetSequentialNumbersFrom1000() {
        Bank bank = new Bank(CLOCK);

        Account first = bank.openSavingsAccount(customer());
        Account second = bank.openCheckingAccount(customer(), BigDecimal.ZERO);

        assertAll(
                () -> assertEquals(1000, first.getNumber()),
                () -> assertEquals(1001, second.getNumber()));
    }

    @Test
    void eachBankHasItsOwnNumbering() {
        new Bank(CLOCK).openSavingsAccount(customer());

        Account account = new Bank(CLOCK).openSavingsAccount(customer());

        assertEquals(1000, account.getNumber());
    }

    @Test
    void rejectedOpeningKeepsTheNumber() {
        Bank bank = new Bank(CLOCK);
        bank.openSavingsAccount(customer());

        assertThrows(IllegalArgumentException.class,
                () -> bank.openCheckingAccount(customer(), new BigDecimal("-1.00")));
        Account nextAccount = bank.openSavingsAccount(customer());

        assertAll(
                () -> assertEquals(1001, nextAccount.getNumber()),
                () -> assertEquals(2, bank.listAccounts().size()));
    }

    @Test
    void openedAccountCanBeFoundByNumber() {
        Bank bank = new Bank(CLOCK);
        Account account = bank.openSavingsAccount(customer());

        assertAll(
                () -> assertEquals(Optional.of(account), bank.findAccountByNumber(account.getNumber())),
                () -> assertEquals(Optional.empty(), bank.findAccountByNumber(-1)));
    }

    @Test
    void listingKeepsOpeningOrderAndIsReadOnly() {
        Bank bank = new Bank(CLOCK);
        Account first = bank.openSavingsAccount(customer());
        List<Account> accounts = bank.listAccounts();

        assertThrows(UnsupportedOperationException.class, accounts::clear);
        Account second = bank.openSavingsAccount(customer());

        assertAll(
                () -> assertEquals(List.of(first), accounts),
                () -> assertEquals(List.of(first, second), bank.listAccounts()));
    }

    @Test
    void bankRequiresAClock() {
        assertThrows(NullPointerException.class, () -> new Bank(null));
    }

    private static Customer customer() {
        return new Customer("Customer", "246.813.579-28");
    }
}
