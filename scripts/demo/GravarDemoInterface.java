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
 * Records the desktop demo video. The window, clicks and data are real (JanelaBanco, Banco and a
 * temporary SQLite file); each frame is painted by the Swing components themselves. The title bar,
 * pointer, text cursor and captions are drawn by this script.
 *
 * Usage, from the project root, after ./mvnw package (needs ffmpeg):
 *   java -cp target/sistema-bancario-java-1.3.0.jar scripts/demo/GravarDemoInterface.java out.mp4
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
        // Grayscale: LCD smoothing would add colored fringes to the video.
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
        if (ffmpeg.waitFor() != 0) throw new IllegalStateException("ffmpeg failed");
        System.out.println("Video: " + saida);
        System.exit(0);
    }

    private void roteiro() throws Exception {
        abrirJanela();
        legenda("From the terminal to a window", "The same Java banking system, now with a desktop interface.");
        segurar(2.6);

        legenda("Opening an account", "The CPF, Brazil's tax ID, is verified by its check digits.");
        clicar("Open first account");
        digitar("nome", "Bruno Lima");
        digitar("cpf", "246.813.579-00");
        digitar("limite", "200.00", true);
        clicar("Create account");
        aguardarMensagem("CPF");
        legenda("Invalid CPF, nothing is created", "The error shows on screen and the form stays filled in.");
        segurar(2.6);
        digitar("cpf", "246.813.579-28", true);
        clicar("Create account");
        aguardarMensagem("created");
        legenda("Checking account created", "With an overdraft limit of R$\u00A0200,00.");
        segurar(2.2);

        legenda("Deposit", "Exact amounts with BigDecimal, stored as whole cents.");
        clicar("Deposit");
        digitar("valor", "1500.00");
        clicar("Confirm deposit");
        aguardarMensagem("completed");
        segurar(2.0);

        legenda("Withdrawing too much", "With the limit, R$\u00A01.700,00 is available. A request for R$\u00A02.000,00 is refused.");
        clicar("Withdraw");
        digitar("valor", "2000.00");
        clicar("Confirm withdrawal");
        aguardarMensagem("Insufficient");
        segurar(3.0);

        legenda("Overdraft", "R$\u00A01.600,00 goes through, and the balance goes negative within the limit.");
        digitar("valor", "1600.00", true);
        clicar("Confirm withdrawal");
        aguardarMensagem("completed");
        segurar(3.0);

        legenda("Savings account", "Another account type, with its own rules and no overdraft.");
        clicar("New account");
        digitar("nome", "Ana Souza");
        digitar("cpf", "135.792.468-28");
        mover(buscarNome("tipo"));
        pulsar();
        edt(() -> ((JComboBox<?>) buscarNome("tipo")).setSelectedIndex(1));
        renderizar();
        segurar(0.6);
        clicar("Create account");
        aguardarMensagem("created");
        clicar("Deposit");
        digitar("valor", "1000.00");
        clicar("Confirm deposit");
        aguardarMensagem("completed");
        segurar(1.2);

        legenda("Monthly interest", "0.5% of the balance, at most once a month.");
        clicar("Monthly interest");
        segurar(1.2);
        clicar("Confirm interest");
        aguardarMensagem("applied");
        segurar(2.4);
        legenda("Second try in the same month", "The same rule as the terminal refuses it.");
        clicar("Monthly interest");
        clicar("Confirm interest");
        aguardarMensagem("already been applied");
        segurar(2.8);
        clicar("Cancel");

        legenda("Transfer", "Both accounts are saved together, in a single SQL transaction.");
        clicar("Transfer");
        digitar("valor", "250.00");
        segurar(0.8);
        clicar("Confirm transfer");
        aguardarMensagem("completed");
        segurar(2.4);

        legenda("Statement", "Date, type, other account and the balance after each transaction.");
        clicar("Transactions");
        segurar(3.4);

        legenda("Close and reopen", "Accounts and statements are still there, stored in SQLite.");
        segurar(1.0);
        edt(() -> janela.dispose());
        abrirJanela();
        segurar(3.0);
        edt(() -> ((JComboBox<?>) buscarNome("contas")).setSelectedIndex(1));
        renderizar();
        segurar(3.0);

        legenda("Linha · Banking System",
                "Java 17, Swing, SQLite and automated tests.\n\ngithub.com/Amandadamata/\nsistema-bancario-java");
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
        throw new IllegalStateException("Expected message did not appear: " + trecho);
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
        g.drawString("Study project · made-up data", x, ALTURA - 66);
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
        if (c == null) throw new IllegalStateException("Component not found");
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
