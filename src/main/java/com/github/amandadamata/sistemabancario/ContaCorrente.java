package com.github.amandadamata.sistemabancario;

public final class ContaCorrente extends Conta {

    private final double limiteChequeEspecial;

    public ContaCorrente(Cliente titular, double limiteChequeEspecial) {
        super(titular);
        if (!Double.isFinite(limiteChequeEspecial) || limiteChequeEspecial < 0) {
            throw new IllegalArgumentException(
                    "O limite do cheque especial deve ser um número finito maior ou igual a zero.");
        }
        this.limiteChequeEspecial = limiteChequeEspecial;
    }

    @Override
    protected double calcularSaldoDisponivel() {
        return getSaldo() + limiteChequeEspecial;
    }

    @Override
    public String getTipoConta() {
        return "Conta Corrente";
    }

    public double getLimiteChequeEspecial() {
        return limiteChequeEspecial;
    }
}
