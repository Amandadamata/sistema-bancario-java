package com.github.amandadamata.sistemabancario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Movimentação imutável do extrato.
 *
 * @param tipo            natureza da movimentação
 * @param valor           valor sempre positivo; o sinal é definido pelo tipo
 * @param saldoResultante saldo da conta logo após a movimentação
 * @param dataHora        momento em que a movimentação foi registrada
 * @param contaRelacionada número da outra conta em transferências, quando houver
 */
public record Transacao(
        TipoTransacao tipo,
        BigDecimal valor,
        BigDecimal saldoResultante,
        LocalDateTime dataHora,
        OptionalInt contaRelacionada) {

    public Transacao {
        Objects.requireNonNull(tipo, "The type is required.");
        Objects.requireNonNull(valor, "The amount is required.");
        Objects.requireNonNull(saldoResultante, "The resulting balance is required.");
        Objects.requireNonNull(dataHora, "The date and time are required.");
        Objects.requireNonNull(contaRelacionada, "Use OptionalInt.empty() when there is no other account.");
    }

    /** Valor com sinal: positivo para créditos e negativo para débitos. */
    public BigDecimal valorComSinal() {
        return tipo.isCredito() ? valor : valor.negate();
    }
}
