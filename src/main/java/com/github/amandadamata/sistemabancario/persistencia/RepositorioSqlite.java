package com.github.amandadamata.sistemabancario.persistencia;

import com.github.amandadamata.sistemabancario.ContaPersistida;
import com.github.amandadamata.sistemabancario.FalhaPersistenciaException;
import com.github.amandadamata.sistemabancario.RepositorioBanco;
import com.github.amandadamata.sistemabancario.TipoConta;
import com.github.amandadamata.sistemabancario.TipoTransacao;
import com.github.amandadamata.sistemabancario.Transacao;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Grava as contas em um arquivo SQLite usando JDBC.
 *
 * <p>Valores monetários são gravados como centavos inteiros ({@code INTEGER}). O tipo
 * {@code REAL} do SQLite é ponto flutuante binário e traria de volta os erros de arredondamento
 * que o domínio evita com {@code BigDecimal}.
 */
public final class RepositorioSqlite implements RepositorioBanco {

    /** Versão do esquema, guardada em {@code PRAGMA user_version}. */
    static final int VERSAO_ESQUEMA = 1;

    private static final String[] ESQUEMA = {
        """
        CREATE TABLE clientes (
            cpf  TEXT PRIMARY KEY CHECK (length(cpf) = 11),
            nome TEXT NOT NULL CHECK (length(trim(nome)) > 0)
        )""",
        """
        CREATE TABLE contas (
            numero                  INTEGER PRIMARY KEY CHECK (numero > 0),
            tipo                    TEXT    NOT NULL CHECK (tipo IN ('CORRENTE', 'POUPANCA')),
            cliente_cpf             TEXT    NOT NULL REFERENCES clientes (cpf),
            saldo_centavos          INTEGER NOT NULL,
            limite_centavos         INTEGER CHECK (limite_centavos >= 0),
            ultimo_mes_rendimento   TEXT,
            CHECK ((tipo = 'CORRENTE' AND limite_centavos IS NOT NULL)
                OR (tipo = 'POUPANCA' AND limite_centavos IS NULL))
        )""",
        """
        CREATE TABLE transacoes (
            conta_numero               INTEGER NOT NULL REFERENCES contas (numero),
            sequencia                  INTEGER NOT NULL CHECK (sequencia > 0),
            tipo                       TEXT    NOT NULL,
            valor_centavos             INTEGER NOT NULL CHECK (valor_centavos > 0),
            saldo_resultante_centavos  INTEGER NOT NULL,
            data_hora                  TEXT    NOT NULL,
            conta_relacionada          INTEGER,
            PRIMARY KEY (conta_numero, sequencia)
        )"""
    };

    private final Path arquivo;

    /**
     * Abre (ou cria) o banco de dados e confere a versão do esquema.
     *
     * @throws FalhaPersistenciaException se o arquivo não puder ser aberto ou for de uma versão
     *                                    mais nova que a aplicação
     */
    public RepositorioSqlite(Path arquivo) {
        this.arquivo = Objects.requireNonNull(arquivo, "O arquivo do banco de dados é obrigatório.");
        prepararArquivo();
    }

    @Override
    public List<ContaPersistida> carregar() {
        String sql = """
                SELECT c.numero, c.tipo, c.saldo_centavos, c.limite_centavos,
                       c.ultimo_mes_rendimento, cl.nome, cl.cpf
                FROM contas c JOIN clientes cl ON cl.cpf = c.cliente_cpf
                ORDER BY c.numero""";
        try (Connection conexao = abrirConexao();
             Statement consulta = conexao.createStatement();
             ResultSet linhas = consulta.executeQuery(sql)) {
            List<ContaPersistida> contas = new ArrayList<>();
            while (linhas.next()) {
                int numero = linhas.getInt("numero");
                String mes = linhas.getString("ultimo_mes_rendimento");
                long limite = linhas.getLong("limite_centavos");
                boolean semLimite = linhas.wasNull();
                contas.add(new ContaPersistida(
                        numero,
                        TipoConta.valueOf(linhas.getString("tipo")),
                        linhas.getString("nome"),
                        linhas.getString("cpf"),
                        semLimite ? null : deCentavos(limite),
                        deCentavos(linhas.getLong("saldo_centavos")),
                        mes == null ? null : YearMonth.parse(mes),
                        carregarHistorico(conexao, numero)));
            }
            return contas;
        } catch (SQLException | RuntimeException falha) {
            throw new FalhaPersistenciaException(
                    "Não foi possível ler os dados de " + arquivo + ". O arquivo não foi alterado.", falha);
        }
    }

