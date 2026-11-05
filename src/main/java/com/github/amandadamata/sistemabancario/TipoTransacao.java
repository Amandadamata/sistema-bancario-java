package com.github.amandadamata.sistemabancario;

/** Tipos de movimentação registrados no histórico de uma conta. */
public enum TipoTransacao {
    DEPOSITO("Deposit", true),
    SAQUE("Withdrawal", false),
    TRANSFERENCIA_ENVIADA("Transfer sent", false),
    TRANSFERENCIA_RECEBIDA("Transfer received", true),
    RENDIMENTO("Interest", true);

    private final String descricao;
    private final boolean credito;

    TipoTransacao(String descricao, boolean credito) {
        this.descricao = descricao;
        this.credito = credito;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Indica se a movimentação aumenta o saldo (crédito) ou o reduz (débito). */
    public boolean isCredito() {
        return credito;
    }
}
