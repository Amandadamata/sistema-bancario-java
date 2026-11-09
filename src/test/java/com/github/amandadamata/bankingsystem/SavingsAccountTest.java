package com.github.amandadamata.bankingsystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavingsAccountTest {

    private AdjustableClock clock;
    private SavingsAccount account;

    @BeforeEach
    void openAccount() {
        clock = AdjustableClock.at("2026-10-20T13:00:00Z");
        account = new Bank(clock).openSavingsAccount(new Customer("Customer", "246.813.579-28"));
    }

    @Test
    void interestCreditsHalfAPercentOfTheBalance() {
        account.deposit(amount("1000.00"));

        BigDecimal interest = account.applyInterest();

        assertAll(
                () -> assertEquals(amount("5.00"), interest),
                () -> assertEquals(amount("1005.00"), account.getBalance()),
                () -> assertEquals(TransactionType.INTEREST,
                        account.getHistory().get(1).type()));
    }

    @Test
    void interestCanOnlyBeAppliedOncePerMonth() {
        account.deposit(amount("1000.00"));
        account.applyInterest();

        IllegalStateException error = assertThrows(IllegalStateException.class,
                account::applyInterest);

        assertAll(
                () -> assertEquals(amount("1005.00"), account.getBalance()),
                () -> assertEquals(2, account.getHistory().size()),
                () -> assertTrue(error.getMessage().contains("10/2026"), error.getMessage()));
    }

    @Test
    void interestIsAvailableAgainNextMonth() {
        account.deposit(amount("1000.00"));
        account.applyInterest();
        clock.advance(Duration.ofDays(12)); // 01/11/2026

        BigDecimal interest = account.applyInterest();

        assertAll(
                // 0.5% of 1005.00 = 5.025; HALF_EVEN rounds the tie to the even cent (5.02).
                () -> assertEquals(amount("5.02"), interest),
                () -> assertEquals(amount("1010.02"), account.getBalance()));
    }

    @Test
    void roundingTiesGoToTheEvenCent() {
        account.deposit(amount("101.00")); // 0.5% = 0.505

        BigDecimal interest = account.applyInterest();

        assertEquals(amount("0.50"), interest);
    }

    @Test
    void tinyBalanceCreatesNoEntryAndKeepsTheMonth() {
        account.deposit(amount("0.50")); // 0.5% = 0.0025

        BigDecimal initialInterest = account.applyInterest();
        account.deposit(amount("199.50"));
        BigDecimal interestAfterDeposit = account.applyInterest();

        assertAll(
                () -> assertEquals(amount("0.00"), initialInterest),
                () -> assertEquals(amount("1.00"), interestAfterDeposit),
                () -> assertEquals(3, account.getHistory().size()));
    }

    private static BigDecimal amount(String text) {
        return new BigDecimal(text);
    }
}
