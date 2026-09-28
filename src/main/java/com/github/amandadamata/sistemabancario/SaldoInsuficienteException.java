package com.github.amandadamata.sistemabancario;

/** Indica que a conta não possui saldo disponível para a operação. */
public class SaldoInsuficienteException extends Exception {

    private static final long serialVersionUID = 1L;

    public SaldoInsuficienteException(String mensagem) {
        super(mensagem);
    }
}
