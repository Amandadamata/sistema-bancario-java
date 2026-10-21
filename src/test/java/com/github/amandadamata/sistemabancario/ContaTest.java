package com.github.amandadamata.sistemabancario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContaTest {

    private RelogioAjustavel relogio;
    private Banco banco;

    @BeforeEach
    void criarBanco() {
        relogio = RelogioAjustavel.em("2026-10-20T13:00:00Z");
        banco = new Banco(relogio);
    }

    @Test
    void somaDeCentavosEExata() {
        ContaPoupanca conta = novaPoupanca();

        conta.depositar(valor("0.10"));
        conta.depositar(valor("0.20"));

        // Com double, o saldo seria 0.30000000000000004.
        assertEquals(valor("0.30"), conta.getSaldo());
    }

    @Test
    void saldoSempreTemDuasCasasDecimais() {
        ContaPoupanca conta = novaPoupanca();

        conta.depositar(new BigDecimal("150"));

        assertEquals("150.00", conta.getSaldo().toPlainString());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "0.00", "-10.00", "0.001", "10.005"})
    void operacoesComValorInvalidoSaoRecusadasSemAlterarSaldos(BigDecimal valorInvalido) {
        ContaPoupanca origem = novaPoupanca();
        ContaPoupanca destino = novaPoupanca();
        origem.depositar(valor("100.00"));

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> origem.depositar(valorInvalido)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> origem.sacar(valorInvalido)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> origem.transferir(destino, valorInvalido)),
                () -> assertEquals(valor("100.00"), origem.getSaldo()),
                () -> assertEquals(valor("0.00"), destino.getSaldo()),
                () -> assertEquals(1, origem.getHistorico().size()));
    }

    @Test
    void saqueValidoAtualizaSaldo() throws SaldoInsuficienteException {
        ContaPoupanca conta = novaPoupanca();
        conta.depositar(valor("200.00"));

        conta.sacar(valor("75.25"));

        assertEquals(valor("124.75"), conta.getSaldo());
    }

    @Test
    void saqueDeTodoOSaldoEPermitido() throws SaldoInsuficienteException {
        ContaPoupanca conta = novaPoupanca();
        conta.depositar(valor("100.00"));

        conta.sacar(valor("100.00"));

        assertEquals(valor("0.00"), conta.getSaldo());
    }

    @Test
    void saqueComSaldoInsuficienteMantemSaldoEInformaODisponivel() {
        ContaPoupanca conta = novaPoupanca();
        conta.depositar(valor("100.00"));

        SaldoInsuficienteException erro = assertThrows(SaldoInsuficienteException.class,
                () -> conta.sacar(valor("100.01")));

        assertAll(
                () -> assertEquals(valor("100.00"), conta.getSaldo()),
                () -> assertTrue(erro.getMessage().contains("R$ 100,00"), erro.getMessage()));
    }

    @Test
    void contaCorrentePermiteUsarChequeEspecialAteOLimite() throws SaldoInsuficienteException {
        ContaCorrente conta = banco.abrirContaCorrente(cliente("Cliente"), valor("200.00"));
        conta.depositar(valor("100.00"));

        conta.sacar(valor("250.00"));

        assertAll(
                () -> assertEquals(valor("-150.00"), conta.getSaldo()),
                () -> assertEquals(valor("50.00"), conta.getSaldoDisponivel()));
        assertThrows(SaldoInsuficienteException.class, () -> conta.sacar(valor("50.01")));
        assertEquals(valor("-150.00"), conta.getSaldo());
    }

    @Test
    void transferenciaPodeUsarChequeEspecial() throws SaldoInsuficienteException {
        ContaCorrente origem = banco.abrirContaCorrente(cliente("Origem"), valor("100.00"));
        ContaPoupanca destino = novaPoupanca();

        origem.transferir(destino, valor("100.00"));

        assertAll(
                () -> assertEquals(valor("-100.00"), origem.getSaldo()),
                () -> assertEquals(valor("100.00"), destino.getSaldo()));
        assertThrows(SaldoInsuficienteException.class,
                () -> origem.transferir(destino, valor("0.01")));
    }

    @Test
    void transferenciaBemSucedidaAtualizaOsDoisSaldos() throws SaldoInsuficienteException {
        ContaCorrente origem = banco.abrirContaCorrente(cliente("Origem"), valor("0.00"));
        ContaPoupanca destino = novaPoupanca();
        origem.depositar(valor("300.00"));

        origem.transferir(destino, valor("125.50"));

        assertAll(
                () -> assertEquals(valor("174.50"), origem.getSaldo()),
                () -> assertEquals(valor("125.50"), destino.getSaldo()));
    }

    @Test
    void transferenciaSemSaldoNaoAlteraNenhumaConta() {
        ContaPoupanca origem = novaPoupanca();
        ContaPoupanca destino = novaPoupanca();
        origem.depositar(valor("100.00"));
        destino.depositar(valor("50.00"));

        assertThrows(SaldoInsuficienteException.class,
                () -> origem.transferir(destino, valor("100.01")));

        assertAll(
                () -> assertEquals(valor("100.00"), origem.getSaldo()),
                () -> assertEquals(valor("50.00"), destino.getSaldo()),
                () -> assertEquals(1, origem.getHistorico().size()),
                () -> assertEquals(1, destino.getHistorico().size()));
    }

    @Test
    void transferenciaComDestinoInvalidoMantemSaldo() {
        ContaPoupanca conta = novaPoupanca();
        conta.depositar(valor("100.00"));

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> conta.transferir(conta, valor("50.00"))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> conta.transferir(null, valor("50.00"))),
                () -> assertEquals(valor("100.00"), conta.getSaldo()));
    }

    @Test
    void historicoRegistraTipoValorSaldoDataEContaRelacionada() throws SaldoInsuficienteException {
        ContaCorrente origem = banco.abrirContaCorrente(cliente("Origem"), valor("0.00"));
        ContaPoupanca destino = novaPoupanca();
        origem.depositar(valor("500.00"));
        origem.sacar(valor("120.00"));
        origem.transferir(destino, valor("80.00"));
        LocalDateTime agora = LocalDateTime.of(2026, 10, 20, 13, 0);

        List<Transacao> historico = origem.getHistorico();

        assertEquals(List.of(
                new Transacao(TipoTransacao.DEPOSITO, valor("500.00"), valor("500.00"),
                        agora, OptionalInt.empty()),
                new Transacao(TipoTransacao.SAQUE, valor("120.00"), valor("380.00"),
                        agora, OptionalInt.empty()),
                new Transacao(TipoTransacao.TRANSFERENCIA_ENVIADA, valor("80.00"), valor("300.00"),
                        agora, OptionalInt.of(destino.getNumero()))),
                historico);
        assertEquals(List.of(new Transacao(TipoTransacao.TRANSFERENCIA_RECEBIDA, valor("80.00"),
                        valor("80.00"), agora, OptionalInt.of(origem.getNumero()))),
                destino.getHistorico());
    }

    @Test
    void valorComSinalDiferenciaCreditoDeDebito() throws SaldoInsuficienteException {
        ContaPoupanca conta = novaPoupanca();
        conta.depositar(valor("10.00"));
        conta.sacar(valor("3.00"));

        List<BigDecimal> valores = conta.getHistorico().stream()
                .map(Transacao::valorComSinal)
                .toList();

        assertEquals(List.of(valor("10.00"), valor("-3.00")), valores);
    }

    @Test
    void historicoRetornadoNaoPodeSerAlterado() {
        ContaPoupanca conta = novaPoupanca();
        conta.depositar(valor("10.00"));

        List<Transacao> historico = conta.getHistorico();

        assertThrows(UnsupportedOperationException.class, historico::clear);
        assertEquals(1, conta.getHistorico().size());
    }

    @Test
    void tiposDeContaSaoIdentificadosCorretamente() {
        Conta corrente = banco.abrirContaCorrente(cliente("Corrente"), valor("100.00"));
        Conta poupanca = novaPoupanca();

        assertAll(
                () -> assertEquals("Conta Corrente", corrente.getTipoConta()),
                () -> assertEquals("Conta Poupança", poupanca.getTipoConta()));
    }

    @Test
    void contaSemTitularERecusada() {
        assertThrows(NullPointerException.class, () -> banco.abrirContaPoupanca(null));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-0.01", "10.001"})
    void limiteDoChequeEspecialInvalidoERecusado(BigDecimal limite) {
        assertThrows(IllegalArgumentException.class,
                () -> banco.abrirContaCorrente(cliente("Cliente"), limite));
    }

    private ContaPoupanca novaPoupanca() {
        return banco.abrirContaPoupanca(cliente("Cliente"));
    }

    private static Cliente cliente(String nome) {
        return new Cliente(nome, "000.000.000-00");
    }

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }
}
