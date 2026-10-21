package com.github.amandadamata.sistemabancario;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Conta poupança com rendimento mensal simplificado.
 *
 * <p>Regra educacional: 0,5% sobre o saldo atual, no máximo uma vez por mês-calendário, com
 * arredondamento bancário para centavos. Não reproduz a regra oficial da poupança brasileira
 * (data de aniversário, TR e Selic).
 */
public final class ContaPoupanca extends Conta {

    static final BigDecimal TAXA_RENDIMENTO_MENSAL = new BigDecimal("0.005");

    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private YearMonth ultimoMesComRendimento;

    ContaPoupanca(int numero, Cliente titular, Clock relogio) {
        super(numero, titular, relogio);
    }

    /**
     * Credita o rendimento do mês atual.
     *
     * @return valor creditado; zero quando o saldo é pequeno demais para render um centavo
     * @throws IllegalStateException quando o rendimento deste mês já foi aplicado
     */
    BigDecimal aplicarRendimento() {
        YearMonth mesAtual = YearMonth.now(getRelogio());
        if (mesAtual.equals(ultimoMesComRendimento)) {
            throw new IllegalStateException("O rendimento de " + mesAtual.format(FORMATO_MES)
                    + " já foi aplicado a esta conta.");
        }

        BigDecimal rendimento = getSaldo()
                .multiply(TAXA_RENDIMENTO_MENSAL)
                .setScale(Moeda.ESCALA, Moeda.ARREDONDAMENTO);
        if (rendimento.signum() > 0) {
            creditar(rendimento, TipoTransacao.RENDIMENTO);
            ultimoMesComRendimento = mesAtual;
        }
        return rendimento;
    }

    /** O mês do último rendimento também faz parte do estado que precisa ser desfeito. */
    @Override
    EstadoConta capturarEstado() {
        EstadoConta base = super.capturarEstado();
        return new EstadoConta(base.saldo(), base.tamanhoHistorico(), ultimoMesComRendimento);
    }

    @Override
    void restaurarEstado(EstadoConta estado) {
        super.restaurarEstado(estado);
        ultimoMesComRendimento = estado.ultimoMesComRendimento();
    }

    void restaurarUltimoMesComRendimento(YearMonth mes) {
        ultimoMesComRendimento = mes;
    }

    @Override
    ContaPersistida paraRegistro() {
        return new ContaPersistida(getNumero(), TipoConta.POUPANCA, getTitular().getNome(),
                getTitular().getCpf().digitos(), null, getSaldo(), ultimoMesComRendimento, getHistorico());
    }

    @Override
    public String getTipoConta() {
        return "Conta Poupança";
    }
}
