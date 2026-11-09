package com.github.amandadamata.bankingsystem.persistence;

import com.github.amandadamata.bankingsystem.StoredAccount;
import com.github.amandadamata.bankingsystem.PersistenceFailureException;
import com.github.amandadamata.bankingsystem.BankRepository;
import com.github.amandadamata.bankingsystem.AccountType;
import com.github.amandadamata.bankingsystem.TransactionType;
import com.github.amandadamata.bankingsystem.Transaction;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Saves the accounts to a SQLite file using JDBC.
 *
 * <p>Money amounts are stored as whole cents ({@code INTEGER}). SQLite's {@code REAL} type is
 * binary floating point and would bring back the rounding errors that the domain avoids with
 * {@code BigDecimal}.
 */
public final class SqliteRepository implements BankRepository {

    /** Schema version, kept in {@code PRAGMA user_version}. */
    static final int SCHEMA_VERSION = 2;

    private static final String[] SCHEMA = {
        """
        CREATE TABLE customers (
            cpf  TEXT PRIMARY KEY CHECK (length(cpf) = 11),
            name TEXT NOT NULL CHECK (length(trim(name)) > 0)
        )""",
        """
        CREATE TABLE accounts (
            number                 INTEGER PRIMARY KEY CHECK (number > 0),
            type                   TEXT    NOT NULL CHECK (type IN ('CHECKING', 'SAVINGS')),
            customer_cpf           TEXT    NOT NULL REFERENCES customers (cpf),
            balance_cents          INTEGER NOT NULL,
            overdraft_limit_cents  INTEGER CHECK (overdraft_limit_cents >= 0),
            last_interest_month    TEXT,
            CHECK ((type = 'CHECKING' AND overdraft_limit_cents IS NOT NULL)
                OR (type = 'SAVINGS' AND overdraft_limit_cents IS NULL))
        )""",
        """
        CREATE TABLE transactions (
            account_number           INTEGER NOT NULL REFERENCES accounts (number),
            sequence                 INTEGER NOT NULL CHECK (sequence > 0),
            type                     TEXT    NOT NULL,
            amount_cents             INTEGER NOT NULL CHECK (amount_cents > 0),
            resulting_balance_cents  INTEGER NOT NULL,
            occurred_at              TEXT    NOT NULL,
            related_account          INTEGER,
            PRIMARY KEY (account_number, sequence)
        )"""
    };

    /**
     * Moves data from schema 1, which used Portuguese table, column and value names, to the
     * current schema. Runs once, inside the same transaction as the new tables.
     */
    private static final String[] MIGRATION_FROM_V1 = {
        "INSERT INTO customers (cpf, name) SELECT cpf, nome FROM clientes",
        """
        INSERT INTO accounts (number, type, customer_cpf, balance_cents, overdraft_limit_cents,
                              last_interest_month)
        SELECT numero, CASE tipo WHEN 'CORRENTE' THEN 'CHECKING' WHEN 'POUPANCA' THEN 'SAVINGS' END,
               cliente_cpf, saldo_centavos, limite_centavos, ultimo_mes_rendimento
        FROM contas""",
        """
        INSERT INTO transactions (account_number, sequence, type, amount_cents,
                                  resulting_balance_cents, occurred_at, related_account)
        SELECT conta_numero, sequencia,
               CASE tipo WHEN 'DEPOSITO' THEN 'DEPOSIT'
                         WHEN 'SAQUE' THEN 'WITHDRAWAL'
                         WHEN 'TRANSFERENCIA_ENVIADA' THEN 'TRANSFER_SENT'
                         WHEN 'TRANSFERENCIA_RECEBIDA' THEN 'TRANSFER_RECEIVED'
                         WHEN 'RENDIMENTO' THEN 'INTEREST'
                         ELSE tipo END,
               valor_centavos, saldo_resultante_centavos, data_hora, conta_relacionada
        FROM transacoes""",
        "DROP TABLE transacoes",
        "DROP TABLE contas",
        "DROP TABLE clientes",
    };

    private final Path file;

