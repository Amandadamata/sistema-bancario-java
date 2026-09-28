package com.github.amandadamata.sistemabancario;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Define o estado e as operações comuns às contas bancárias. */
public abstract class Conta {

    private static final int PRIMEIRO_NUMERO_CONTA = 1000;
    private static int proximoNumero = PRIMEIRO_NUMERO_CONTA;

    private final int numero;
    private final Cliente titular;
    private final List<Transacao> historico;
    private double saldo;

    protected Conta(Cliente titular) {
        this.numero = proximoNumero++;
        this.titular = Objects.requireNonNull(titular, "O titular é obrigatório.");
        this.historico = new ArrayList<>();
    }

    public void depositar(double valor) {
        creditar(valor, "DEPÓSITO");
    }

    public void sacar(double valor) throws SaldoInsuficienteException {
        validarValorPositivo(valor, "O valor do saque deve ser um número finito maior que zero.");

        double saldoDisponivel = calcularSaldoDisponivel();
        if (valor > saldoDisponivel) {
            throw new SaldoInsuficienteException(
                    "Saldo insuficiente. Disponível para saque: R$ "
                            + String.format(Locale.US, "%.2f", saldoDisponivel));
        }

        debitar(valor, "SAQUE");
    }

    /**
     * Transfere um valor para outra conta.
     *
     * @throws SaldoInsuficienteException quando a conta de origem não possui saldo disponível
     * @throws IllegalArgumentException quando o destino ou o valor são inválidos
     */
    public void transferir(Conta destino, double valor) throws SaldoInsuficienteException {
        if (destino == null) {
            throw new IllegalArgumentException("Conta de destino inválida.");
        }
        if (destino == this) {
            throw new IllegalArgumentException("Não é possível transferir para a mesma conta.");
        }

        validarValorPositivo(
                valor, "O valor da transferência deve ser um número finito maior que zero.");

        double saldoDisponivel = calcularSaldoDisponivel();
        if (valor > saldoDisponivel) {
            throw new SaldoInsuficienteException(
                    "Saldo insuficiente. Disponível para transferência: R$ "
                            + String.format(Locale.US, "%.2f", saldoDisponivel));
        }

        destino.validarCredito(valor);
        debitar(valor, "TRANSFERÊNCIA ENVIADA (conta " + destino.getNumero() + ")");
        destino.creditar(valor, "TRANSFERÊNCIA RECEBIDA (conta " + numero + ")");
    }

    protected void creditar(double valor, String descricao) {
        validarCredito(valor);
        saldo += valor;
        registrarTransacao(descricao, valor);
    }

    protected double calcularSaldoDisponivel() {
        return saldo;
    }

    private void validarCredito(double valor) {
        validarValorPositivo(valor, "O valor deve ser um número finito maior que zero.");
        if (!Double.isFinite(saldo + valor)) {
            throw new IllegalArgumentException("O saldo resultante excede o limite suportado.");
        }
    }

    private static void validarValorPositivo(double valor, String mensagem) {
        if (!Double.isFinite(valor) || valor <= 0) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private void debitar(double valor, String descricao) {
        double novoSaldo = saldo - valor;
        if (!Double.isFinite(novoSaldo)) {
            throw new IllegalArgumentException("O saldo resultante excede o limite suportado.");
        }
        saldo = novoSaldo;
        registrarTransacao(descricao, valor);
    }

    private void registrarTransacao(String descricao, double valor) {
        historico.add(new Transacao(descricao, valor, saldo));
    }

    public void exibirExtrato() {
        System.out.println("\n===== EXTRATO — Conta " + numero + " (" + getTipoConta()
                + ") — Titular: " + titular.getNome() + " =====");
        if (historico.isEmpty()) {
            System.out.println("Nenhuma movimentação registrada ainda.");
        } else {
            for (Transacao transacao : historico) {
                System.out.println(transacao);
            }
        }
        System.out.printf(Locale.US, "Saldo atual: R$ %.2f%n", saldo);
        System.out.println("=================================================\n");
    }

    public abstract String getTipoConta();

    public int getNumero() {
        return numero;
    }

    public Cliente getTitular() {
        return titular;
    }

    public double getSaldo() {
        return saldo;
    }

}
