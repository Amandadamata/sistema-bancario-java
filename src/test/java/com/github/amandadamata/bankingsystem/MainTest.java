package com.github.amandadamata.bankingsystem;

import com.github.amandadamata.bankingsystem.persistence.SqliteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the menu with simulated input, like a person typing in the terminal. */
class MainTest {

    @Test
    void fullFlowWithBrazilianFormattedAmounts() {
        String output = runMenu(
                "1", "Ana Pereira", "246.813.579-28", "2", // savings account 1000
                "1", "Bruno Alves", "135.792.468-28", "1", "100,00", // checking account 1001
                "2", "1000", "0,10",
                "2", "1000", "0,20",
                "4", "1000", "1001", "0,05",
                "5", "1000",
                "0");

        assertAll(
                () -> assertTrue(output.contains("Account created! Number: 1000"), output),
                () -> assertTrue(output.contains("Account created! Number: 1001"), output),
                () -> assertTrue(output.contains("Transfer completed!"), output),
                () -> assertTrue(output.contains("Transfer sent (to account 1001)"), output),
                () -> assertTrue(output.contains("Current balance: R$ 0,25"), output));
    }

    @Test
    void repeatedInterestInTheSameMonthIsRejected() {
        String output = runMenu(
                "1", "Ana Pereira", "246.813.579-28", "2",
                "2", "1000", "1000",
                "7", "1000",
                "7", "1000",
                "0");

        assertAll(
                () -> assertTrue(output.contains("Interest of R$ 5,00 applied to account 1000."), output),
                () -> assertTrue(output.contains("has already been applied to this account"), output));
    }

    @Test
    void invalidInputsAskAgainWithoutClosingTheMenu() {
        String output = runMenu(
                "abc",
                "1", "Ana Pereira", "246.813.579-28", "2",
                "2", "1000", "dez reais", "10,999", "10,99",
                "3", "1000", "0",
                "2", "9999",
                "0");

        assertAll(
                () -> assertTrue(output.contains("Invalid input. Enter a whole number"), output),
                () -> assertTrue(output.contains("Invalid amount."), output),
                () -> assertTrue(output.contains("Deposit completed!"), output),
                () -> assertTrue(output.contains("Error: The withdrawal amount must be greater than zero."), output),
                () -> assertTrue(output.contains("Account not found."), output),
                () -> assertTrue(output.contains("Shutting down. Goodbye!"), output));
    }

    @Test
    void rejectedOperationsEmptyStatementAndListing() {
        String output = runMenu(
                "6",                                                   // list with no accounts
                "1", "Ana Pereira", "246.813.579-28", "1", "50",         // checking 1000, limit 50
                "1", "Bruno Alves", "135.792.468-28", "2",             // savings 1001
                "5", "1001",                                           // empty statement
                "3", "1000", "50,01",                                  // withdrawal above the available balance
                "7", "1000",                                           // interest on a checking account
                "7", "1001",                                           // interest with a zero balance
                "4", "1000", "1000", "10",                             // transfer to the same account
                "6",
                "5", "1000",
                "0");

        assertAll(
                () -> assertTrue(output.contains("No accounts yet."), output),
                () -> assertTrue(output.contains("No transactions yet."), output),
                () -> assertTrue(output.contains("Error: Insufficient balance. Available for withdrawal: R$ 50,00"), output),
                () -> assertTrue(output.contains("This operation is only available for savings accounts."), output),
                () -> assertTrue(output.contains("The current balance doesn't earn at least R$ 0,01 in interest."), output),
                () -> assertTrue(output.contains("Error: You can't transfer to the same account."), output),
                () -> assertTrue(output.contains("No. 1001 | Savings Account  | Holder: Bruno Alves"), output),
                () -> assertTrue(output.contains("Overdraft limit: R$ 50,00 | Available: R$ 50,00"), output));
    }

    @Test
    void invalidCpfIsAskedAgainWithoutEchoingIt() {
        String output = runMenu(
                "1", "Ana Pereira", "123", "246.813.579-29", "246.813.579-28", "2",
                "0");

        assertAll(
                () -> assertTrue(output.contains("The CPF must have 11 digits."), output),
                () -> assertTrue(output.contains("Invalid CPF: the check digits don't match."), output),
                () -> assertTrue(output.contains("Account created! Number: 1000"), output),
                () -> assertFalse(output.contains("246.813.579"), output));
    }

    @Test
    void cpfOfAnotherHolderIsRejected() {
        String output = runMenu(
                "1", "Ana Pereira", "246.813.579-28", "2",
                "1", "Someone Else", "24681357928", "2",
                "0");

        assertTrue(output.contains("Error: This CPF is already registered to another holder."), output);
    }

    @Test
    void dataSurvivesARestart(@TempDir Path folder) {
        Path file = folder.resolve("bank.db");

        runMenu(Bank.load(CLOCK, new SqliteRepository(file)),
                "1", "Ana Pereira", "246.813.579-28", "2",
                "2", "1000", "250,75",
                "0");
        String secondRun = runMenu(Bank.load(CLOCK, new SqliteRepository(file)),
                "6",
                "5", "1000",
                "0");

        assertAll(
                () -> assertTrue(secondRun.contains("No. 1000 | Savings Account  | Holder: Ana Pereira"), secondRun),
                () -> assertTrue(secondRun.contains("Current balance: R$ 250,75"), secondRun),
                () -> assertTrue(secondRun.contains("Deposit"), secondRun));
    }

    @Test
    void defaultOrConfiguredDataFile() {
        assertAll(
                () -> assertEquals(Path.of("data", "linha.db"), Main.resolveFile(null)),
                () -> assertEquals(Path.of("data", "linha.db"), Main.resolveFile("  ")),
                () -> assertEquals(Path.of("/tmp/x.db"), Main.resolveFile(" /tmp/x.db ")));
    }

    @Test
    void endOfInputExitsSafely() {
        String output = runMenu("1", "Ana Pereira");

        assertTrue(output.contains("Input ended. Shutting down safely."), output);
    }

    private static final AdjustableClock CLOCK = AdjustableClock.at("2026-10-20T13:00:00Z");

    private static String runMenu(String... rows) {
        return runMenu(new Bank(CLOCK), rows);
    }

    private static String runMenu(Bank bank, String... rows) {
        byte[] input = (String.join("\n", rows) + "\n").getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(outputBytes, true, StandardCharsets.UTF_8);

        new Main(bank, new ByteArrayInputStream(input), output).runMenu();

        return outputBytes.toString(StandardCharsets.UTF_8);
    }
}