    /**
     * Opens (or creates) the database and checks the schema version.
     *
     * @throws PersistenceFailureException if the file cannot be opened or comes from a newer
     *                                     version of the application
     */
    public SqliteRepository(Path file) {
        this.file = Objects.requireNonNull(file, "The database file is required.");
        prepareFile();
    }

    @Override
    public List<StoredAccount> load() {
        String sql = """
                SELECT c.number, c.type, c.balance_cents, c.overdraft_limit_cents,
                       c.last_interest_month, cl.name, cl.cpf
                FROM accounts c JOIN customers cl ON cl.cpf = c.customer_cpf
                ORDER BY c.number""";
        try (Connection connection = openConnection();
             Statement query = connection.createStatement();
             ResultSet rows = query.executeQuery(sql)) {
            List<StoredAccount> accounts = new ArrayList<>();
            while (rows.next()) {
                int number = rows.getInt("number");
                String month = rows.getString("last_interest_month");
                long limit = rows.getLong("overdraft_limit_cents");
                boolean noLimit = rows.wasNull();
                accounts.add(new StoredAccount(
                        number,
                        AccountType.valueOf(rows.getString("type")),
                        rows.getString("name"),
                        rows.getString("cpf"),
                        noLimit ? null : fromCents(limit),
                        fromCents(rows.getLong("balance_cents")),
                        month == null ? null : YearMonth.parse(month),
                        loadHistory(connection, number)));
            }
            return accounts;
        } catch (SQLException | RuntimeException failure) {
            throw new PersistenceFailureException(
                    "Could not read the data from " + file + ". The file was not changed.", failure);
        }
    }

