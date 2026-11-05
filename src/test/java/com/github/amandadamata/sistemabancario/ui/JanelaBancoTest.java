package com.github.amandadamata.sistemabancario.ui;

import com.github.amandadamata.sistemabancario.*;
import com.github.amandadamata.sistemabancario.persistencia.RepositorioSqlite;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

/** Exercita eventos Swing reais e SQLite temporário. Requer uma sessão gráfica (-Pui-tests). */
@EnabledIfSystemProperty(named = "linha.ui.tests", matches = "true")
class JanelaBancoTest {
    @TempDir Path pasta;
    private Banco banco;
    private JanelaBanco janela;
    private Path arquivo;

    @BeforeEach void preparar() {
        arquivo = pasta.resolve("teste.db");
        banco = Banco.carregar(Clock.systemDefaultZone(), new RepositorioSqlite(arquivo));
    }

    private void abrir() throws Exception {
        edt(() -> {
            Tema.instalar();
            janela = new JanelaBanco(banco, arquivo, () -> {});
            janela.setVisible(true);
        });
    }

    @AfterEach void fechar() throws Exception {
        if (janela != null) edt(() -> janela.dispose());
    }

    @Test void cadastroDepositoSaqueExtratoECancelamento() throws Exception {
        abrir();
        clicar("Open first account");
        preencher("nome", "Ana Teste");
        preencher("cpf", "123");
        clicar("Create account");
        aguardarMensagem("11 digits");
        assertTrue(banco.listarContas().isEmpty());
        preencher("cpf", "24681357928");
        clicar("Create account");
        aguardarMensagem("created");
        assertEquals(1, banco.listarContas().size());
        clicar("Deposit");
        for (String invalido : new String[]{"abc", "-10", "1.234,56", "10,005", "0"}) {
            preencher("valor", invalido);
            clicar("Confirm deposit");
            aguardar(() -> mensagem().contains("Invalid") || mensagem().contains("greater than zero"));
            assertEquals(new BigDecimal("0.00"), conta(1000).getSaldo());
        }
        preencher("valor", "150,50");
        clicar("Cancel");
        assertEquals(new BigDecimal("0.00"), conta(1000).getSaldo());
        operar("Deposit", "Confirm deposit", "150,50");
        aguardarMensagem("completed");
        edt(() -> assertEquals("R$ 150,50", ((JLabel) nome("saldo")).getText()));
        operar("Withdraw", "Confirm withdrawal", "150,51");
        aguardarMensagem("Insufficient balance");
        preencher("valor", "50,25");
        clicar("Confirm withdrawal");
        aguardarMensagem("completed");
        clicar("View all transactions");
        edt(() -> {
            JTable tabela = (JTable) nome("historico");
            assertEquals(2, tabela.getRowCount());
            assertEquals("− R$ 50,25", tabela.getValueAt(0, 2));
        });
        assertEquals(new BigDecimal("100.25"), recarregar().buscarContaPorNumero(1000).orElseThrow().getSaldo());
    }

    @Test void transferenciaAtualizaAsDuasContasESeletorSemPermitirAutoTransferencia() throws Exception {
        duasContas();
        banco.depositar(1000, new BigDecimal("100.00"));
        abrir();
        clicar("Transfer");
        edt(() -> {
            JComboBox<?> destino = (JComboBox<?>) nome("destino");
            assertEquals(1, destino.getItemCount());
            assertEquals(1001, ((Conta) destino.getItemAt(0)).getNumero());
        });
        preencher("valor", "100,01");
        clicar("Confirm transfer");
        aguardarMensagem("Insufficient balance");
        assertEquals(new BigDecimal("0.00"), conta(1001).getSaldo());
        preencher("valor", "25,10");
        clicar("Confirm transfer");
        aguardarMensagem("Transfer of R$ 25,10 completed.");
        assertEquals(new BigDecimal("74.90"), conta(1000).getSaldo());
        assertEquals(new BigDecimal("25.10"), recarregar().buscarContaPorNumero(1001).orElseThrow().getSaldo());
        edt(() -> ((JComboBox<?>) nome("contas")).setSelectedIndex(1));
        edt(() -> assertEquals("R$ 25,10", ((JLabel) nome("saldo")).getText()));
        clicar("Transactions");
        edt(() -> assertEquals("+ R$ 25,10", ((JTable) nome("historico")).getValueAt(0, 2)));
    }

