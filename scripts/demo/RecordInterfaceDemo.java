import com.github.amandadamata.bankingsystem.Bank;
import com.github.amandadamata.bankingsystem.persistence.SqliteRepository;
import com.github.amandadamata.bankingsystem.ui.BankWindow;
import com.github.amandadamata.bankingsystem.ui.Theme;
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
 *   java -cp target/linha-banking-system-1.4.0.jar scripts/demo/RecordInterfaceDemo.java out.mp4
 */
public class RecordInterfaceDemo {
    private static final int WIDTH = 1920, HEIGHT = 1080, FPS = 30, BAR = 28;
    private static final double SCALE = 1.17;
    private static final int X = 56, Y = 60;
    private static final Color BACKGROUND = Color.decode("#1F2D33");

    private final BufferedImage frame = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
    private final byte[] bytes = ((DataBufferByte) frame.getRaster().getDataBuffer()).getData();
    private final OutputStream video;
    private final Path file;
    private BankWindow window;
    private Bank bank;
    private double px = 900, py = 700, clickProgress = -1;
    private JTextField field;
    private String title = "", detail = "";
    private long counter;

    private RecordInterfaceDemo(OutputStream video, Path file) { this.video = video; this.file = file; }

    public static void main(String[] args) throws Exception {
        // Grayscale: LCD smoothing would add colored fringes to the video.
        System.setProperty("awt.useSystemAAFontSettings", "on");
        Path output = Path.of(args[0]).toAbsolutePath();
        Path folder = Files.createTempDirectory("linha-demo");
        Process ffmpeg = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo",
                "-pix_fmt", "bgr24", "-s", WIDTH + "x" + HEIGHT, "-r", String.valueOf(FPS), "-i", "-",
                "-c:v", "libx264", "-preset", "medium", "-crf", "18", "-pix_fmt", "yuv420p",
                "-movflags", "+faststart", output.toString()).inheritIO()
                .redirectInput(ProcessBuilder.Redirect.PIPE).start();
        try (OutputStream video = ffmpeg.getOutputStream()) {
            new RecordInterfaceDemo(video, folder.resolve("demo.db")).script();
        }
        if (ffmpeg.waitFor() != 0) throw new IllegalStateException("ffmpeg failed");
        System.out.println("Video: " + output);
        System.exit(0);
    }

    private void script() throws Exception {
        openWindow();
        caption("From the terminal to a window", "The same Java banking system, now with a desktop interface.");
        hold(2.6);

        caption("Opening an account", "The CPF, Brazil's tax ID, is verified by its check digits.");
        click("Open first account");
        typeText("name", "Bruno Lima");
        typeText("cpf", "246.813.579-00");
        typeText("limit", "200.00", true);
        click("Create account");
        waitForMessage("CPF");
        caption("Invalid CPF, nothing is created", "The error shows on screen and the form stays filled in.");
        hold(2.6);
        typeText("cpf", "246.813.579-28", true);
        click("Create account");
        waitForMessage("created");
        caption("Checking account created", "With an overdraft limit of R$\u00A0200,00.");
        hold(2.2);

        caption("Deposit", "Exact amounts with BigDecimal, stored as whole cents.");
        click("Deposit");
        typeText("amount", "1500.00");
        click("Confirm deposit");
        waitForMessage("completed");
        hold(2.0);

        caption("Withdrawing too much", "With the limit, R$\u00A01.700,00 is available. A request for R$\u00A02.000,00 is refused.");
        click("Withdraw");
        typeText("amount", "2000.00");
        click("Confirm withdrawal");
        waitForMessage("Insufficient");
        hold(3.0);

        caption("Overdraft", "R$\u00A01.600,00 goes through, and the balance goes negative within the limit.");
        typeText("amount", "1600.00", true);
        click("Confirm withdrawal");
        waitForMessage("completed");
        hold(3.0);

        caption("Savings account", "Another account type, with its own rules and no overdraft.");
        click("New account");
        typeText("name", "Ana Souza");
        typeText("cpf", "135.792.468-28");
        moveTo(findByName("type"));
        pulse();
        edt(() -> ((JComboBox<?>) findByName("type")).setSelectedIndex(1));
        render();
        hold(0.6);
        click("Create account");
        waitForMessage("created");
        click("Deposit");
        typeText("amount", "1000.00");
        click("Confirm deposit");
        waitForMessage("completed");
        hold(1.2);

        caption("Monthly interest", "0.5% of the balance, at most once a month.");
        click("Monthly interest");
        hold(1.2);
        click("Confirm interest");
        waitForMessage("applied");
        hold(2.4);
        caption("Second try in the same month", "The same rule as the terminal refuses it.");
        click("Monthly interest");
        click("Confirm interest");
        waitForMessage("already been applied");
        hold(2.8);
        click("Cancel");

        caption("Transfer", "Both accounts are saved together, in a single SQL transaction.");
        click("Transfer");
        typeText("amount", "250.00");
        hold(0.8);
        click("Confirm transfer");
        waitForMessage("completed");
        hold(2.4);

        caption("Statement", "Date, type, other account and the balance after each transaction.");
        click("Transactions");
        hold(3.4);

        caption("Close and reopen", "Accounts and statements are still there, stored in SQLite.");
        hold(1.0);
        edt(() -> window.dispose());
        openWindow();
        hold(3.0);
        edt(() -> ((JComboBox<?>) findByName("accounts")).setSelectedIndex(1));
        render();
        hold(3.0);

        caption("Linha · Banking System",
                "Java 17, Swing, SQLite and automated tests.\n\ngithub.com/Amandadamata/\nsistema-bancario-java");
        hold(4.5);
        edt(() -> window.dispose());
    }

    private void openWindow() throws Exception {
        bank = Bank.load(Clock.systemDefaultZone(), new SqliteRepository(file));
        edt(() -> {
            Theme.install();
            window = new BankWindow(bank, file, () -> { });
            window.setFocusableWindowState(false);
            window.setVisible(true);
        });
        Thread.sleep(250);
        render();
    }

    private void caption(String t, String d) throws Exception { title = t; detail = d; render(); }

    private void click(String text) throws Exception {
        Component button = find(c -> c instanceof JButton b && text.equals(b.getText()) && b.isShowing());
        field = null;
        moveTo(button);
        pulse();
        edt(() -> ((JButton) button).doClick());
        Thread.sleep(60);
        render();
    }

    private void typeText(String name, String text) throws Exception { typeText(name, text, false); }

    private void typeText(String name, String text, boolean replaceText) throws Exception {
        JTextField target = (JTextField) findByName(name);
        moveTo(target);
        pulse();
        field = target;
        if (replaceText) {
            edt(() -> target.setText(""));
            render();
            hold(0.15);
        }
        StringBuilder current = new StringBuilder(target.getText());
        for (char letter : text.toCharArray()) {
            current.append(letter);
            String amount = current.toString();
            edt(() -> target.setText(amount));
            render();
            hold(2.0 / FPS);
        }
        hold(0.25);
    }

    private void moveTo(Component target) throws Exception {
        Point[] center = new Point[1];
        edt(() -> center[0] = SwingUtilities.convertPoint(target, target.getWidth() / 2, target.getHeight() / 2,
                window.getContentPane()));
        double xi = px, yi = py;
        double xf = X + center[0].x * SCALE, yf = Y + (BAR + center[0].y) * SCALE;
        int steps = (int) (FPS * 0.45);
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps, s = t * t * (3 - 2 * t);
            px = xi + (xf - xi) * s;
            py = yi + (yf - yi) * s;
            render();
            writeFrames(1);
        }
    }

    private void pulse() throws Exception {
        for (int i = 0; i < 7; i++) {
            clickProgress = i / 7.0;
            render();
            writeFrames(1);
        }
        clickProgress = -1;
    }

    private void waitForMessage(String segment) throws Exception {
        long limit = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < limit) {
            String[] text = new String[1];
            edt(() -> text[0] = ((JTextArea) findByName("message")).getText());
            if (text[0].contains(segment)) { render(); return; }
            Thread.sleep(25);
        }
        throw new IllegalStateException("Expected message did not appear: " + segment);
    }

    private void hold(double seconds) throws Exception { writeFrames((int) Math.round(seconds * FPS)); }

    private void writeFrames(int times) throws Exception {
        for (int i = 0; i < times; i++) video.write(bytes);
        counter += times;
    }

    private void render() throws Exception { edt(this::paintFrame); }

    private void paintFrame() {
        Container content = window.getContentPane();
        window.validate();
        Graphics2D g = frame.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        int width = content.getWidth(), height = content.getHeight();
        Graphics2D j = (Graphics2D) g.create();
        j.translate(X, Y);
        j.scale(SCALE, SCALE);
        j.setColor(new Color(0, 0, 0, 70));
        j.fill(new RoundRectangle2D.Double(-3, 3, width + 6, height + BAR + 6, 22, 22));
        Shape shape = new RoundRectangle2D.Double(0, 0, width, height + BAR, 20, 20);
        j.setClip(shape);
        j.setColor(Color.decode("#ECECEC"));
        j.fillRect(0, 0, width, BAR);
        String[] colors = {"#FF5F57", "#FEBC2E", "#28C840"};
        for (int i = 0; i < 3; i++) {
            j.setColor(Color.decode(colors[i]));
            j.fillOval(9 + i * 20, 8, 12, 12);
        }
        j.setColor(Color.decode("#4A4A4A"));
        j.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        String name = window.getTitle();
        j.drawString(name, (width - j.getFontMetrics().stringWidth(name)) / 2, 19);
        j.translate(0, BAR);
        Graphics2D c = (Graphics2D) j.create();
        c.setClip(new Rectangle(0, 0, width, height));
        content.printAll(c);
        c.dispose();
        if (field != null && field.isShowing()) focus(j);
        j.dispose();
        texts(g);
        pointer(g);
        g.dispose();
    }

    private void focus(Graphics2D g) {
        Point p = SwingUtilities.convertPoint(field, 0, 0, window.getContentPane());
        g.setColor(Theme.HIGHLIGHT);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(p.x + 1, p.y + 1, field.getWidth() - 2, field.getHeight() - 2, 10, 10));
        {
            FontMetrics m = field.getFontMetrics(field.getFont());
            int x = p.x + field.getInsets().left + m.stringWidth(field.getText()) + 1;
            g.setColor(Theme.TEXT);
            g.drawLine(x, p.y + 10, x, p.y + field.getHeight() - 10);
        }
    }

    private void texts(Graphics2D g) {
        int x = 1420, width = 450, y = 120;
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 46));
        y = paragraph(g, title, x, y, width, 56) + 26;
        g.setColor(Color.decode("#C8D5DA"));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 31));
        paragraph(g, detail, x, y, width, 42);
        g.setColor(Color.decode("#8FA3AB"));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        g.drawString("Study project · made-up data", x, HEIGHT - 66);
    }

    private static int paragraph(Graphics2D g, String text, int x, int y, int width, int lineSpacing) {
        FontMetrics m = g.getFontMetrics();
        for (String block : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : block.split(" ")) {
                String attempt = line.isEmpty() ? word : line + " " + word;
                if (m.stringWidth(attempt) > width) {
                    g.drawString(line.toString(), x, y += lineSpacing);
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(attempt);
                }
            }
            g.drawString(line.toString(), x, y += lineSpacing);
        }
        return y;
    }

    private void pointer(Graphics2D g) {
        if (clickProgress >= 0) {
            double r = 12 + clickProgress * 26;
            g.setColor(new Color(98, 125, 138, (int) (150 * (1 - clickProgress))));
            g.fill(new Ellipse2D.Double(px - r, py - r, r * 2, r * 2));
        }
        Path2D arrow = new Path2D.Double();
        double[][] points = {{0, 0}, {0, 26}, {6.5, 20}, {11, 30.5}, {15.5, 28.5}, {11, 18.5}, {19.5, 18.5}};
        arrow.moveTo(px, py);
        for (double[] p : points) arrow.lineTo(px + p[0], py + p[1]);
        arrow.closePath();
        g.setColor(Color.BLACK);
        g.fill(arrow);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        g.draw(arrow);
    }

    private Component findByName(String name) { return find(c -> name.equals(c.getName())); }

    private Component find(Predicate<Component> condition) {
        Component c = find(window.getContentPane(), condition);
        if (c == null) throw new IllegalStateException("Component not found");
        return c;
    }

    private static Component find(Container root, Predicate<Component> condition) {
        for (Component c : root.getComponents()) {
            if (condition.test(c)) return c;
            if (c instanceof Container child) {
                Component match = find(child, condition);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static void edt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
}
