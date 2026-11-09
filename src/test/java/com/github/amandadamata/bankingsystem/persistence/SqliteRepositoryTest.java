package com.github.amandadamata.bankingsystem.persistence;

import com.github.amandadamata.bankingsystem.Bank;
import com.github.amandadamata.bankingsystem.Customer;
import com.github.amandadamata.bankingsystem.Account;
import com.github.amandadamata.bankingsystem.CheckingAccount;
import com.github.amandadamata.bankingsystem.SavingsAccount;
import com.github.amandadamata.bankingsystem.PersistenceFailureException;
import com.github.amandadamata.bankingsystem.InsufficientFundsException;
import com.github.amandadamata.bankingsystem.Transaction;
import com.github.amandadamata.bankingsystem.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests with a real SQLite file in a temporary folder.
 *
 * <p>"Restarting" means creating a new {@link SqliteRepository} and a new {@link Bank} on the
 * same file, exactly as happens when the program is closed and opened again.
 */
class SqliteRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T13:00:00Z"), ZoneOffset.UTC);
    private static final Customer ANA = new Customer("Ana", "246.813.579-28");
    private static final Customer BRUNO = new Customer("Bruno", "135.792.468-28");

    @TempDir
    Path folder;

    private Path file;

    @BeforeEach
    void setFile() {
        file = folder.resolve("data/bank.db");
    }

    @Test
    void restartKeepsCustomersAccountsBalancesHistoryAndInterest() throws InsufficientFundsException {
        Bank bank = Bank.load(CLOCK, new SqliteRepository(file));
        CheckingAccount checking = bank.openCheckingAccount(ANA, amount("200.00"));
        SavingsAccount savings = bank.openSavingsAccount(BRUNO);
        bank.deposit(savings.getNumber(), amount("1000.00"));
        bank.applyInterest(savings.getNumber());
        bank.transfer(checking.getNumber(), savings.getNumber(), amount("150.50"));

        Bank restarted = Bank.load(CLOCK, new SqliteRepository(file));

        Account loadedChecking = restarted.findAccountByNumber(1000).orElseThrow();
        Account loadedSavings = restarted.findAccountByNumber(1001).orElseThrow();
        assertAll(
                () -> assertEquals(2, restarted.listAccounts().size()),
                () -> assertEquals("Ana", loadedChecking.getHolder().getName()),
                () -> assertEquals(ANA.getCpf(), loadedChecking.getHolder().getCpf()),
                () -> assertEquals(amount("-150.50"), loadedChecking.getBalance()),
                () -> assertEquals(amount("49.50"), loadedChecking.getAvailableBalance()),
                () -> assertEquals(checking.getHistory(), loadedChecking.getHistory()),
                () -> assertEquals(amount("1155.50"), loadedSavings.getBalance()),
                () -> assertEquals(savings.getHistory(), loadedSavings.getHistory()),
                () -> assertThrows(IllegalStateException.class, () -> restarted.applyInterest(1001)),
                () -> assertEquals(1002, restarted.openSavingsAccount(ANA).getNumber()));
    }

    @Test
    void transferInterruptedMidSaveLeavesNeitherSideSaved() throws Exception {
        Bank bank = Bank.load(CLOCK, new SqliteRepository(file));
        SavingsAccount source = bank.openSavingsAccount(ANA);
        SavingsAccount destination = bank.openSavingsAccount(BRUNO);
        bank.deposit(source.getNumber(), amount("100.00"));
        // Makes the database reject the destination entry after the source was already saved
        // in the same transaction.
        runSql("""
                CREATE TRIGGER simulated_failure BEFORE INSERT ON transactions
                WHEN NEW.type = 'TRANSFER_RECEIVED'
                BEGIN SELECT RAISE(ABORT, 'simulated failure'); END""");

        assertThrows(PersistenceFailureException.class,
                () -> bank.transfer(source.getNumber(), destination.getNumber(), amount("40.00")));

        Bank restarted = Bank.load(CLOCK, new SqliteRepository(file));
        assertAll(
                () -> assertEquals(amount("100.00"), source.getBalance(), "source in memory"),
                () -> assertEquals(amount("0.00"), destination.getBalance(), "destination in memory"),
                () -> assertEquals(amount("100.00"), restarted.findAccountByNumber(1000).orElseThrow().getBalance()),
                () -> assertEquals(amount("0.00"), restarted.findAccountByNumber(1001).orElseThrow().getBalance()),
                () -> assertEquals(0, countRows("SELECT COUNT(*) FROM transactions WHERE type LIKE 'TRANSFER%'")));
    }

    @Test
    void amountsAreStoredAsWholeCents() throws SQLException {
        Bank bank = Bank.load(CLOCK, new SqliteRepository(file));
        bank.openSavingsAccount(ANA);
        bank.deposit(1000, amount("10.50"));

        try (Connection connection = connect(); Statement query = connection.createStatement();
             ResultSet line = query.executeQuery(
                     "SELECT balance_cents, typeof(balance_cents) FROM accounts WHERE number = 1000")) {
            assertAll(
                    () -> assertEquals(1050, line.getLong(1)),
                    () -> assertEquals("integer", line.getString(2)));
        }
    }

    @Test
    void cpfIsStoredWithoutPunctuation() throws SQLException {
        Bank.load(CLOCK, new SqliteRepository(file)).openSavingsAccount(ANA);

        assertEquals(1, countRows("SELECT COUNT(*) FROM customers WHERE cpf = '24681357928'"));
    }

    @Test
    void balanceChangedOutsideTheAppIsDetectedOnLoad() throws SQLException {
        Bank bank = Bank.load(CLOCK, new SqliteRepository(file));
        bank.openSavingsAccount(ANA);
        bank.deposit(1000, amount("10.00"));
        runSql("UPDATE accounts SET balance_cents = 999999 WHERE number = 1000");

        PersistenceFailureException error = assertThrows(PersistenceFailureException.class,
                () -> Bank.load(CLOCK, new SqliteRepository(file)));

        assertTrue(error.getMessage().contains("the saved balance doesn't match the history"), error.getMessage());
    }

    @Test
    void entryChangedOutsideTheAppIsDetectedOnLoad() throws SQLException {
        Bank bank = Bank.load(CLOCK, new SqliteRepository(file));
        bank.openSavingsAccount(ANA);
        bank.deposit(1000, amount("10.00"));
        bank.deposit(1000, amount("5.00"));
        runSql("UPDATE transactions SET resulting_balance_cents = 2000 WHERE sequence = 1");

        PersistenceFailureException error = assertThrows(PersistenceFailureException.class,
                () -> Bank.load(CLOCK, new SqliteRepository(file)));

        assertTrue(error.getMessage().contains("an entry doesn't match"), error.getMessage());
    }

    @Test
    void unknownEntryTypeStopsTheLoad() throws SQLException {
        Bank bank = Bank.load(CLOCK, new SqliteRepository(file));
        bank.openSavingsAccount(ANA);
        bank.deposit(1000, amount("10.00"));
        runSql("UPDATE transactions SET type = 'REVERSAL' WHERE sequence = 1");

        PersistenceFailureException error = assertThrows(PersistenceFailureException.class,
                () -> new SqliteRepository(file).load());

        assertTrue(error.getMessage().contains("The file was not changed"), error.getMessage());
    }

    @Test
    void databaseConstraintsRejectInvalidData() throws SQLException {
        new SqliteRepository(file);

        assertAll(
                () -> assertThrows(SQLException.class, () -> runSql(
                        "INSERT INTO accounts VALUES (1000, 'CHECKING', '24681357928', 0, 0, NULL)")),
                () -> assertThrows(SQLException.class, () -> runSql(
                        "INSERT INTO customers VALUES ('123', 'Short CPF')")),
                () -> assertThrows(SQLException.class, () -> {
                    runSql("INSERT INTO customers VALUES ('24681357928', 'Ana')");
                    runSql("INSERT INTO accounts VALUES (1000, 'SAVINGS', '24681357928', 0, 500, NULL)");
                }));
    }

    /** The previous test uses its own connection; this one checks the connections opened by the repository. */
    @Test
    void repositoryConnectionsEnableForeignKeys() throws SQLException {
        SqliteRepository repository = new SqliteRepository(file);

        try (Connection connection = repository.openConnection(); Statement query = connection.createStatement();
             ResultSet result = query.executeQuery("PRAGMA foreign_keys")) {
            assertEquals(1, result.getInt(1));
        }
    }

    @Test
    void newDatabaseGetsTheCurrentSchema() throws SQLException {
        new SqliteRepository(file);

        assertEquals(SqliteRepository.SCHEMA_VERSION, countRows("PRAGMA user_version"));
    }

    @Test
    void fileFromSchemaVersion1IsMigratedWithoutLosingData() throws SQLException, IOException {
        Files.createDirectories(file.getParent());
        try (Connection connection = connect(); Statement command = connection.createStatement()) {
            for (String sql : new String[]{
                    "CREATE TABLE clientes (cpf TEXT PRIMARY KEY, nome TEXT NOT NULL)",
                    "CREATE TABLE contas (numero INTEGER PRIMARY KEY, tipo TEXT NOT NULL, cliente_cpf TEXT NOT NULL"
                            + " REFERENCES clientes (cpf), saldo_centavos INTEGER NOT NULL, limite_centavos INTEGER,"
                            + " ultimo_mes_rendimento TEXT)",
                    "CREATE TABLE transacoes (conta_numero INTEGER NOT NULL REFERENCES contas (numero),"
                            + " sequencia INTEGER NOT NULL, tipo TEXT NOT NULL, valor_centavos INTEGER NOT NULL,"
                            + " saldo_resultante_centavos INTEGER NOT NULL, data_hora TEXT NOT NULL,"
                            + " conta_relacionada INTEGER, PRIMARY KEY (conta_numero, sequencia))",
                    "INSERT INTO clientes VALUES ('24681357928', 'Ana'), ('13579246828', 'Bruno')",
                    "INSERT INTO contas VALUES (1000, 'POUPANCA', '24681357928', 101500, NULL, '2026-10'),"
                            + " (1001, 'CORRENTE', '13579246828', 2000, 5000, NULL)",
                    "INSERT INTO transacoes VALUES"
                            + " (1000, 1, 'DEPOSITO', 100000, 100000, '2026-10-01T10:00', NULL),"
                            + " (1000, 2, 'RENDIMENTO', 500, 100500, '2026-10-02T10:00', NULL),"
                            + " (1000, 3, 'TRANSFERENCIA_RECEBIDA', 1000, 101500, '2026-10-03T10:00', 1001),"
                            + " (1001, 1, 'DEPOSITO', 3000, 3000, '2026-10-01T11:00', NULL),"
                            + " (1001, 2, 'TRANSFERENCIA_ENVIADA', 1000, 2000, '2026-10-03T10:00', 1000)",
                    "PRAGMA user_version = 1"}) {
                command.execute(sql);
            }
        }

        Bank bank = Bank.load(CLOCK, new SqliteRepository(file));

        SavingsAccount savings = (SavingsAccount) bank.findAccountByNumber(1000).orElseThrow();
        CheckingAccount checking = (CheckingAccount) bank.findAccountByNumber(1001).orElseThrow();
        assertAll(
                () -> assertEquals(SqliteRepository.SCHEMA_VERSION, countRows("PRAGMA user_version")),
                () -> assertEquals(0, countRows("SELECT COUNT(*) FROM sqlite_master WHERE name = 'contas'")),
                () -> assertEquals(amount("1015.00"), savings.getBalance()),
                () -> assertEquals(List.of(TransactionType.DEPOSIT, TransactionType.INTEREST,
                        TransactionType.TRANSFER_RECEIVED), savings.getHistory().stream().map(Transaction::type).toList()),
                () -> assertEquals(amount("20.00"), checking.getBalance()),
                () -> assertEquals(amount("50.00"), checking.getOverdraftLimit()),
                () -> assertThrows(IllegalStateException.class, () -> bank.applyInterest(1000)));
    }

    @Test
    void schemaFromANewerVersionIsRejected() throws SQLException {
        new SqliteRepository(file);
        runSql("PRAGMA user_version = 99");

        PersistenceFailureException error = assertThrows(PersistenceFailureException.class,
                () -> new SqliteRepository(file));

        assertTrue(error.getMessage().contains("newer version"), error.getMessage());
    }

    @Test
    void nonDatabaseFileIsRejectedUnchanged() throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "this is not a database");

        assertThrows(PersistenceFailureException.class, () -> new SqliteRepository(file));
        assertEquals("this is not a database", Files.readString(file));
    }

    @Test
    void unreachablePathGivesAClearError() throws IOException {
        Path regularFile = Files.writeString(folder.resolve("blocker"), "x");

        PersistenceFailureException error = assertThrows(PersistenceFailureException.class,
                () -> new SqliteRepository(regularFile.resolve("bank.db")));

        assertTrue(error.getMessage().contains("Could not create the folder"), error.getMessage());
    }

    @Test
    void centsConversionIsExactBothWays() {
        assertAll(
                () -> assertEquals(1234, SqliteRepository.toCents(amount("12.34"))),
                () -> assertEquals(-15050, SqliteRepository.toCents(amount("-150.50"))),
                () -> assertEquals(amount("12.34"), SqliteRepository.fromCents(1234)),
                () -> assertThrows(ArithmeticException.class, () -> SqliteRepository.toCents(amount("0.001"))));
    }

    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
        try (Statement command = connection.createStatement()) {
            command.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    /** Runs a single SQL statement on its own connection. */
    private void runSql(String sql) throws SQLException {
        try (Connection connection = connect(); Statement command = connection.createStatement()) {
            command.execute(sql);
        }
    }

    private int countRows(String sql) throws SQLException {
        try (Connection connection = connect(); Statement query = connection.createStatement();
             ResultSet result = query.executeQuery(sql)) {
            return result.getInt(1);
        }
    }

    private static BigDecimal amount(String text) {
        return new BigDecimal(text);
    }
}