    /**
     * Saves the accounts in a single database transaction.
     *
     * <p>If any statement fails, {@code rollback()} undoes all the previous ones: in a transfer,
     * source and destination are saved together or not at all.
     */
    @Override
    public void save(List<StoredAccount> accounts) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                for (StoredAccount account : accounts) {
                    saveCustomer(connection, account);
                    saveAccount(connection, account);
                    saveNewTransactions(connection, account);
                }
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                connection.rollback();
                throw failure;
            }
        } catch (SQLException | RuntimeException failure) {
            throw new PersistenceFailureException("Could not save the data to " + file + ".", failure);
        }
    }

    private void prepareFile() {
        try {
            Path folder = file.toAbsolutePath().getParent();
            if (folder != null) {
                Files.createDirectories(folder);
            }
        } catch (IOException failure) {
            throw new PersistenceFailureException("Could not create the folder for " + file + ".", failure);
        }
        try (Connection connection = openConnection(); Statement command = connection.createStatement()) {
            int version;
            try (ResultSet result = command.executeQuery("PRAGMA user_version")) {
                version = result.getInt(1);
            }
            if (version == 0) {
                createSchema(connection, new String[0]);
            } else if (version == 1) {
                createSchema(connection, MIGRATION_FROM_V1);
            } else if (version > SCHEMA_VERSION) {
                throw new PersistenceFailureException("The file " + file + " was created by a newer"
                        + " version of the application (schema " + version + "). It was not changed.");
            }
        } catch (SQLException failure) {
            throw new PersistenceFailureException("Could not open the database " + file
                    + ". Check that the path exists and that you have read and write permission.", failure);
        }
    }

    /** Creates the current tables and then runs {@code migration}, all in one transaction. */
    private void createSchema(Connection connection, String[] migration) throws SQLException {
        connection.setAutoCommit(false);
        try (Statement command = connection.createStatement()) {
            for (String table : SCHEMA) {
                command.executeUpdate(table);
            }
            for (String step : migration) {
                command.executeUpdate(step);
            }
            command.executeUpdate("PRAGMA user_version = " + SCHEMA_VERSION);
            connection.commit();
        } catch (SQLException failure) {
            connection.rollback();
            throw failure;
        }
    }

    /** Turns on foreign keys, which SQLite leaves off by default. Package-private so it can be tested. */
    Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
        try (Statement command = connection.createStatement()) {
            command.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException failure) {
            connection.close();
            throw failure;
        }
        return connection;
    }

    private static List<Transaction> loadHistory(Connection connection, int accountNumber) throws SQLException {
        String sql = """
                SELECT type, amount_cents, resulting_balance_cents, occurred_at, related_account
                FROM transactions WHERE account_number = ? ORDER BY sequence""";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setInt(1, accountNumber);
            try (ResultSet rows = query.executeQuery()) {
                List<Transaction> history = new ArrayList<>();
                while (rows.next()) {
                    int related = rows.getInt("related_account");
                    boolean noRelated = rows.wasNull();
                    history.add(new Transaction(
                            TransactionType.valueOf(rows.getString("type")),
                            fromCents(rows.getLong("amount_cents")),
                            fromCents(rows.getLong("resulting_balance_cents")),
                            LocalDateTime.parse(rows.getString("occurred_at")),
                            noRelated ? OptionalInt.empty() : OptionalInt.of(related)));
                }
                return history;
            }
        }
    }

    /** UPSERT: inserts the customer or, if the CPF already exists, updates the name. */
    private static void saveCustomer(Connection connection, StoredAccount account) throws SQLException {
        String sql = """
                INSERT INTO customers (cpf, name) VALUES (?, ?)
                ON CONFLICT (cpf) DO UPDATE SET name = excluded.name""";
        try (PreparedStatement command = connection.prepareStatement(sql)) {
            command.setString(1, account.holderCpf());
            command.setString(2, account.holderName());
            command.executeUpdate();
        }
    }

    private static void saveAccount(Connection connection, StoredAccount account) throws SQLException {
        String sql = """
                INSERT INTO accounts (number, type, customer_cpf, balance_cents, overdraft_limit_cents,
                                    last_interest_month)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (number) DO UPDATE SET
                    balance_cents = excluded.balance_cents,
                    last_interest_month = excluded.last_interest_month""";
        try (PreparedStatement command = connection.prepareStatement(sql)) {
            command.setInt(1, account.number());
            command.setString(2, account.type().name());
            command.setString(3, account.holderCpf());
            command.setLong(4, toCents(account.balance()));
            if (account.overdraftLimit() == null) {
                command.setNull(5, Types.INTEGER);
            } else {
                command.setLong(5, toCents(account.overdraftLimit()));
            }
            command.setString(6, account.lastInterestMonth() == null
                    ? null : account.lastInterestMonth().toString());
            command.executeUpdate();
        }
    }

    /**
     * The history only grows. Saves only the entries with a sequence higher than the last one
     * saved; earlier entries are never rewritten.
     */
    private static void saveNewTransactions(Connection connection, StoredAccount account) throws SQLException {
        int alreadySaved;
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT COALESCE(MAX(sequence), 0) FROM transactions WHERE account_number = ?")) {
            query.setInt(1, account.number());
            try (ResultSet result = query.executeQuery()) {
                alreadySaved = result.getInt(1);
            }
        }
        String sql = """
                INSERT INTO transactions (account_number, sequence, type, amount_cents,
                                        resulting_balance_cents, occurred_at, related_account)
                VALUES (?, ?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement command = connection.prepareStatement(sql)) {
            List<Transaction> history = account.history();
            for (int index = alreadySaved; index < history.size(); index++) {
                Transaction transaction = history.get(index);
                command.setInt(1, account.number());
                command.setInt(2, index + 1);
                command.setString(3, transaction.type().name());
                command.setLong(4, toCents(transaction.amount()));
                command.setLong(5, toCents(transaction.resultingBalance()));
                command.setString(6, transaction.dateTime().toString());
                if (transaction.relatedAccount().isPresent()) {
                    command.setInt(7, transaction.relatedAccount().getAsInt());
                } else {
                    command.setNull(7, Types.INTEGER);
                }
                command.executeUpdate();
            }
        }
    }

    /** R$ 12,34 becomes 1234. {@code longValueExact} fails if there are fractions of a cent. */
    static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    /** 1234 becomes R$ 12,34, with scale 2. */
    static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
