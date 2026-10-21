package com.github.amandadamata.sistemabancario;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Executa o menu com entradas simuladas, como uma pessoa digitando no terminal. */
class MainTest {

    @Test
    void fluxoCompletoComValoresEmFormatoBrasileiro() {
        String saida = executar(
                "1", "Ana Teste", "000.000.000-00", "2", // conta poupança 1000
                "1", "Bruno Teste", "000.000.000-00", "1", "100,00", // conta corrente 1001
                "2", "1000", "0,10",
                "2", "1000", "0,20",
                "4", "1000", "1001", "0,05",
                "5", "1000",
                "0");

        assertAll(
                () -> assertTrue(saida.contains("Conta criada com sucesso! Número: 1000"), saida),
                () -> assertTrue(saida.contains("Conta criada com sucesso! Número: 1001"), saida),
                () -> assertTrue(saida.contains("Transferência realizada com sucesso!"), saida),
                () -> assertTrue(saida.contains("Transferência enviada (para a conta 1001)"), saida),
                () -> assertTrue(saida.contains("Saldo atual: R$ 0,25"), saida));
    }

    @Test
    void rendimentoRepetidoNoMesmoMesEhRecusado() {
        String saida = executar(
                "1", "Ana Teste", "000.000.000-00", "2",
                "2", "1000", "1000",
                "7", "1000",
                "7", "1000",
                "0");

        assertAll(
                () -> assertTrue(saida.contains("Rendimento de R$ 5,00 aplicado à conta 1000."), saida),
                () -> assertTrue(saida.contains("já foi aplicado a esta conta"), saida));
    }

    @Test
    void entradasInvalidasPedemNovoValorSemEncerrarOMenu() {
        String saida = executar(
                "abc",
                "1", "Ana Teste", "000.000.000-00", "2",
                "2", "1000", "dez reais", "10,999", "10,99",
                "3", "1000", "0",
                "2", "9999",
                "0");

        assertAll(
                () -> assertTrue(saida.contains("Entrada inválida. Digite um número inteiro"), saida),
                () -> assertTrue(saida.contains("Valor inválido."), saida),
                () -> assertTrue(saida.contains("Depósito realizado com sucesso!"), saida),
                () -> assertTrue(saida.contains("Erro: O valor do saque deve ser maior que zero."), saida),
                () -> assertTrue(saida.contains("Conta não encontrada."), saida),
                () -> assertTrue(saida.contains("Encerrando o sistema. Até logo!"), saida));
    }

    @Test
    void fimDaEntradaEncerraComSeguranca() {
        String saida = executar("1", "Ana Teste");

        assertTrue(saida.contains("Entrada encerrada. Encerrando o sistema com segurança."), saida);
    }

    private static String executar(String... linhas) {
        byte[] entrada = (String.join("\n", linhas) + "\n").getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream bytesSaida = new ByteArrayOutputStream();
        PrintStream saida = new PrintStream(bytesSaida, true, StandardCharsets.UTF_8);
        RelogioAjustavel relogio = RelogioAjustavel.em("2026-10-20T13:00:00Z");

        new Main(new Banco(relogio), new ByteArrayInputStream(entrada), saida).executar();

        return bytesSaida.toString(StandardCharsets.UTF_8);
    }
}
