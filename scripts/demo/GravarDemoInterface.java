import com.github.amandadamata.sistemabancario.Banco;
import com.github.amandadamata.sistemabancario.persistencia.RepositorioSqlite;
import com.github.amandadamata.sistemabancario.ui.JanelaBanco;
import com.github.amandadamata.sistemabancario.ui.Tema;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.function.Predicate;

/**
 * Grava o vídeo de demonstração da interface. A janela, os cliques e os dados são os reais
 * (JanelaBanco, Banco e um SQLite temporário); cada quadro é pintado pelos próprios componentes
 * Swing. A moldura, o ponteiro, o cursor de texto e as legendas são desenhados por este script.
 *
 * Uso, na raiz do projeto, depois de ./mvnw package (requer ffmpeg):
 *   java -cp target/sistema-bancario-java-1.2.0.jar scripts/demo/GravarDemoInterface.java saida.mp4
 */
public class GravarDemoInterface {
    private static final int LARGURA = 1920, ALTURA = 1080, FPS = 30, BARRA = 28;
    private static final double ESCALA = 1.17;
    private static final int X = 56, Y = 60;
    private static final Color FUNDO = Color.decode("#1F2D33");

    private final BufferedImage quadro = new BufferedImage(LARGURA, ALTURA, BufferedImage.TYPE_3BYTE_BGR);
    private final byte[] bytes = ((DataBufferByte) quadro.getRaster().getDataBuffer()).getData();
    private final OutputStream video;
    private final Path arquivo;
    private JanelaBanco janela;
    private Banco banco;
    private double px = 900, py = 700, clique = -1;
    private JTextField campo;
    private String titulo = "", detalhe = "";
    private long contador;

    private GravarDemoInterface(OutputStream video, Path arquivo) { this.video = video; this.arquivo = arquivo; }

