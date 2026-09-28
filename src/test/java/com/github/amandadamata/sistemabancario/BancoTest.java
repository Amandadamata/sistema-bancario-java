package com.github.amandadamata.sistemabancario;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BancoTest {

    @Test
    void contaAdicionadaPodeSerBuscadaPorNumero() {
        Banco banco = new Banco();
        Conta conta = novaConta();

        banco.adicionarConta(conta);

        assertSame(conta, banco.buscarContaPorNumero(conta.getNumero()));
        assertNull(banco.buscarContaPorNumero(-1));
    }

    @Test
    void listagemNaoPermiteAlterarAsContasDoBanco() {
        Banco banco = new Banco();
        banco.adicionarConta(novaConta());
        List<Conta> contas = banco.listarContas();

        assertThrows(UnsupportedOperationException.class, contas::clear);
        banco.adicionarConta(novaConta());

        assertEquals(1, contas.size());
        assertEquals(2, banco.listarContas().size());
    }

    @Test
    void contaNulaNaoPodeSerAdicionada() {
        Banco banco = new Banco();

        assertThrows(NullPointerException.class, () -> banco.adicionarConta(null));
    }

    private static Conta novaConta() {
        return new ContaPoupanca(new Cliente("Cliente", "000.000.000-00"));
    }
}
