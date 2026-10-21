package com.github.amandadamata.sistemabancario;

import java.util.Objects;

public final class Cliente {

    private final String nome;
    private final Cpf cpf;

    /** @throws IllegalArgumentException se o nome estiver vazio ou o CPF for inválido */
    public Cliente(String nome, String cpf) {
        this(nome, Cpf.de(cpf));
    }

    public Cliente(String nome, Cpf cpf) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do cliente é obrigatório.");
        }
        this.nome = nome.trim();
        this.cpf = Objects.requireNonNull(cpf, "O CPF é obrigatório.");
    }

    public String getNome() {
        return nome;
    }

    public Cpf getCpf() {
        return cpf;
    }

    /**
     * Mostra o CPF mascarado. {@code toString()} costuma aparecer em logs e mensagens de
     * depuração, onde o documento completo não deve ser exposto.
     */
    @Override
    public String toString() {
        return nome + " (CPF " + cpf.mascarado() + ")";
    }
}
