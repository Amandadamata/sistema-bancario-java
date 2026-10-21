package com.github.amandadamata.sistemabancario;

import java.math.BigDecimal;
import java.time.Clock;

public final class ContaCorrente extends Conta {

    private final BigDecimal limiteChequeEspecial;

    ContaCorrente(int numero, Cliente titular, BigDecimal limiteChequeEspecial, Clock relogio) {
        super(numero, titular, relogio);
        this.limiteChequeEspecial = Moeda.validarValorNaoNegativo(limiteChequeEspecial,
                "O limite do cheque especial deve ser maior ou igual a zero.");
    }

    /** O cheque especial permite usar o limite além do saldo, deixando o saldo negativo. */
    @Override
    protected BigDecimal calcularSaldoDisponivel() {
        return getSaldo().add(limiteChequeEspecial);
    }

    @Override
    public String getTipoConta() {
        return "Conta Corrente";
    }

    public BigDecimal getLimiteChequeEspecial() {
        return limiteChequeEspecial;
    }
}
