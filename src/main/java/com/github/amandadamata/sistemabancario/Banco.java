package com.github.amandadamata.sistemabancario;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Abre contas e permite encontrá-las pelo número.
 *
 * <p>O banco é o único responsável pela numeração. Cada instância tem sua própria sequência,
 * então testes e execuções diferentes não interferem entre si.
 */
public final class Banco {

    static final int PRIMEIRO_NUMERO_CONTA = 1000;

    private final Map<Integer, Conta> contas = new LinkedHashMap<>();
    private final Clock relogio;
    private int proximoNumero = PRIMEIRO_NUMERO_CONTA;

    public Banco(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "O relógio é obrigatório.");
    }

    public ContaCorrente abrirContaCorrente(Cliente titular, BigDecimal limiteChequeEspecial) {
        return registrar(new ContaCorrente(proximoNumero, titular, limiteChequeEspecial, relogio));
    }

    public ContaPoupanca abrirContaPoupanca(Cliente titular) {
        return registrar(new ContaPoupanca(proximoNumero, titular, relogio));
    }

    public Optional<Conta> buscarContaPorNumero(int numero) {
        return Optional.ofNullable(contas.get(numero));
    }

    /** Contas em ordem de abertura. A lista retornada não pode ser alterada. */
    public List<Conta> listarContas() {
        return List.copyOf(contas.values());
    }

    /**
     * O número só é consumido depois que a conta foi criada com sucesso; uma abertura recusada
     * por dados inválidos não deixa lacunas na numeração.
     */
    private <T extends Conta> T registrar(T conta) {
        contas.put(conta.getNumero(), conta);
        proximoNumero++;
        return conta;
    }
}
