package com.github.amandadamata.sistemabancario;

/** Tipos de movimentação registrados no histórico de uma conta. */
public enum TipoTransacao {
    DEPOSITO("Depósito", true),
    SAQUE("Saque", false),
    TRANSFERENCIA_ENVIADA("Transferência enviada", false),
    TRANSFERENCIA_RECEBIDA("Transferência recebida", true),
    RENDIMENTO("Rendimento", true);

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
