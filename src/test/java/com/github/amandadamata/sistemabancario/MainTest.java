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
                () -> assertTrue(saida.contains("Conta criada com sucesso! Número: 1000"), saida),
                () -> assertTrue(saida.contains("Conta criada com sucesso! Número: 1001"), saida),
                () -> assertTrue(saida.contains("Transferência realizada com sucesso!"), saida),
                () -> assertTrue(saida.contains("Transferência enviada (para a conta 1001)"), saida),
                () -> assertTrue(saida.contains("Saldo atual: R$ 0,25"), saida));
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
                () -> assertTrue(saida.contains("Rendimento de R$ 5,00 aplicado à conta 1000."), saida),
                () -> assertTrue(saida.contains("já foi aplicado a esta conta"), saida));
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
                () -> assertTrue(saida.contains("Entrada inválida. Digite um número inteiro"), saida),
                () -> assertTrue(saida.contains("Valor inválido."), saida),
                () -> assertTrue(saida.contains("Depósito realizado com sucesso!"), saida),
                () -> assertTrue(saida.contains("Erro: O valor do saque deve ser maior que zero."), saida),
                () -> assertTrue(saida.contains("Conta não encontrada."), saida),
                () -> assertTrue(saida.contains("Encerrando o sistema. Até logo!"), saida));
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
                () -> assertTrue(saida.contains("Nenhuma conta cadastrada."), saida),
                () -> assertTrue(saida.contains("Nenhuma movimentação registrada ainda."), saida),
                () -> assertTrue(saida.contains("Erro: Saldo insuficiente. Disponível para saque: R$ 50,00"), saida),
                () -> assertTrue(saida.contains("Essa operação só está disponível para contas poupança."), saida),
                () -> assertTrue(saida.contains("O saldo atual não gera rendimento de pelo menos R$ 0,01."), saida),
                () -> assertTrue(saida.contains("Erro: Não é possível transferir para a mesma conta."), saida),
                () -> assertTrue(saida.contains("Nº 1001 | Conta Poupança | Titular: Bruno Teste"), saida),
                () -> assertTrue(saida.contains("Limite do cheque especial: R$ 50,00 | Disponível: R$ 50,00"), saida));
    }

    @Test
    void cpfInvalidoEPedidoDeNovoSemRepetirONumero() {
        String saida = executar(
                "1", "Ana Teste", "123", "246.813.579-29", "246.813.579-28", "2",
                "0");

        assertAll(
                () -> assertTrue(saida.contains("O CPF deve ter 11 dígitos."), saida),
                () -> assertTrue(saida.contains("CPF inválido: os dígitos verificadores não conferem."), saida),
                () -> assertTrue(saida.contains("Conta criada com sucesso! Número: 1000"), saida),
                () -> assertFalse(saida.contains("246.813.579"), saida));
    }

    @Test
    void cpfDeOutroTitularERecusado() {
        String saida = executar(
                "1", "Ana Teste", "246.813.579-28", "2",
                "1", "Outra Pessoa", "24681357928", "2",
                "0");

        assertTrue(saida.contains("Erro: Este CPF já está cadastrado para outro titular."), saida);
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
                () -> assertTrue(segundaExecucao.contains("Nº 1000 | Conta Poupança | Titular: Ana Teste"), segundaExecucao),
                () -> assertTrue(segundaExecucao.contains("Saldo atual: R$ 250,75"), segundaExecucao),
                () -> assertTrue(segundaExecucao.contains("Depósito"), segundaExecucao));
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

        assertTrue(saida.contains("Entrada encerrada. Encerrando o sistema com segurança."), saida);
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
