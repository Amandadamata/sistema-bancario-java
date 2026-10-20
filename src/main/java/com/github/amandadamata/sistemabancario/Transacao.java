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
        Objects.requireNonNull(tipo, "O tipo é obrigatório.");
        Objects.requireNonNull(valor, "O valor é obrigatório.");
        Objects.requireNonNull(saldoResultante, "O saldo resultante é obrigatório.");
        Objects.requireNonNull(dataHora, "A data e hora são obrigatórias.");
        Objects.requireNonNull(contaRelacionada, "Use OptionalInt.empty() quando não houver conta.");
    }

    /** Valor com sinal: positivo para créditos e negativo para débitos. */
    public BigDecimal valorComSinal() {
        return tipo.isCredito() ? valor : valor.negate();
    }
}