    /**
     * Grava as contas em uma única transação do banco de dados.
     *
     * <p>Se qualquer comando falhar, {@code rollback()} desfaz todos os anteriores: em uma
     * transferência, a origem e o destino ficam gravados juntos ou nenhum dos dois.
     */
    @Override
    public void salvar(List<ContaPersistida> contas) {
        try (Connection conexao = abrirConexao()) {
            conexao.setAutoCommit(false);
            try {
                for (ContaPersistida conta : contas) {
                    gravarCliente(conexao, conta);
                    gravarConta(conexao, conta);
                    gravarTransacoesNovas(conexao, conta);
                }
                conexao.commit();
            } catch (SQLException | RuntimeException falha) {
                conexao.rollback();
                throw falha;
            }
        } catch (SQLException | RuntimeException falha) {
            throw new FalhaPersistenciaException("Não foi possível gravar os dados em " + arquivo + ".", falha);
        }
    }

    private void prepararArquivo() {
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
        } catch (IOException falha) {
            throw new FalhaPersistenciaException("Não foi possível criar a pasta de " + arquivo + ".", falha);
        }
        try (Connection conexao = abrirConexao(); Statement comando = conexao.createStatement()) {
            int versao;
            try (ResultSet resultado = comando.executeQuery("PRAGMA user_version")) {
                versao = resultado.getInt(1);
            }
            if (versao == 0) {
                criarEsquema(conexao);
            } else if (versao > VERSAO_ESQUEMA) {
                throw new FalhaPersistenciaException("O arquivo " + arquivo + " foi criado por uma versão"
                        + " mais nova da aplicação (esquema " + versao + "). Ele não foi alterado.");
            }
        } catch (SQLException falha) {
            throw new FalhaPersistenciaException("Não foi possível abrir o banco de dados " + arquivo
                    + ". Verifique se o caminho existe e se há permissão de leitura e escrita.", falha);
        }
    }

    private void criarEsquema(Connection conexao) throws SQLException {
        conexao.setAutoCommit(false);
        try (Statement comando = conexao.createStatement()) {
            for (String tabela : ESQUEMA) {
                comando.executeUpdate(tabela);
            }
            comando.executeUpdate("PRAGMA user_version = " + VERSAO_ESQUEMA);
            conexao.commit();
        } catch (SQLException falha) {
            conexao.rollback();
            throw falha;
        }
    }

    /** Liga as chaves estrangeiras, desligadas por padrão no SQLite. De pacote para ser testado. */
    Connection abrirConexao() throws SQLException {
        Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
        try (Statement comando = conexao.createStatement()) {
            comando.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException falha) {
            conexao.close();
            throw falha;
        }
        return conexao;
    }

    private static List<Transacao> carregarHistorico(Connection conexao, int numeroConta) throws SQLException {
        String sql = """
                SELECT tipo, valor_centavos, saldo_resultante_centavos, data_hora, conta_relacionada
                FROM transacoes WHERE conta_numero = ? ORDER BY sequencia""";
        try (PreparedStatement consulta = conexao.prepareStatement(sql)) {
            consulta.setInt(1, numeroConta);
            try (ResultSet linhas = consulta.executeQuery()) {
                List<Transacao> historico = new ArrayList<>();
                while (linhas.next()) {
                    int relacionada = linhas.getInt("conta_relacionada");
                    boolean semRelacionada = linhas.wasNull();
                    historico.add(new Transacao(
                            TipoTransacao.valueOf(linhas.getString("tipo")),
                            deCentavos(linhas.getLong("valor_centavos")),
                            deCentavos(linhas.getLong("saldo_resultante_centavos")),
                            LocalDateTime.parse(linhas.getString("data_hora")),
                            semRelacionada ? OptionalInt.empty() : OptionalInt.of(relacionada)));
                }
                return historico;
            }
        }
    }

    /** UPSERT: insere o cliente ou, se o CPF já existir, atualiza o nome. */
    private static void gravarCliente(Connection conexao, ContaPersistida conta) throws SQLException {
        String sql = """
                INSERT INTO clientes (cpf, nome) VALUES (?, ?)
                ON CONFLICT (cpf) DO UPDATE SET nome = excluded.nome""";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, conta.titularCpf());
            comando.setString(2, conta.titularNome());
            comando.executeUpdate();
        }
    }

    private static void gravarConta(Connection conexao, ContaPersistida conta) throws SQLException {
        String sql = """
                INSERT INTO contas (numero, tipo, cliente_cpf, saldo_centavos, limite_centavos,
                                    ultimo_mes_rendimento)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (numero) DO UPDATE SET
                    saldo_centavos = excluded.saldo_centavos,
                    ultimo_mes_rendimento = excluded.ultimo_mes_rendimento""";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setInt(1, conta.numero());
            comando.setString(2, conta.tipo().name());
            comando.setString(3, conta.titularCpf());
            comando.setLong(4, emCentavos(conta.saldo()));
            if (conta.limiteChequeEspecial() == null) {
                comando.setNull(5, Types.INTEGER);
            } else {
                comando.setLong(5, emCentavos(conta.limiteChequeEspecial()));
            }
            comando.setString(6, conta.ultimoMesComRendimento() == null
                    ? null : conta.ultimoMesComRendimento().toString());
            comando.executeUpdate();
        }
    }

    /**
     * O histórico só cresce. Grava apenas os lançamentos com sequência maior que a última já
     * gravada; os anteriores nunca são reescritos.
     */
    private static void gravarTransacoesNovas(Connection conexao, ContaPersistida conta) throws SQLException {
        int jaGravadas;
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT COALESCE(MAX(sequencia), 0) FROM transacoes WHERE conta_numero = ?")) {
            consulta.setInt(1, conta.numero());
            try (ResultSet resultado = consulta.executeQuery()) {
                jaGravadas = resultado.getInt(1);
            }
        }
        String sql = """
                INSERT INTO transacoes (conta_numero, sequencia, tipo, valor_centavos,
                                        saldo_resultante_centavos, data_hora, conta_relacionada)
                VALUES (?, ?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            List<Transacao> historico = conta.historico();
            for (int indice = jaGravadas; indice < historico.size(); indice++) {
                Transacao transacao = historico.get(indice);
                comando.setInt(1, conta.numero());
                comando.setInt(2, indice + 1);
                comando.setString(3, transacao.tipo().name());
                comando.setLong(4, emCentavos(transacao.valor()));
                comando.setLong(5, emCentavos(transacao.saldoResultante()));
                comando.setString(6, transacao.dataHora().toString());
                if (transacao.contaRelacionada().isPresent()) {
                    comando.setInt(7, transacao.contaRelacionada().getAsInt());
                } else {
                    comando.setNull(7, Types.INTEGER);
                }
                comando.executeUpdate();
            }
        }
    }

    /** R$ 12,34 vira 1234. {@code longValueExact} falha se houver frações de centavo. */
    static long emCentavos(BigDecimal valor) {
        return valor.movePointRight(2).longValueExact();
    }

    /** 1234 vira R$ 12,34, com escala 2. */
    static BigDecimal deCentavos(long centavos) {
        return BigDecimal.valueOf(centavos, 2);
    }
}
