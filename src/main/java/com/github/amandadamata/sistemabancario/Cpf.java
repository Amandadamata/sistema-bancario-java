package com.github.amandadamata.sistemabancario;

import java.util.Objects;

/**
 * CPF validado e normalizado (apenas os 11 dígitos).
 *
 * <p>A validação é matemática: confere o tamanho e os dois dígitos verificadores. Ela não
 * comprova identidade, titularidade nem situação cadastral na Receita Federal.
 *
 * <p>Por privacidade, {@link #toString()} e as mensagens de erro nunca mostram o número completo.
 */
public final class Cpf {

    private static final int TAMANHO = 11;

    private final String digitos;

    private Cpf(String digitos) {
        this.digitos = digitos;
    }

    /**
     * Valida e normaliza um CPF digitado com ou sem pontuação ("123.456.789-09" ou "12345678909").
     *
     * @throws IllegalArgumentException com uma mensagem que não repete o número informado
     */
    public static Cpf de(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("O CPF é obrigatório.");
        }
        String semPontuacao = texto.strip().replace(".", "").replace("-", "").replace(" ", "");
        if (!semPontuacao.chars().allMatch(c -> c >= '0' && c <= '9')) {
            throw new IllegalArgumentException("O CPF deve conter apenas números, pontos e hífen.");
        }
        if (semPontuacao.length() != TAMANHO) {
            throw new IllegalArgumentException("O CPF deve ter 11 dígitos.");
        }
        if (semPontuacao.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("CPF inválido: todos os dígitos são iguais.");
        }
        if (digitoVerificador(semPontuacao, 9) != semPontuacao.charAt(9) - '0'
                || digitoVerificador(semPontuacao, 10) != semPontuacao.charAt(10) - '0') {
            throw new IllegalArgumentException("CPF inválido: os dígitos verificadores não conferem.");
        }
        return new Cpf(semPontuacao);
    }

    /**
     * Calcula o dígito verificador a partir dos {@code quantidade} primeiros dígitos.
     *
     * <p>Cada dígito é multiplicado por um peso decrescente, começando em {@code quantidade + 1}.
     * Se o resto da soma por 11 for menor que 2, o dígito é 0; caso contrário, é 11 menos o resto.
     */
    static int digitoVerificador(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            int peso = quantidade + 1 - i;
            soma += (digitos.charAt(i) - '0') * peso;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Os 11 dígitos, sem pontuação; é o formato gravado no banco de dados. */
    public String digitos() {
        return digitos;
    }

    /** Formato de exibição: 000.000.000-00. */
    public String formatado() {
        return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "."
                + digitos.substring(6, 9) + "-" + digitos.substring(9);
    }

    /** Só os dois últimos dígitos ficam visíveis: ***.***.***-00. */
    public String mascarado() {
        return "***.***.***-" + digitos.substring(9);
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof Cpf cpf && digitos.equals(cpf.digitos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(digitos);
    }

    @Override
    public String toString() {
        return mascarado();
    }
}
