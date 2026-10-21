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

    /**
     * Mostra apenas os dois últimos caracteres do CPF. {@code toString()} costuma aparecer em logs
     * e mensagens de depuração, onde o documento completo não deve ser exposto.
     */
    @Override
    public String toString() {
        String finalDoCpf = cpf.length() <= 2 ? cpf : cpf.substring(cpf.length() - 2);
        return nome + " (CPF terminado em " + finalDoCpf + ")";
    }
}
