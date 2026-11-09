package com.github.amandadamata.bankingsystem;

import java.util.Objects;

public final class Customer {

    private final String name;
    private final Cpf cpf;

    /** @throws IllegalArgumentException if the name is blank or the CPF is invalid */
    public Customer(String name, String cpf) {
        this(name, Cpf.of(cpf));
    }

    public Customer(String name, Cpf cpf) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The account holder's name is required.");
        }
        this.name = name.trim();
        this.cpf = Objects.requireNonNull(cpf, "The CPF is required.");
    }

    public String getName() {
        return name;
    }

    public Cpf getCpf() {
        return cpf;
    }

    /**
     * Shows the masked CPF. {@code toString()} often ends up in logs and debug messages, where
     * the full document must not be exposed.
     */
    @Override
    public String toString() {
        return name + " (CPF " + cpf.masked() + ")";
    }
}
