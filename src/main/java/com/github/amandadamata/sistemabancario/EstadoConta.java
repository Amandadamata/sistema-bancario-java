package com.github.amandadamata.sistemabancario;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Cópia do estado mutável de uma conta, tirada antes de uma operação.
 *
 * <p>Se a gravação falhar, o {@link Banco} devolve a conta a este estado, para que a memória
 * continue igual ao que está gravado (padrão <i>memento</i>).
 */
record EstadoConta(BigDecimal saldo, int tamanhoHistorico, YearMonth ultimoMesComRendimento) {
}
