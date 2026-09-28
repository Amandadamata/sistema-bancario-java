package com.github.amandadamata.sistemabancario;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Banco {

    private final List<Conta> contas = new ArrayList<>();

    public void adicionarConta(Conta conta) {
        contas.add(Objects.requireNonNull(conta, "A conta é obrigatória."));
    }

    public Conta buscarContaPorNumero(int numero) {
        for (Conta conta : contas) {
            if (conta.getNumero() == numero) {
                return conta;
            }
        }
        return null;
    }

    public List<Conta> listarContas() {
        return List.copyOf(contas);
    }
}
