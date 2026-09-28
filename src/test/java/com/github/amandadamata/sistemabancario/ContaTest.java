package com.github.amandadamata.sistemabancario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContaTest {

    private static final double MARGEM_ERRO = 0.001;

    @Test
    void depositoValidoAtualizaSaldo() {
        ContaPoupanca conta = novaContaPoupanca("Cliente");

        conta.depositar(150.75);

        assertEquals(150.75, conta.getSaldo(), MARGEM_ERRO);
    }

    @ParameterizedTest
    @MethodSource("valoresInvalidos")
    void operacoesComValorInvalidoSaoRecusadas(double valor) {
        ContaPoupanca origem = novaContaPoupanca("Origem");
        ContaPoupanca destino = novaContaPoupanca("Destino");
        origem.depositar(100.00);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> origem.depositar(valor)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> origem.sacar(valor)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> origem.transferir(destino, valor)),
                () -> assertEquals(100.00, origem.getSaldo(), MARGEM_ERRO),
                () -> assertEquals(0.00, destino.getSaldo(), MARGEM_ERRO));
    }

    @Test
    void saqueValidoAtualizaSaldo() throws SaldoInsuficienteException {
        ContaPoupanca conta = novaContaPoupanca("Cliente");
        conta.depositar(200.00);

        conta.sacar(75.25);

        assertEquals(124.75, conta.getSaldo(), MARGEM_ERRO);
    }

    @Test
    void saqueComSaldoInsuficienteMantemSaldo() {
        ContaPoupanca conta = novaContaPoupanca("Cliente");
        conta.depositar(100.00);

        assertThrows(SaldoInsuficienteException.class, () -> conta.sacar(100.01));
        assertEquals(100.00, conta.getSaldo(), MARGEM_ERRO);
    }

    @Test
    void contaCorrentePermiteUsarChequeEspecialAteOLimite()
            throws SaldoInsuficienteException {
        ContaCorrente conta = new ContaCorrente(novoCliente("Cliente"), 200.00);
        conta.depositar(100.00);

        conta.sacar(250.00);

        assertEquals(-150.00, conta.getSaldo(), MARGEM_ERRO);
        assertThrows(SaldoInsuficienteException.class, () -> conta.sacar(50.01));
        assertEquals(-150.00, conta.getSaldo(), MARGEM_ERRO);
    }

    @Test
    void transferenciaBemSucedidaAtualizaOsDoisSaldos() throws SaldoInsuficienteException {
        ContaCorrente origem = new ContaCorrente(novoCliente("Origem"), 0);
        ContaPoupanca destino = novaContaPoupanca("Destino");
        origem.depositar(300.00);

        origem.transferir(destino, 125.50);

        assertAll(
                () -> assertEquals(174.50, origem.getSaldo(), MARGEM_ERRO),
                () -> assertEquals(125.50, destino.getSaldo(), MARGEM_ERRO));
    }

    @Test
    void transferenciaSemSaldoMantemOsDoisSaldos() {
        ContaPoupanca origem = novaContaPoupanca("Origem");
        ContaPoupanca destino = novaContaPoupanca("Destino");
        origem.depositar(100.00);
        destino.depositar(50.00);

        assertThrows(SaldoInsuficienteException.class,
                () -> origem.transferir(destino, 100.01));

        assertAll(
                () -> assertEquals(100.00, origem.getSaldo(), MARGEM_ERRO),
                () -> assertEquals(50.00, destino.getSaldo(), MARGEM_ERRO));
    }

    @Test
    void transferenciaComDestinoInvalidoMantemSaldo() {
        ContaPoupanca conta = novaContaPoupanca("Cliente");
        conta.depositar(100.00);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> conta.transferir(conta, 50.00)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> conta.transferir(null, 50.00)),
                () -> assertEquals(100.00, conta.getSaldo(), MARGEM_ERRO));
    }

    @Test
    void rendimentoDaPoupancaCalculaMeioPorCento() {
        ContaPoupanca conta = novaContaPoupanca("Cliente");
        conta.depositar(1000.00);

        double rendimento = conta.aplicarRendimento();

        assertAll(
                () -> assertEquals(5.00, rendimento, MARGEM_ERRO),
                () -> assertEquals(1005.00, conta.getSaldo(), MARGEM_ERRO));
    }

    @Test
    void depositoQueUltrapassaFaixaDoDoubleERecusado() {
        ContaPoupanca conta = novaContaPoupanca("Cliente");
        conta.depositar(Double.MAX_VALUE);

        assertThrows(IllegalArgumentException.class, () -> conta.depositar(Double.MAX_VALUE));
        assertTrue(Double.isFinite(conta.getSaldo()));
        assertEquals(Double.MAX_VALUE, conta.getSaldo());
    }

    @Test
    void tiposDeContaSaoIdentificadosCorretamente() {
        Conta corrente = new ContaCorrente(novoCliente("Corrente"), 100.00);
        Conta poupanca = novaContaPoupanca("Poupança");

        assertAll(
                () -> assertEquals("Conta Corrente", corrente.getTipoConta()),
                () -> assertEquals("Conta Poupança", poupanca.getTipoConta()));
    }

    @Test
    void contaSemTitularERecusada() {
        assertThrows(NullPointerException.class, () -> new ContaPoupanca(null));
    }

    private static Stream<Double> valoresInvalidos() {
        return Stream.of(0.0, -10.0, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }

    private static ContaPoupanca novaContaPoupanca(String nome) {
        return new ContaPoupanca(novoCliente(nome));
    }

    private static Cliente novoCliente(String nome) {
        return new Cliente(nome, "000.000.000-00");
    }
}
