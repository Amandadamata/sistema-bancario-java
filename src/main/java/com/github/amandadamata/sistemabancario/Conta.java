package com.github.amandadamata.sistemabancario;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Define o estado e as operações comuns às contas bancárias.
 *
 * <p>As operações que alteram o saldo não são públicas: quem as chama é o {@link Banco}, que
 * também grava o resultado. Assim, nenhuma movimentação acontece sem ser persistida.
 */
public abstract class Conta {

    private final int numero;
    private final Cliente titular;
    private final Clock relogio;
    private final List<Transacao> historico = new ArrayList<>();
    private BigDecimal saldo = BigDecimal.ZERO.setScale(Moeda.ESCALA);

    /**
     * Contas são abertas pelo {@link Banco}, que controla a numeração. Por isso o construtor não
     * é público.
     */
    protected Conta(int numero, Cliente titular, Clock relogio) {
        if (numero <= 0) {
            throw new IllegalArgumentException("O número da conta deve ser positivo.");
        }
        this.numero = numero;
        this.titular = Objects.requireNonNull(titular, "O titular é obrigatório.");
        this.relogio = Objects.requireNonNull(relogio, "O relógio é obrigatório.");
    }

    void depositar(BigDecimal valor) {
        BigDecimal valorValidado = Moeda.validarValorPositivo(
                valor, "O valor do depósito deve ser maior que zero.");
        registrar(TipoTransacao.DEPOSITO, valorValidado, OptionalInt.empty());
    }

    void sacar(BigDecimal valor) throws SaldoInsuficienteException {
        BigDecimal valorValidado = Moeda.validarValorPositivo(
                valor, "O valor do saque deve ser maior que zero.");
        garantirSaldoDisponivel(valorValidado, "saque");
        registrar(TipoTransacao.SAQUE, valorValidado, OptionalInt.empty());
    }

    /**
     * Transfere um valor para outra conta.
     *
     * <p>Todas as validações acontecem antes de qualquer alteração de saldo, então uma
     * transferência recusada não deixa nenhuma das contas modificada.
     *
     * @throws SaldoInsuficienteException quando a conta de origem não possui saldo disponível
     * @throws IllegalArgumentException quando o destino ou o valor são inválidos
     */
    void transferir(Conta destino, BigDecimal valor) throws SaldoInsuficienteException {
        if (destino == null) {
            throw new IllegalArgumentException("Conta de destino inválida.");
        }
        if (destino == this) {
            throw new IllegalArgumentException("Não é possível transferir para a mesma conta.");
        }
        BigDecimal valorValidado = Moeda.validarValorPositivo(
                valor, "O valor da transferência deve ser maior que zero.");
        garantirSaldoDisponivel(valorValidado, "transferência");

        registrar(TipoTransacao.TRANSFERENCIA_ENVIADA, valorValidado, OptionalInt.of(destino.numero));
        destino.registrar(TipoTransacao.TRANSFERENCIA_RECEBIDA, valorValidado, OptionalInt.of(numero));
    }

    /** Permite que subclasses registrem créditos próprios, como o rendimento da poupança. */
    protected final void creditar(BigDecimal valor, TipoTransacao tipo) {
        if (!tipo.isCredito()) {
            throw new IllegalArgumentException("O tipo informado não é um crédito: " + tipo);
        }
        registrar(tipo, Moeda.validarValorPositivo(valor, "O crédito deve ser maior que zero."),
                OptionalInt.empty());
    }

    /** Saldo que pode ser usado em saques e transferências. */
    protected BigDecimal calcularSaldoDisponivel() {
        return saldo;
    }

    protected final Clock getRelogio() {
        return relogio;
    }

    private void garantirSaldoDisponivel(BigDecimal valor, String operacao)
            throws SaldoInsuficienteException {
        BigDecimal disponivel = calcularSaldoDisponivel();
        if (valor.compareTo(disponivel) > 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente. Disponível para " + operacao
                    + ": " + Moeda.formatar(disponivel));
        }
    }

    private void registrar(TipoTransacao tipo, BigDecimal valor, OptionalInt contaRelacionada) {
        saldo = tipo.isCredito() ? saldo.add(valor) : saldo.subtract(valor);
        historico.add(new Transacao(
                tipo, valor, saldo, LocalDateTime.now(relogio), contaRelacionada));
    }

    /** Guarda o estado atual, para desfazer a operação se a gravação falhar. */
    EstadoConta capturarEstado() {
        return new EstadoConta(saldo, historico.size(), null);
    }

    /** Volta ao estado capturado, descartando as movimentações feitas depois dele. */
    void restaurarEstado(EstadoConta estado) {
        saldo = estado.saldo();
        historico.subList(estado.tamanhoHistorico(), historico.size()).clear();
    }

    /**
     * Recoloca um histórico lido do armazenamento, conferindo cada lançamento.
     *
     * <p>O saldo inicial é zero; cada lançamento precisa resultar exatamente no saldo registrado
     * nele, e o último saldo precisa ser igual ao saldo gravado na conta. Qualquer diferença
     * indica dados corrompidos ou alterados fora da aplicação.
     *
     * @throws FalhaPersistenciaException se o histórico não for consistente
     */
    void restaurarHistorico(List<Transacao> lancamentos, BigDecimal saldoGravado) {
        BigDecimal saldoCalculado = BigDecimal.ZERO.setScale(Moeda.ESCALA);
        for (Transacao lancamento : lancamentos) {
            saldoCalculado = saldoCalculado.add(lancamento.valorComSinal());
            if (saldoCalculado.compareTo(lancamento.saldoResultante()) != 0) {
                throw new FalhaPersistenciaException("Dados inconsistentes na conta " + numero
                        + ": um lançamento não corresponde ao saldo registrado.");
            }
        }
        if (saldoCalculado.compareTo(saldoGravado) != 0) {
            throw new FalhaPersistenciaException("Dados inconsistentes na conta " + numero
                    + ": o saldo gravado não corresponde ao histórico.");
        }
        historico.clear();
        historico.addAll(lancamentos);
        saldo = saldoCalculado.setScale(Moeda.ESCALA);
    }

    /** Converte a conta para o formato gravado pelo repositório. */
    abstract ContaPersistida paraRegistro();

    public abstract String getTipoConta();

    public int getNumero() {
        return numero;
    }

    public Cliente getTitular() {
        return titular;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public final BigDecimal getSaldoDisponivel() {
        return calcularSaldoDisponivel();
    }

    /** Histórico em ordem cronológica. A lista retornada não pode ser alterada. */
    public List<Transacao> getHistorico() {
        return List.copyOf(historico);
    }
}
