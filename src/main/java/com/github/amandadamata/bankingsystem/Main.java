package com.github.amandadamata.bankingsystem;

import com.github.amandadamata.bankingsystem.persistence.SqliteRepository;

import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

/**
 * Command-line interface.
 *
 * <p>All business rules live in the domain classes; this class only reads input, calls the
 * {@link Bank} operations and prints the results. Input and output come in through the
 * constructor so the menu flow can be tested without typing in a terminal.
 */
public class Main {

    static final Path DEFAULT_FILE = Path.of("data", "linha.db");
    private static final Path LEGACY_DEFAULT_FILE = Path.of("dados", "sistema-bancario.db");

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Bank bank;
    private final Scanner scanner;
    private final PrintStream output;

    Main(Bank bank, InputStream input, PrintStream output) {
        this.bank = bank;
        this.scanner = new Scanner(input, StandardCharsets.UTF_8);
        this.output = output;
    }

    public static void main(String[] args) {
        PrintStream output = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        Bank bank;
        try {
            String configured = AppDataFile.configuredPath();
            Path file = resolveFile(configured);
            if (configured == null || configured.isBlank()) {
                file = AppDataFile.preferExisting(file, LEGACY_DEFAULT_FILE);
            }
            bank = Bank.load(Clock.systemDefaultZone(), new SqliteRepository(file));
            output.println("Data in: " + file.toAbsolutePath().normalize());
        } catch (InvalidPathException e) {
            output.println("The variable " + AppDataFile.FILE_VARIABLE + " contains an invalid path.");
            return;
        } catch (PersistenceFailureException e) {
            output.println("Could not open the saved data. The system will not start.");
            output.println(e.getMessage());
            return;
        }
        new Main(bank, System.in, output).runMenu();
    }

