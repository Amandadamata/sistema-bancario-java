package com.github.amandadamata.sistemabancario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContaPoupancaTest {

    private RelogioAjustavel relogio;
    private ContaPoupanca conta;

    @BeforeEach
    void abrirConta() {
        relogio = RelogioAjustavel.em("2026-10-20T13:00:00Z");
        conta = new Banco(relogio).abrirContaPoupanca(new Cliente("Cliente", "000.000.000-00"));
    }

    @Test
    void rendimentoCreditaMeioPorCentoDoSaldo() {
        conta.depositar(valor("1000.00"));

        BigDecimal rendimento = conta.aplicarRendimento();

        assertAll(
                () -> assertEquals(valor("5.00"), rendimento),
                () -> assertEquals(valor("1005.00"), conta.getSaldo()),
                () -> assertEquals(TipoTransacao.RENDIMENTO,
                        conta.getHistorico().get(1).tipo()));
    }

    @Test
    void rendimentoSoPodeSerAplicadoUmaVezPorMes() {
        conta.depositar(valor("1000.00"));
        conta.aplicarRendimento();

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                conta::aplicarRendimento);

        assertAll(
                () -> assertEquals(valor("1005.00"), conta.getSaldo()),
                () -> assertEquals(2, conta.getHistorico().size()),
                () -> assertTrue(erro.getMessage().contains("10/2026"), erro.getMessage()));
    }

    @Test
    void rendimentoVoltaAFicarDisponivelNoMesSeguinte() {
        conta.depositar(valor("1000.00"));
        conta.aplicarRendimento();
        relogio.avancar(Duration.ofDays(12)); // 01/11/2026

        BigDecimal rendimento = conta.aplicarRendimento();

        assertAll(
                // 0,5% de 1005,00 = 5,025; HALF_EVEN arredonda o empate para o centavo par (5,02).
                () -> assertEquals(valor("5.02"), rendimento),
                () -> assertEquals(valor("1010.02"), conta.getSaldo()));
    }

    @Test
    void empateNoArredondamentoVaiParaOCentavoPar() {
        conta.depositar(valor("101.00")); // 0,5% = 0,505

        BigDecimal rendimento = conta.aplicarRendimento();

        assertEquals(valor("0.50"), rendimento);
    }

    @Test
    void saldoPequenoDemaisNaoGeraLancamentoNemConsomeOMes() {
        conta.depositar(valor("0.50")); // 0,5% = 0,0025

        BigDecimal rendimentoInicial = conta.aplicarRendimento();
        conta.depositar(valor("199.50"));
        BigDecimal rendimentoDepoisDoDeposito = conta.aplicarRendimento();

        assertAll(
                () -> assertEquals(valor("0.00"), rendimentoInicial),
                () -> assertEquals(valor("1.00"), rendimentoDepoisDoDeposito),
                () -> assertEquals(3, conta.getHistorico().size()));
    }

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }
}