    @Test void falhaDeGravacaoExibeErroERestauraSaldoSemDuplicarAoTentarNovamente() throws Exception {
        duasContas();
        try (var conexao = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
             var sql = conexao.createStatement()) {
            sql.execute("CREATE TRIGGER falha BEFORE INSERT ON transacoes BEGIN SELECT RAISE(ABORT, 'teste'); END");
        }
        abrir();
        operar("Deposit", "Confirm deposit", "15,00");
        aguardarMensagem("undone");
        assertEquals(new BigDecimal("0.00"), conta(1000).getSaldo());
        assertTrue(conta(1000).getHistorico().isEmpty());
        try (var conexao = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
             var sql = conexao.createStatement()) { sql.execute("DROP TRIGGER falha"); }
        clicar("Confirm deposit");
        aguardarMensagem("completed");
        assertEquals(1, conta(1000).getHistorico().size());
        assertEquals(new BigDecimal("15.00"), recarregar().buscarContaPorNumero(1000).orElseThrow().getSaldo());
    }

    @Test void rendimentoETrocaDeTelasFuncionamNoTamanhoMinimo() throws Exception {
        duasContas();
        banco.depositar(1000, new BigDecimal("1000.00"));
        abrir();
        edt(() -> janela.setSize(janela.getMinimumSize()));
        clicar("Monthly interest");
        clicar("Confirm interest");
        aguardarMensagem("Interest of R$ 5,00 applied");
        clicar("Monthly interest");
        clicar("Confirm interest");
        aguardarMensagem("already been applied");
        clicar("Cancel");
        clicar("Transactions");
        clicar("Overview");
        edt(() -> assertEquals("R$ 1.005,00", ((JLabel) nome("saldo")).getText()));
    }

    @Test void transferenciaSemDestinoFicaDesabilitada() throws Exception {
        banco.abrirContaPoupanca(new Cliente("Ana", "24681357928"));
        abrir();
        clicar("Transfer");
        edt(() -> assertFalse(botao("Confirm transfer").isEnabled()));
        clicar("Cancel");
        edt(() -> assertNotNull(nome("saldo")));
    }

    @Test void operacaoLentaNaoBloqueiaEdtNemPermiteDuploEnvioOuFechamento() throws Exception {
        CountDownLatch iniciou = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        RepositorioBanco repositorio = new RepositorioBanco() {
            public java.util.List<ContaPersistida> carregar() { return java.util.List.of(); }
            public void salvar(java.util.List<ContaPersistida> contas) {
                if (contas.get(0).historico().isEmpty()) return;
                iniciou.countDown();
                try {
                    if (!liberar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Tempo limite do teste");
                } catch (InterruptedException e) { throw new IllegalStateException(e); }
            }
        };
        banco = Banco.carregar(Clock.systemUTC(), repositorio);
        banco.abrirContaPoupanca(new Cliente("Ana", "24681357928"));
        abrir();
        try {
            operar("Deposit", "Confirm deposit", "10");
            assertTrue(iniciou.await(3, TimeUnit.SECONDS));
            edt(() -> {
                assertFalse(botao("Confirm deposit").isEnabled());
                botao("Confirm deposit").doClick();
                janela.dispatchEvent(new WindowEvent(janela, WindowEvent.WINDOW_CLOSING));
                assertTrue(janela.isDisplayable());
            });
        } finally { liberar.countDown(); }
        aguardarMensagem("completed");
        assertEquals(1, conta(1000).getHistorico().size());
        assertEquals(new BigDecimal("10.00"), conta(1000).getSaldo());
    }

    @Test void areaLivreDaJanelaUsaAMesmaCorDeFundo() throws Exception {
        abrir();
        edt(() -> {
            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(
                    JViewport.class, botao("Open first account"));
            assertEquals(Tema.FUNDO, viewport.getBackground());
            assertFalse(nome("contas").isVisible());
        });
    }

    @Test void renderizaTelasReaisParaInspecaoVisual() throws Exception {
        duasContas();
        banco.depositar(1000, new BigDecimal("2500.00"));
        banco.sacar(1000, new BigDecimal("120.50"));
        banco.transferir(1000, 1001, new BigDecimal("350.00"));
        abrir();
        capturar("resumo");
        clicar("Transfer");
        capturar("transferencia");
        clicar("Cancel");
        edt(() -> janela.setSize(janela.getMinimumSize()));
        capturar("resumo-compacto");
        edt(() -> {
            JButton transferir = botao("Transfer");
            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, transferir);
            Rectangle limites = SwingUtilities.convertRectangle(transferir.getParent(), transferir.getBounds(), viewport);
            assertTrue(limites.x >= 0 && limites.x + limites.width <= viewport.getWidth(),
                    "The Transfer action must not be cut off when the window shrinks");
        });
        clicar("New account");
        capturar("nova-conta-compacta");
    }