    static Path resolveFile(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return DEFAULT_FILE;
        }
        return Path.of(configuredPath.trim());
    }

    void runMenu() {
        output.println("=== BANKING SYSTEM ===\n");

        try {
            int option;
            do {
                showMenu();
                option = readInt("Choose an option: ");

                switch (option) {
                    case 1 -> createAccount();
                    case 2 -> deposit();
                    case 3 -> withdraw();
                    case 4 -> transfer();
                    case 5 -> showStatement();
                    case 6 -> listAccounts();
                    case 7 -> applySavingsInterest();
                    case 0 -> output.println("Shutting down. Goodbye!");
                    default -> output.println("Invalid option. Try again.\n");
                }
            } while (option != 0);
        } catch (NoSuchElementException e) {
            output.println("\nInput ended. Shutting down safely.");
        }
    }

    private void showMenu() {
        output.println("------------ MENU ------------");
        output.println("1 - Create account");
        output.println("2 - Deposit");
        output.println("3 - Withdraw");
        output.println("4 - Transfer");
        output.println("5 - View statement");
        output.println("6 - List accounts");
        output.println("7 - Apply monthly interest (savings)");
        output.println("0 - Exit");
        output.println("-------------------------------");
    }

    private void createAccount() {
        String name = readRequiredText("Account holder name: ");
        Cpf cpf = readCpf();
        Customer customer = new Customer(name, cpf);

        output.println("Account type: 1 - Checking | 2 - Savings");
        int type;
        do {
            type = readInt("Choice: ");
            if (type != 1 && type != 2) {
                output.println("Invalid type. Enter 1 for Checking or 2 for Savings.");
            }
        } while (type != 1 && type != 2);

        try {
            Account account;
            if (type == 1) {
                BigDecimal limit = readAmount("Overdraft limit: R$ ");
                account = bank.openCheckingAccount(customer, limit);
            } else {
                account = bank.openSavingsAccount(customer);
            }
            output.println("Account created! Number: " + account.getNumber()
                    + " (" + account.getAccountType() + ")\n");
        } catch (IllegalArgumentException | PersistenceFailureException e) {
            output.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void deposit() {
        Optional<Account> account = askForAccount();
        if (account.isEmpty()) {
            return;
        }

        BigDecimal amount = readAmount("Deposit amount: R$ ");
        try {
            bank.deposit(account.get().getNumber(), amount);
            output.println("Deposit completed!\n");
        } catch (IllegalArgumentException | PersistenceFailureException e) {
            output.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void withdraw() {
        Optional<Account> account = askForAccount();
        if (account.isEmpty()) {
            return;
        }

        BigDecimal amount = readAmount("Withdrawal amount: R$ ");
        try {
            bank.withdraw(account.get().getNumber(), amount);
            output.println("Withdrawal completed!\n");
        } catch (InsufficientFundsException | IllegalArgumentException | PersistenceFailureException e) {
            output.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void transfer() {
        output.println("FROM account:");
        Optional<Account> source = askForAccount();
        if (source.isEmpty()) {
            return;
        }

        output.println("TO account:");
        Optional<Account> destination = askForAccount();
        if (destination.isEmpty()) {
            return;
        }

        BigDecimal amount = readAmount("Transfer amount: R$ ");
        try {
            bank.transfer(source.get().getNumber(), destination.get().getNumber(), amount);
            output.println("Transfer completed!\n");
        } catch (InsufficientFundsException | IllegalArgumentException | PersistenceFailureException e) {
            output.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void showStatement() {
        Optional<Account> found = askForAccount();
        if (found.isEmpty()) {
            return;
        }

        Account account = found.get();
        output.println("\n===== STATEMENT — Account " + account.getNumber() + " (" + account.getAccountType()
                + ") — Holder: " + account.getHolder().getName() + " =====");
        List<Transaction> history = account.getHistory();
        if (history.isEmpty()) {
            output.println("No transactions yet.");
        }
        for (Transaction transaction : history) {
            output.println(formatTransaction(transaction));
        }
        output.println("Current balance: " + Money.format(account.getBalance()));
        if (account instanceof CheckingAccount checking) {
            output.println("Overdraft limit: "
                    + Money.format(checking.getOverdraftLimit())
                    + " | Available: " + Money.format(account.getAvailableBalance()));
        }
        output.println("=================================================\n");
    }

    private String formatTransaction(Transaction transaction) {
        String description = transaction.type().getDescription();
        if (transaction.relatedAccount().isPresent()) {
            String preposition = transaction.type().isCredit() ? "from" : "to";
            description += " (" + preposition + " account " + transaction.relatedAccount().getAsInt() + ")";
        }
        String sign = transaction.type().isCredit() ? "+" : "-";
        return String.format("[%s] %-42s %s%s | Balance after: %s",
                transaction.dateTime().format(DATE_FORMAT),
                description,
                sign,
                Money.format(transaction.amount()),
                Money.format(transaction.resultingBalance()));
    }

    private void listAccounts() {
        output.println("\n===== ACCOUNTS =====");
        List<Account> accounts = bank.listAccounts();
        if (accounts.isEmpty()) {
            output.println("No accounts yet.");
        }
        for (Account account : accounts) {
            output.printf("No. %d | %-16s | Holder: %-20s | Balance: %s%n",
                    account.getNumber(), account.getAccountType(), account.getHolder().getName(),
                    Money.format(account.getBalance()));
        }
        output.println();
    }

    private void applySavingsInterest() {
        Optional<Account> account = askForAccount();
        if (account.isEmpty()) {
            return;
        }

        try {
            BigDecimal interest = bank.applyInterest(account.get().getNumber());
            if (interest.signum() > 0) {
                output.println("Interest of " + Money.format(interest)
                        + " applied to account " + account.get().getNumber() + ".\n");
            } else {
                output.println("The current balance doesn't earn at least R$ 0,01 in interest.\n");
            }
        } catch (IllegalArgumentException | IllegalStateException | PersistenceFailureException e) {
            output.println("Error: " + e.getMessage() + "\n");
        }
    }

    private Optional<Account> askForAccount() {
        int number = readInt("Account number: ");
        Optional<Account> account = bank.findAccountByNumber(number);
        if (account.isEmpty()) {
            output.println("Account not found.\n");
        }
        return account;
    }

    private int readInt(String message) {
        output.print(message);
        while (true) {
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                output.print("Invalid input. Enter a whole number: ");
            }
        }
    }

    /** Reads a money amount. Business rule checks (for example, positive) stay in the domain. */
    private BigDecimal readAmount(String message) {
        output.print(message);
        while (true) {
            try {
                return Money.parse(scanner.nextLine());
            } catch (IllegalArgumentException e) {
                output.print(e.getMessage() + " Try again: ");
            }
        }
    }

    /** Asks again until it gets a valid CPF. The error message does not repeat the number. */
    private Cpf readCpf() {
        while (true) {
            output.print("Holder's CPF: ");
            try {
                return Cpf.of(scanner.nextLine());
            } catch (IllegalArgumentException e) {
                output.println(e.getMessage());
            }
        }
    }

    private String readRequiredText(String message) {
        while (true) {
            output.print(message);
            String amount = scanner.nextLine().trim();
            if (!amount.isEmpty()) {
                return amount;
            }
            output.println("This field is required. Enter a value.");
        }
    }
}
