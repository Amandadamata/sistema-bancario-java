package com.github.amandadamata.sistemabancario.persistencia;

import com.github.amandadamata.sistemabancario.Banco;
import com.github.amandadamata.sistemabancario.Cliente;
import com.github.amandadamata.sistemabancario.Conta;
import com.github.amandadamata.sistemabancario.ContaCorrente;
import com.github.amandadamata.sistemabancario.ContaPoupanca;
import com.github.amandadamata.sistemabancario.FalhaPersistenciaException;
import com.github.amandadamata.sistemabancario.SaldoInsuficienteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de integração com um arquivo SQLite real em uma pasta temporária.
 *
 * <p>"Reiniciar" significa criar um novo {@link RepositorioSqlite} e um novo {@link Banco} sobre
 * o mesmo arquivo, exatamente como acontece quando o programa é fechado e aberto de novo.
 */
class RepositorioSqliteTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-10-20T13:00:00Z"), ZoneOffset.UTC);
    private static final Cliente ANA = new Cliente("Ana", "246.813.579-28");
    private static final Cliente BRUNO = new Cliente("Bruno", "135.792.468-28");

    @TempDir
    Path pasta;

    private Path arquivo;

    @BeforeEach
    void definirArquivo() {
        arquivo = pasta.resolve("dados/banco.db");
    }

    @Test
    void reiniciarPreservaClientesContasSaldosHistoricoERendimento() throws SaldoInsuficienteException {
        Banco banco = Banco.carregar(RELOGIO, new RepositorioSqlite(arquivo));
        ContaCorrente corrente = banco.abrirContaCorrente(ANA, valor("200.00"));
        ContaPoupanca poupanca = banco.abrirContaPoupanca(BRUNO);
        banco.depositar(poupanca.getNumero(), valor("1000.00"));
        banco.aplicarRendimento(poupanca.getNumero());
        banco.transferir(corrente.getNumero(), poupanca.getNumero(), valor("150.50"));

        Banco reiniciado = Banco.carregar(RELOGIO, new RepositorioSqlite(arquivo));

        Conta correnteLida = reiniciado.buscarContaPorNumero(1000).orElseThrow();
        Conta poupancaLida = reiniciado.buscarContaPorNumero(1001).orElseThrow();
        assertAll(
                () -> assertEquals(2, reiniciado.listarContas().size()),
                () -> assertEquals("Ana", correnteLida.getTitular().getNome()),
                () -> assertEquals(ANA.getCpf(), correnteLida.getTitular().getCpf()),
                () -> assertEquals(valor("-150.50"), correnteLida.getSaldo()),
                () -> assertEquals(valor("49.50"), correnteLida.getSaldoDisponivel()),
                () -> assertEquals(corrente.getHistorico(), correnteLida.getHistorico()),
                () -> assertEquals(valor("1155.50"), poupancaLida.getSaldo()),
                () -> assertEquals(poupanca.getHistorico(), poupancaLida.getHistorico()),
                () -> assertThrows(IllegalStateException.class, () -> reiniciado.aplicarRendimento(1001)),
                () -> assertEquals(1002, reiniciado.abrirContaPoupanca(ANA).getNumero()));
    }

    @Test
    void transferenciaInterrompidaNoMeioDaGravacaoNaoDeixaNenhumLadoGravado() throws Exception {
        Banco banco = Banco.carregar(RELOGIO, new RepositorioSqlite(arquivo));
        ContaPoupanca origem = banco.abrirContaPoupanca(ANA);
        ContaPoupanca destino = banco.abrirContaPoupanca(BRUNO);
        banco.depositar(origem.getNumero(), valor("100.00"));
        // Faz o banco de dados recusar o lançamento do destino, depois que a origem já foi gravada
        // dentro da mesma transação.
        executarSql("""
                CREATE TRIGGER falha_simulada BEFORE INSERT ON transacoes
                WHEN NEW.tipo = 'TRANSFERENCIA_RECEBIDA'
                BEGIN SELECT RAISE(ABORT, 'falha simulada'); END""");

        assertThrows(FalhaPersistenciaException.class,
                () -> banco.transferir(origem.getNumero(), destino.getNumero(), valor("40.00")));

        Banco reiniciado = Banco.carregar(RELOGIO, new RepositorioSqlite(arquivo));
        assertAll(
                () -> assertEquals(valor("100.00"), origem.getSaldo(), "memória da origem"),
                () -> assertEquals(valor("0.00"), destino.getSaldo(), "memória do destino"),
                () -> assertEquals(valor("100.00"), reiniciado.buscarContaPorNumero(1000).orElseThrow().getSaldo()),
                () -> assertEquals(valor("0.00"), reiniciado.buscarContaPorNumero(1001).orElseThrow().getSaldo()),
                () -> assertEquals(0, contarLinhas("SELECT COUNT(*) FROM transacoes WHERE tipo LIKE 'TRANSFERENCIA%'")));
    }

    @Test
    void valoresSaoGravadosComoCentavosInteiros() throws SQLException {
        Banco banco = Banco.carregar(RELOGIO, new RepositorioSqlite(arquivo));
        banco.abrirContaPoupanca(ANA);
        banco.depositar(1000, valor("10.50"));

        try (Connection conexao = conectar(); Statement consulta = conexao.createStatement();
             ResultSet linha = consulta.executeQuery(
                     "SELECT saldo_centavos, typeof(saldo_centavos) FROM contas WHERE numero = 1000")) {
            assertAll(
                    () -> assertEquals(1050, linha.getLong(1)),
                    () -> assertEquals("integer", linha.getString(2)));
        }
    }

    @Test
    void cpfEGravadoSemPontuacao() throws SQLException {
        Banco.carregar(RELOGIO, new RepositorioSqlite(arquivo)).abrirContaPoupanca(ANA);

        assertEquals(1, contarLinhas("SELECT COUNT(*) FROM clientes WHERE cpf = '24681357928'"));
    }

    @Test
    void saldoAlteradoForaDaAplicacaoEDetectadoAoCarregar() throws SQLException {
        Banco banco = Banco.carregar(RELOGIO, new RepositorioSqlite(arquivo));
        banco.abrirContaPoupanca(ANA);
        banco.depositar(1000, valor("10.00"));
        executarSql("UPDATE contas SET saldo_centavos = 999999 WHERE numero = 1000");

        FalhaPersistenciaException erro = assertThrows(FalhaPersistenciaException.class,
                () -> Banco.carregar(RELOGIO, new RepositorioSqlite(arquivo)));

        assertTrue(erro.getMessage().contains("o saldo gravado não corresponde ao histórico"), erro.getMessage());
    }

    @Test
    void lancamentoAlteradoForaDaAplicacaoEDetectadoAoCarregar() throws SQLException {
        Banco banco = Banco.carregar(RELOGIO, new RepositorioSqlite(arquivo));
        banco.abrirContaPoupanca(ANA);
        banco.depositar(1000, valor("10.00"));
        banco.depositar(1000, valor("5.00"));
        executarSql("UPDATE transacoes SET saldo_resultante_centavos = 2000 WHERE sequencia = 1");

        FalhaPersistenciaException erro = assertThrows(FalhaPersistenciaException.class,
                () -> Banco.carregar(RELOGIO, new RepositorioSqlite(arquivo)));

        assertTrue(erro.getMessage().contains("um lançamento não corresponde"), erro.getMessage());
    }

    @Test
    void tipoDeLancamentoDesconhecidoImpedeOCarregamento() throws SQLException {
        Banco banco = Banco.carregar(RELOGIO, new RepositorioSqlite(arquivo));
        banco.abrirContaPoupanca(ANA);
        banco.depositar(1000, valor("10.00"));
        executarSql("UPDATE transacoes SET tipo = 'ESTORNO' WHERE sequencia = 1");

        FalhaPersistenciaException erro = assertThrows(FalhaPersistenciaException.class,
                () -> new RepositorioSqlite(arquivo).carregar());

        assertTrue(erro.getMessage().contains("O arquivo não foi alterado"), erro.getMessage());
    }

    @Test
    void restricoesDoBancoDeDadosRecusamDadosInvalidos() throws SQLException {
        new RepositorioSqlite(arquivo);

        assertAll(
                () -> assertThrows(SQLException.class, () -> executarSql(
                        "INSERT INTO contas VALUES (1000, 'CORRENTE', '24681357928', 0, 0, NULL)")),
                () -> assertThrows(SQLException.class, () -> executarSql(
                        "INSERT INTO clientes VALUES ('123', 'CPF curto')")),
                () -> assertThrows(SQLException.class, () -> {
                    executarSql("INSERT INTO clientes VALUES ('24681357928', 'Ana')");
                    executarSql("INSERT INTO contas VALUES (1000, 'POUPANCA', '24681357928', 0, 500, NULL)");
                }));
    }

    /** O teste anterior usa uma conexão própria; este confere as conexões abertas pelo repositório. */
    @Test
    void conexoesDoRepositorioLigamAsChavesEstrangeiras() throws SQLException {
        RepositorioSqlite repositorio = new RepositorioSqlite(arquivo);

        try (Connection conexao = repositorio.abrirConexao(); Statement consulta = conexao.createStatement();
             ResultSet resultado = consulta.executeQuery("PRAGMA foreign_keys")) {
            assertEquals(1, resultado.getInt(1));
        }
    }

    @Test
    void bancoNovoRecebeOEsquemaNaVersaoAtual() throws SQLException {
        new RepositorioSqlite(arquivo);

        assertEquals(RepositorioSqlite.VERSAO_ESQUEMA, contarLinhas("PRAGMA user_version"));
    }

    @Test
    void esquemaDeUmaVersaoMaisNovaERecusado() throws SQLException {
        new RepositorioSqlite(arquivo);
        executarSql("PRAGMA user_version = 99");

        FalhaPersistenciaException erro = assertThrows(FalhaPersistenciaException.class,
                () -> new RepositorioSqlite(arquivo));

        assertTrue(erro.getMessage().contains("versão mais nova"), erro.getMessage());
    }

    @Test
    void arquivoQueNaoEUmBancoDeDadosERecusadoSemSerAlterado() throws IOException {
        Files.createDirectories(arquivo.getParent());
        Files.writeString(arquivo, "isto não é um banco de dados");

        assertThrows(FalhaPersistenciaException.class, () -> new RepositorioSqlite(arquivo));
        assertEquals("isto não é um banco de dados", Files.readString(arquivo));
    }

    @Test
    void caminhoInacessivelGeraFalhaClara() throws IOException {
        Path arquivoComum = Files.writeString(pasta.resolve("bloqueio"), "x");

        FalhaPersistenciaException erro = assertThrows(FalhaPersistenciaException.class,
                () -> new RepositorioSqlite(arquivoComum.resolve("banco.db")));

        assertTrue(erro.getMessage().contains("Não foi possível criar a pasta"), erro.getMessage());
    }

    @Test
    void conversaoDeCentavosEExataEmAmbosOsSentidos() {
        assertAll(
                () -> assertEquals(1234, RepositorioSqlite.emCentavos(valor("12.34"))),
                () -> assertEquals(-15050, RepositorioSqlite.emCentavos(valor("-150.50"))),
                () -> assertEquals(valor("12.34"), RepositorioSqlite.deCentavos(1234)),
                () -> assertThrows(ArithmeticException.class, () -> RepositorioSqlite.emCentavos(valor("0.001"))));
    }

    private Connection conectar() throws SQLException {
        Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
        try (Statement comando = conexao.createStatement()) {
            comando.execute("PRAGMA foreign_keys = ON");
        }
        return conexao;
    }

    /** Executa um único comando SQL em uma conexão própria. */
    private void executarSql(String sql) throws SQLException {
        try (Connection conexao = conectar(); Statement comando = conexao.createStatement()) {
            comando.execute(sql);
        }
    }

    private int contarLinhas(String sql) throws SQLException {
        try (Connection conexao = conectar(); Statement consulta = conexao.createStatement();
             ResultSet resultado = consulta.executeQuery(sql)) {
            return resultado.getInt(1);
        }
    }

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }
}
