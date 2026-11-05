package com.github.amandadamata.sistemabancario;

import com.github.amandadamata.sistemabancario.persistencia.RepositorioSqlite;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Executa o menu com entradas simuladas, como uma pessoa digitando no terminal. */
class MainTest {

    @Test
    void fluxoCompletoComValoresEmFormatoBrasileiro() {
        String saida = executar(
                "1", "Ana Teste", "246.813.579-28", "2", // conta poupança 1000
                "1", "Bruno Teste", "135.792.468-28", "1", "100,00", // conta corrente 1001
                "2", "1000", "0,10",
                "2", "1000", "0,20",
                "4", "1000", "1001", "0,05",
                "5", "1000",
                "0");

        assertAll(
                () -> assertTrue(saida.contains("Account created! Number: 1000"), saida),
                () -> assertTrue(saida.contains("Account created! Number: 1001"), saida),
                () -> assertTrue(saida.contains("Transfer completed!"), saida),
                () -> assertTrue(saida.contains("Transfer sent (to account 1001)"), saida),
                () -> assertTrue(saida.contains("Current balance: R$ 0,25"), saida));
    }

    @Test
    void rendimentoRepetidoNoMesmoMesEhRecusado() {
        String saida = executar(
                "1", "Ana Teste", "246.813.579-28", "2",
                "2", "1000", "1000",
                "7", "1000",
                "7", "1000",
                "0");

        assertAll(
                () -> assertTrue(saida.contains("Interest of R$ 5,00 applied to account 1000."), saida),
                () -> assertTrue(saida.contains("has already been applied to this account"), saida));
    }

    @Test
    void entradasInvalidasPedemNovoValorSemEncerrarOMenu() {
        String saida = executar(
                "abc",
                "1", "Ana Teste", "246.813.579-28", "2",
                "2", "1000", "dez reais", "10,999", "10,99",
                "3", "1000", "0",
                "2", "9999",
                "0");

        assertAll(
                () -> assertTrue(saida.contains("Invalid input. Enter a whole number"), saida),
                () -> assertTrue(saida.contains("Invalid amount."), saida),
                () -> assertTrue(saida.contains("Deposit completed!"), saida),
                () -> assertTrue(saida.contains("Error: The withdrawal amount must be greater than zero."), saida),
                () -> assertTrue(saida.contains("Account not found."), saida),
                () -> assertTrue(saida.contains("Shutting down. Goodbye!"), saida));
    }

    @Test
    void operacoesRecusadasExtratoVazioEListagem() {
        String saida = executar(
                "6",                                                   // listar sem contas
                "1", "Ana Teste", "246.813.579-28", "1", "50",         // corrente 1000, limite 50
                "1", "Bruno Teste", "135.792.468-28", "2",             // poupança 1001
                "5", "1001",                                           // extrato vazio
                "3", "1000", "50,01",                                  // saque acima do disponível
                "7", "1000",                                           // rendimento em corrente
                "7", "1001",                                           // rendimento com saldo zero
                "4", "1000", "1000", "10",                             // transferência para si mesma
                "6",
                "5", "1000",
                "0");

        assertAll(
                () -> assertTrue(saida.contains("No accounts yet."), saida),
                () -> assertTrue(saida.contains("No transactions yet."), saida),
                () -> assertTrue(saida.contains("Error: Insufficient balance. Available for withdrawal: R$ 50,00"), saida),
                () -> assertTrue(saida.contains("This operation is only available for savings accounts."), saida),
                () -> assertTrue(saida.contains("The current balance doesn't earn at least R$ 0,01 in interest."), saida),
                () -> assertTrue(saida.contains("Error: You can't transfer to the same account."), saida),
                () -> assertTrue(saida.contains("No. 1001 | Savings Account  | Holder: Bruno Teste"), saida),
                () -> assertTrue(saida.contains("Overdraft limit: R$ 50,00 | Available: R$ 50,00"), saida));
    }

    @Test
    void cpfInvalidoEPedidoDeNovoSemRepetirONumero() {
        String saida = executar(
                "1", "Ana Teste", "123", "246.813.579-29", "246.813.579-28", "2",
                "0");

        assertAll(
                () -> assertTrue(saida.contains("The CPF must have 11 digits."), saida),
                () -> assertTrue(saida.contains("Invalid CPF: the check digits don't match."), saida),
                () -> assertTrue(saida.contains("Account created! Number: 1000"), saida),
                () -> assertFalse(saida.contains("246.813.579"), saida));
    }

    @Test
    void cpfDeOutroTitularERecusado() {
        String saida = executar(
                "1", "Ana Teste", "246.813.579-28", "2",
                "1", "Outra Pessoa", "24681357928", "2",
                "0");

        assertTrue(saida.contains("Error: This CPF is already registered to another holder."), saida);
    }

    @Test
    void dadosSobrevivemAoReinicioDoPrograma(@TempDir Path pasta) {
        Path arquivo = pasta.resolve("banco.db");

        executar(Banco.carregar(RELOGIO, new RepositorioSqlite(arquivo)),
                "1", "Ana Teste", "246.813.579-28", "2",
                "2", "1000", "250,75",
                "0");
        String segundaExecucao = executar(Banco.carregar(RELOGIO, new RepositorioSqlite(arquivo)),
                "6",
                "5", "1000",
                "0");

        assertAll(
                () -> assertTrue(segundaExecucao.contains("No. 1000 | Savings Account  | Holder: Ana Teste"), segundaExecucao),
                () -> assertTrue(segundaExecucao.contains("Current balance: R$ 250,75"), segundaExecucao),
                () -> assertTrue(segundaExecucao.contains("Deposit"), segundaExecucao));
    }

    @Test
    void arquivoDeDadosPadraoOuConfigurado() {
        assertAll(
                () -> assertEquals(Path.of("dados", "sistema-bancario.db"), Main.resolverArquivo(null)),
                () -> assertEquals(Path.of("dados", "sistema-bancario.db"), Main.resolverArquivo("  ")),
                () -> assertEquals(Path.of("/tmp/x.db"), Main.resolverArquivo(" /tmp/x.db ")));
    }

    @Test
    void fimDaEntradaEncerraComSeguranca() {
        String saida = executar("1", "Ana Teste");

        assertTrue(saida.contains("Input ended. Shutting down safely."), saida);
    }

    private static final RelogioAjustavel RELOGIO = RelogioAjustavel.em("2026-10-20T13:00:00Z");

    private static String executar(String... linhas) {
        return executar(new Banco(RELOGIO), linhas);
    }

    private static String executar(Banco banco, String... linhas) {
        byte[] entrada = (String.join("\n", linhas) + "\n").getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream bytesSaida = new ByteArrayOutputStream();
        PrintStream saida = new PrintStream(bytesSaida, true, StandardCharsets.UTF_8);

        new Main(banco, new ByteArrayInputStream(entrada), saida).executar();

        return bytesSaida.toString(StandardCharsets.UTF_8);
    }
}