    public static void main(String[] args) throws Exception {
        // Tons de cinza: a suavização LCD criaria franjas coloridas no vídeo.
        System.setProperty("awt.useSystemAAFontSettings", "on");
        Path saida = Path.of(args[0]).toAbsolutePath();
        Path pasta = Files.createTempDirectory("linha-demo");
        Process ffmpeg = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo",
                "-pix_fmt", "bgr24", "-s", LARGURA + "x" + ALTURA, "-r", String.valueOf(FPS), "-i", "-",
                "-c:v", "libx264", "-preset", "medium", "-crf", "18", "-pix_fmt", "yuv420p",
                "-movflags", "+faststart", saida.toString()).inheritIO()
                .redirectInput(ProcessBuilder.Redirect.PIPE).start();
        try (OutputStream video = ffmpeg.getOutputStream()) {
            new GravarDemoInterface(video, pasta.resolve("demo.db")).roteiro();
        }
        if (ffmpeg.waitFor() != 0) throw new IllegalStateException("ffmpeg falhou");
        System.out.println("Vídeo: " + saida);
        System.exit(0);
    }

    private void roteiro() throws Exception {
        abrirJanela();
        legenda("Do terminal para a janela", "O mesmo sistema bancário em Java, agora com interface gráfica.");
        segurar(2.6);

        legenda("Abrir uma conta", "O CPF é conferido pelos dígitos verificadores.");
        clicar("Abrir primeira conta");
        digitar("nome", "Bruno Lima");
        digitar("cpf", "246.813.579-00");
        digitar("limite", "200,00", true);
        clicar("Criar conta");
        aguardarMensagem("CPF");
        legenda("CPF inválido, nada é criado", "O erro aparece na tela e o formulário continua preenchido.");
        segurar(2.6);
        digitar("cpf", "246.813.579-28", true);
        clicar("Criar conta");
        aguardarMensagem("criada");
        legenda("Conta corrente criada", "Limite de R$\u00A0200,00 no cheque especial.");
        segurar(2.2);

        legenda("Depósito", "Valores exatos com BigDecimal, gravados em centavos.");
        clicar("Depositar");
        digitar("valor", "1500,00");
        clicar("Confirmar depósito");
        aguardarMensagem("concluído");
        segurar(2.0);

        legenda("Saque acima do disponível", "Com o limite, o disponível é R$\u00A01.700,00. O pedido de R$\u00A02.000,00 é recusado.");
        clicar("Sacar");
        digitar("valor", "2000,00");
        clicar("Confirmar saque");
        aguardarMensagem("insuficiente");
        segurar(3.0);

        legenda("Cheque especial", "Com R$\u00A01.600,00 o saque passa, e a conta fica negativa dentro do limite.");
        digitar("valor", "1600,00", true);
        clicar("Confirmar saque");
        aguardarMensagem("concluído");
        segurar(3.0);

        legenda("Conta poupança", "Outro tipo de conta, com regras próprias, sem cheque especial.");
        clicar("Nova conta");
        digitar("nome", "Ana Souza");
        digitar("cpf", "135.792.468-28");
        mover(buscarNome("tipo"));
        pulsar();
        edt(() -> ((JComboBox<?>) buscarNome("tipo")).setSelectedIndex(1));
        renderizar();
        segurar(0.6);
        clicar("Criar conta");
        aguardarMensagem("criada");
        clicar("Depositar");
        digitar("valor", "1000,00");
        clicar("Confirmar depósito");
        aguardarMensagem("concluído");
        segurar(1.2);

        legenda("Rendimento mensal", "0,5% sobre o saldo, no máximo uma vez por mês.");
        clicar("Rendimento mensal");
        segurar(1.2);
        clicar("Confirmar rendimento");
        aguardarMensagem("aplicado");
        segurar(2.4);
        legenda("Segunda tentativa no mês", "A mesma regra do terminal recusa o rendimento repetido.");
        clicar("Rendimento mensal");
        clicar("Confirmar rendimento");
        aguardarMensagem("já foi aplicado");
        segurar(2.8);
        clicar("Cancelar");

        legenda("Transferência", "As duas contas são gravadas juntas, em uma única transação SQL.");
        clicar("Transferir");
        digitar("valor", "250,00");
        segurar(0.8);
        clicar("Confirmar transferência");
        aguardarMensagem("concluída");
        segurar(2.4);

        legenda("Extrato", "Data, tipo, conta relacionada e saldo após cada operação.");
        clicar("Movimentações");
        segurar(3.4);

        legenda("Fechar e abrir de novo", "Contas e extratos continuam lá: os dados ficam em SQLite.");
        segurar(1.0);
        edt(() -> janela.dispose());
        abrirJanela();
        segurar(3.0);
        edt(() -> ((JComboBox<?>) buscarNome("contas")).setSelectedIndex(1));
        renderizar();
        segurar(3.0);

        legenda("Linha · Sistema Bancário",
                "Java 17, Swing, SQLite e testes automatizados.\n\ngithub.com/Amandadamata/\nsistema-bancario-java");
        segurar(4.5);
        edt(() -> janela.dispose());
    }

    private void abrirJanela() throws Exception {
        banco = Banco.carregar(Clock.systemDefaultZone(), new RepositorioSqlite(arquivo));
        edt(() -> {
            Tema.instalar();
            janela = new JanelaBanco(banco, arquivo, () -> { });
            janela.setFocusableWindowState(false);
            janela.setVisible(true);
        });
        Thread.sleep(250);
        renderizar();
    }

    private void legenda(String t, String d) throws Exception { titulo = t; detalhe = d; renderizar(); }

    private void clicar(String texto) throws Exception {
        Component botao = buscar(c -> c instanceof JButton b && texto.equals(b.getText()) && b.isShowing());
        campo = null;
        mover(botao);
        pulsar();
        edt(() -> ((JButton) botao).doClick());
        Thread.sleep(60);
        renderizar();
    }

    private void digitar(String nome, String texto) throws Exception { digitar(nome, texto, false); }

    private void digitar(String nome, String texto, boolean substituir) throws Exception {
        JTextField alvo = (JTextField) buscarNome(nome);
        mover(alvo);
        pulsar();
        campo = alvo;
        if (substituir) {
            edt(() -> alvo.setText(""));
            renderizar();
            segurar(0.15);
        }
        StringBuilder atual = new StringBuilder(alvo.getText());
        for (char letra : texto.toCharArray()) {
            atual.append(letra);
            String valor = atual.toString();
            edt(() -> alvo.setText(valor));
            renderizar();
            segurar(2.0 / FPS);
        }
        segurar(0.25);
    }

    private void mover(Component alvo) throws Exception {
        Point[] centro = new Point[1];
        edt(() -> centro[0] = SwingUtilities.convertPoint(alvo, alvo.getWidth() / 2, alvo.getHeight() / 2,
                janela.getContentPane()));
        double xi = px, yi = py;
        double xf = X + centro[0].x * ESCALA, yf = Y + (BARRA + centro[0].y) * ESCALA;
        int passos = (int) (FPS * 0.45);
        for (int i = 1; i <= passos; i++) {
            double t = (double) i / passos, s = t * t * (3 - 2 * t);
            px = xi + (xf - xi) * s;
            py = yi + (yf - yi) * s;
            renderizar();
            escrever(1);
        }
    }

    private void pulsar() throws Exception {
        for (int i = 0; i < 7; i++) {
            clique = i / 7.0;
            renderizar();
            escrever(1);
        }
        clique = -1;
    }

    private void aguardarMensagem(String trecho) throws Exception {
        long limite = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < limite) {
            String[] texto = new String[1];
            edt(() -> texto[0] = ((JTextArea) buscarNome("mensagem")).getText());
            if (texto[0].contains(trecho)) { renderizar(); return; }
            Thread.sleep(25);
        }
        throw new IllegalStateException("Mensagem esperada não apareceu: " + trecho);
    }

    private void segurar(double segundos) throws Exception { escrever((int) Math.round(segundos * FPS)); }

    private void escrever(int vezes) throws Exception {
        for (int i = 0; i < vezes; i++) video.write(bytes);
        contador += vezes;
    }

    private void renderizar() throws Exception { edt(this::pintar); }

    private void pintar() {
        Container conteudo = janela.getContentPane();
        janela.validate();
        Graphics2D g = quadro.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(FUNDO);
        g.fillRect(0, 0, LARGURA, ALTURA);
        int largura = conteudo.getWidth(), altura = conteudo.getHeight();
        Graphics2D j = (Graphics2D) g.create();
        j.translate(X, Y);
        j.scale(ESCALA, ESCALA);
        j.setColor(new Color(0, 0, 0, 70));
        j.fill(new RoundRectangle2D.Double(-3, 3, largura + 6, altura + BARRA + 6, 22, 22));
        Shape forma = new RoundRectangle2D.Double(0, 0, largura, altura + BARRA, 20, 20);
        j.setClip(forma);
        j.setColor(Color.decode("#ECECEC"));
        j.fillRect(0, 0, largura, BARRA);
        String[] cores = {"#FF5F57", "#FEBC2E", "#28C840"};
        for (int i = 0; i < 3; i++) {
            j.setColor(Color.decode(cores[i]));
            j.fillOval(9 + i * 20, 8, 12, 12);
        }
        j.setColor(Color.decode("#4A4A4A"));
        j.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        String nome = janela.getTitle();
        j.drawString(nome, (largura - j.getFontMetrics().stringWidth(nome)) / 2, 19);
        j.translate(0, BARRA);
        Graphics2D c = (Graphics2D) j.create();
        c.setClip(new Rectangle(0, 0, largura, altura));
        conteudo.printAll(c);
        c.dispose();
        if (campo != null && campo.isShowing()) foco(j);
        j.dispose();
        textos(g);
        ponteiro(g);
        g.dispose();
    }

    private void foco(Graphics2D g) {
        Point p = SwingUtilities.convertPoint(campo, 0, 0, janela.getContentPane());
        g.setColor(Tema.DESTAQUE);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(p.x + 1, p.y + 1, campo.getWidth() - 2, campo.getHeight() - 2, 10, 10));
        {
            FontMetrics m = campo.getFontMetrics(campo.getFont());
            int x = p.x + campo.getInsets().left + m.stringWidth(campo.getText()) + 1;
            g.setColor(Tema.TEXTO);
            g.drawLine(x, p.y + 10, x, p.y + campo.getHeight() - 10);
        }
    }

    private void textos(Graphics2D g) {
        int x = 1420, largura = 450, y = 120;
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 46));
        y = paragrafo(g, titulo, x, y, largura, 56) + 26;
        g.setColor(Color.decode("#C8D5DA"));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 31));
        paragrafo(g, detalhe, x, y, largura, 42);
        g.setColor(Color.decode("#8FA3AB"));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        g.drawString("Projeto educacional · dados fictícios", x, ALTURA - 66);
    }

    private static int paragrafo(Graphics2D g, String texto, int x, int y, int largura, int entrelinha) {
        FontMetrics m = g.getFontMetrics();
        for (String bloco : texto.split("\n", -1)) {
            StringBuilder linha = new StringBuilder();
            for (String palavra : bloco.split(" ")) {
                String tentativa = linha.isEmpty() ? palavra : linha + " " + palavra;
                if (m.stringWidth(tentativa) > largura) {
                    g.drawString(linha.toString(), x, y += entrelinha);
                    linha = new StringBuilder(palavra);
                } else {
                    linha = new StringBuilder(tentativa);
                }
            }
            g.drawString(linha.toString(), x, y += entrelinha);
        }
        return y;
    }

    private void ponteiro(Graphics2D g) {
        if (clique >= 0) {
            double r = 12 + clique * 26;
            g.setColor(new Color(98, 125, 138, (int) (150 * (1 - clique))));
            g.fill(new Ellipse2D.Double(px - r, py - r, r * 2, r * 2));
        }
        Path2D seta = new Path2D.Double();
        double[][] pontos = {{0, 0}, {0, 26}, {6.5, 20}, {11, 30.5}, {15.5, 28.5}, {11, 18.5}, {19.5, 18.5}};
        seta.moveTo(px, py);
        for (double[] p : pontos) seta.lineTo(px + p[0], py + p[1]);
        seta.closePath();
        g.setColor(Color.BLACK);
        g.fill(seta);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        g.draw(seta);
    }

    private Component buscarNome(String nome) { return buscar(c -> nome.equals(c.getName())); }

    private Component buscar(Predicate<Component> condicao) {
        Component c = buscar(janela.getContentPane(), condicao);
        if (c == null) throw new IllegalStateException("Componente não encontrado");
        return c;
    }

    private static Component buscar(Container raiz, Predicate<Component> condicao) {
        for (Component c : raiz.getComponents()) {
            if (condicao.test(c)) return c;
            if (c instanceof Container filho) {
                Component achado = buscar(filho, condicao);
                if (achado != null) return achado;
            }
        }
        return null;
    }

    private static void edt(Runnable acao) throws Exception { SwingUtilities.invokeAndWait(acao); }
}
