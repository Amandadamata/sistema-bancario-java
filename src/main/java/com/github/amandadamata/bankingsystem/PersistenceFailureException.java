package com.github.amandadamata.bankingsystem;

/** The data could not be read or saved, or it is inconsistent. */
public class PersistenceFailureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PersistenceFailureException(String message) {
        super(message);
    }

    public PersistenceFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
