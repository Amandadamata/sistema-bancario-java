package com.github.amandadamata.sistemabancario;

public final class ContaPoupanca extends Conta {

    private static final double TAXA_RENDIMENTO_MENSAL = 0.005; // 0,5% ao mês

    public ContaPoupanca(Cliente titular) {
        super(titular);
    }

    public double aplicarRendimento() {
        double rendimento = getSaldo() * TAXA_RENDIMENTO_MENSAL;
        if (rendimento > 0) {
            creditar(rendimento, "RENDIMENTO");
        }
        return rendimento;
    }

    @Override
    public String getTipoConta() {
        return "Conta Poupança";
    }
}
