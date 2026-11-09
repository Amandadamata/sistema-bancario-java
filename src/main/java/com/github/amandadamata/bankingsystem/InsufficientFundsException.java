package com.github.amandadamata.bankingsystem;

/** The account does not have enough available balance for the operation. */
public class InsufficientFundsException extends Exception {

    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String message) {
        super(message);
    }
}
