package com.github.amandadamata.sistemabancario;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

final class Transacao {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final String descricao;
    private final double valor;
    private final double saldoResultante;
    private final LocalDateTime dataHora;

    Transacao(String descricao, double valor, double saldoResultante) {
        this.descricao = Objects.requireNonNull(descricao, "A descrição é obrigatória.");
        this.valor = valor;
        this.saldoResultante = saldoResultante;
        this.dataHora = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "[%s] %-30s R$ %10.2f | Saldo após: R$ %.2f",
                dataHora.format(FORMATO_DATA), descricao, valor, saldoResultante);
    }
}
