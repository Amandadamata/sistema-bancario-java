package com.github.amandadamata.sistemabancario;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BancoTest {

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-10-20T13:00:00Z"), ZoneOffset.UTC);

    @Test
    void contasRecebemNumerosSequenciaisAPartirDe1000() {
        Banco banco = new Banco(RELOGIO);

        Conta primeira = banco.abrirContaPoupanca(cliente());
        Conta segunda = banco.abrirContaCorrente(cliente(), BigDecimal.ZERO);

        assertAll(
                () -> assertEquals(1000, primeira.getNumero()),
                () -> assertEquals(1001, segunda.getNumero()));
    }

    @Test
    void cadaBancoTemSuaPropriaNumeracao() {
        new Banco(RELOGIO).abrirContaPoupanca(cliente());

        Conta conta = new Banco(RELOGIO).abrirContaPoupanca(cliente());

        assertEquals(1000, conta.getNumero());
    }

    @Test
    void aberturaRecusadaNaoConsomeNumero() {
        Banco banco = new Banco(RELOGIO);
        banco.abrirContaPoupanca(cliente());

        assertThrows(IllegalArgumentException.class,
                () -> banco.abrirContaCorrente(cliente(), new BigDecimal("-1.00")));
        Conta proxima = banco.abrirContaPoupanca(cliente());

        assertAll(
                () -> assertEquals(1001, proxima.getNumero()),
                () -> assertEquals(2, banco.listarContas().size()));
    }

    @Test
    void contaAbertaPodeSerBuscadaPorNumero() {
        Banco banco = new Banco(RELOGIO);
        Conta conta = banco.abrirContaPoupanca(cliente());

        assertAll(
                () -> assertEquals(Optional.of(conta), banco.buscarContaPorNumero(conta.getNumero())),
                () -> assertEquals(Optional.empty(), banco.buscarContaPorNumero(-1)));
    }

    @Test
    void listagemMantemOrdemDeAberturaENaoPermiteAlteracao() {
        Banco banco = new Banco(RELOGIO);
        Conta primeira = banco.abrirContaPoupanca(cliente());
        List<Conta> contas = banco.listarContas();

        assertThrows(UnsupportedOperationException.class, contas::clear);
        Conta segunda = banco.abrirContaPoupanca(cliente());

        assertAll(
                () -> assertEquals(List.of(primeira), contas),
                () -> assertEquals(List.of(primeira, segunda), banco.listarContas()));
    }

    @Test
    void bancoExigeRelogio() {
        assertThrows(NullPointerException.class, () -> new Banco(null));
    }

    private static Cliente cliente() {
        return new Cliente("Cliente", "246.813.579-28");
    }
}
