package com.github.amandadamata.sistemabancario;

public final class Cliente {

    private final String nome;
    private final String cpf;

    public Cliente(String nome, String cpf) {
        this.nome = validarCampoObrigatorio(nome, "nome");
        this.cpf = validarCampoObrigatorio(cpf, "CPF");
    }

    private static String validarCampoObrigatorio(String valor, String nomeCampo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O " + nomeCampo + " do cliente é obrigatório.");
        }
        return valor.trim();
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    @Override
    public String toString() {
        return nome + " (CPF: " + cpf + ")";
    }
}