    private void capturar(String nome) throws Exception {
        // Renderiza os próprios componentes Swing, sem recriar a interface em outro formato.
        edt(() -> {
            janela.validate();
            java.awt.image.BufferedImage imagem = new java.awt.image.BufferedImage(
                    janela.getContentPane().getWidth(), janela.getContentPane().getHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = imagem.createGraphics();
            janela.getContentPane().printAll(g);
            g.dispose();
            try {
                Path destino = Path.of("target", "visual", nome + ".png");
                java.nio.file.Files.createDirectories(destino.getParent());
                javax.imageio.ImageIO.write(imagem, "png", destino.toFile());
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        });
    }

    private void duasContas() {
        banco.abrirContaPoupanca(new Cliente("Ana Teste", "24681357928"));
        banco.abrirContaCorrente(new Cliente("Bruno Teste", "13579246828"), BigDecimal.ZERO);
    }
    private Banco recarregar() { return Banco.carregar(Clock.systemDefaultZone(), new RepositorioSqlite(arquivo)); }
    private Conta conta(int numero) { return banco.buscarContaPorNumero(numero).orElseThrow(); }
    private void operar(String operacao, String confirmar, String valor) throws Exception {
        clicar(operacao); preencher("valor", valor); clicar(confirmar);
    }
    private void clicar(String texto) throws Exception { edt(() -> botao(texto).doClick()); }
    private void preencher(String nome, String valor) throws Exception { edt(() -> ((JTextField) nome(nome)).setText(valor)); }
    private JButton botao(String texto) { return (JButton) buscar(janela, c -> c instanceof JButton b && texto.equals(b.getText())); }
    private Component nome(String nome) { return buscar(janela, c -> nome.equals(c.getName())); }
    private String mensagem() { return ((JTextArea) nome("mensagem")).getText(); }
    private static Component buscar(Container raiz, Predicate<Component> condicao) {
        for (Component c : raiz.getComponents()) {
            if (condicao.test(c)) return c;
            if (c instanceof Container filho) {
                Component encontrado = buscar(filho, condicao);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }
    private void aguardarMensagem(String texto) throws Exception { aguardar(() -> mensagem().contains(texto)); }
    private void aguardar(java.util.function.BooleanSupplier condicao) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        do {
            AtomicReference<Boolean> resultado = new AtomicReference<>(false);
            edt(() -> resultado.set(condicao.getAsBoolean()));
            if (resultado.get()) return;
            Thread.sleep(25);
        } while (System.nanoTime() < limite);
        edt(() -> fail("Expected result did not appear. Message: " + mensagem()));
    }
    private static void edt(Runnable acao) throws Exception { SwingUtilities.invokeAndWait(acao); }
}
