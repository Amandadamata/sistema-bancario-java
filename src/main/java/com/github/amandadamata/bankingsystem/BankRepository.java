package com.github.amandadamata.bankingsystem;

import java.util.List;

/**
 * Storage contract used by {@link Bank}.
 *
 * <p>The domain depends on this interface, not on a specific database. The real
 * implementation uses SQLite; the business rule tests use {@link #withoutPersistence()}.
 */
public interface BankRepository {

    /** Reads every saved account, ordered by number. */
    List<StoredAccount> load();

    /**
     * Saves the given accounts as a single operation: either every change is saved, or none is.
     *
     * @throws PersistenceFailureException if saving fails; in that case nothing was saved
     */
    void save(List<StoredAccount> accounts);

    /** Repository that stores nothing; the data only exists in memory. */
    static BankRepository withoutPersistence() {
        return new BankRepository() {
            @Override
            public List<StoredAccount> load() {
                return List.of();
            }

            @Override
            public void save(List<StoredAccount> accounts) {
                // Nada a gravar.
            }
        };
    }
}
