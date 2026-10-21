package com.github.amandadamata.sistemabancario;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * Dados de uma conta no formato em que são gravados e lidos pelo {@link RepositorioBanco}.
 *
 * <p>É uma "fotografia" sem comportamento: o repositório só copia esses dados para o
 * armazenamento e de volta. Quem transforma esse registro em uma {@link Conta} (e confere a
 * consistência do histórico) é o {@link Banco}.
 *
 * @param limiteChequeEspecial obrigatório em conta corrente; {@code null} em poupança
 * @param ultimoMesComRendimento só para poupança; {@code null} se nunca rendeu
 */
public record ContaPersistida(
        int numero,
        TipoConta tipo,
        String titularNome,
        String titularCpf,
        BigDecimal limiteChequeEspecial,
        BigDecimal saldo,
        YearMonth ultimoMesComRendimento,
        List<Transacao> historico) {

    public ContaPersistida {
        Objects.requireNonNull(tipo, "O tipo da conta é obrigatório.");
        Objects.requireNonNull(saldo, "O saldo é obrigatório.");
        historico = List.copyOf(Objects.requireNonNull(historico, "O histórico é obrigatório."));
    }
}
