package com.github.amandadamata.bankingsystem.ui;

import com.github.amandadamata.bankingsystem.*;
import com.github.amandadamata.bankingsystem.persistence.SqliteRepository;
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

/** Exercises real Swing events and a temporary SQLite file. Needs a graphical session (-Pui-tests). */
@EnabledIfSystemProperty(named = "linha.ui.tests", matches = "true")
class BankWindowTest {
    @TempDir Path folder;
    private Bank bank;
    private BankWindow window;
    private Path file;

    @BeforeEach void prepare() {
        file = folder.resolve("test.db");
        bank = Bank.load(Clock.systemDefaultZone(), new SqliteRepository(file));
    }

    private void openApp() throws Exception {
        edt(() -> {
            Theme.install();
            window = new BankWindow(bank, file, () -> {});
            window.setVisible(true);
        });
    }

    @AfterEach void closeWindow() throws Exception {
        if (window != null) edt(() -> window.dispose());
    }

    @Test void signUpDepositWithdrawalStatementAndCancel() throws Exception {
        openApp();
        click("Open first account");
        fillField("name", "Ana Pereira");
        fillField("cpf", "123");
        click("Create account");
        waitForMessage("11 digits");
        assertTrue(bank.listAccounts().isEmpty());
        fillField("cpf", "24681357928");
        click("Create account");
        waitForMessage("created");
        assertEquals(1, bank.listAccounts().size());
        click("Deposit");
        for (String invalid : new String[]{"abc", "-10", "1.234,56", "10,005", "0"}) {
            fillField("amount", invalid);
            click("Confirm deposit");
            waitUntil(() -> message().contains("Invalid") || message().contains("greater than zero"));
            assertEquals(new BigDecimal("0.00"), account(1000).getBalance());
        }
        fillField("amount", "150,50");
        click("Cancel");
        assertEquals(new BigDecimal("0.00"), account(1000).getBalance());
        operate("Deposit", "Confirm deposit", "150,50");
        waitForMessage("completed");
        edt(() -> assertEquals("R$ 150,50", ((JLabel) name("balance")).getText()));
        operate("Withdraw", "Confirm withdrawal", "150,51");
        waitForMessage("Insufficient balance");
        fillField("amount", "50,25");
        click("Confirm withdrawal");
        waitForMessage("completed");
        click("View all transactions");
        edt(() -> {
            JTable historyTable = (JTable) name("history");
            assertEquals(2, historyTable.getRowCount());
            assertEquals("− R$ 50,25", historyTable.getValueAt(0, 2));
        });
        assertEquals(new BigDecimal("100.25"), reload().findAccountByNumber(1000).orElseThrow().getBalance());
    }

    @Test void transferUpdatesBothAccountsAndTheSelectorWithoutSelfTransfer() throws Exception {
        twoAccounts();
        bank.deposit(1000, new BigDecimal("100.00"));
        openApp();
        click("Transfer");
        edt(() -> {
            JComboBox<?> destination = (JComboBox<?>) name("destination");
            assertEquals(1, destination.getItemCount());
            assertEquals(1001, ((Account) destination.getItemAt(0)).getNumber());
        });
        fillField("amount", "100,01");
        click("Confirm transfer");
        waitForMessage("Insufficient balance");
        assertEquals(new BigDecimal("0.00"), account(1001).getBalance());
        fillField("amount", "25,10");
        click("Confirm transfer");
        waitForMessage("Transfer of R$ 25,10 completed.");
        assertEquals(new BigDecimal("74.90"), account(1000).getBalance());
        assertEquals(new BigDecimal("25.10"), reload().findAccountByNumber(1001).orElseThrow().getBalance());
        edt(() -> ((JComboBox<?>) name("accounts")).setSelectedIndex(1));
        edt(() -> assertEquals("R$ 25,10", ((JLabel) name("balance")).getText()));
        click("Transactions");
        edt(() -> assertEquals("+ R$ 25,10", ((JTable) name("history")).getValueAt(0, 2)));
    }

    @Test void saveFailureShowsAnErrorAndRestoresTheBalanceWithoutDuplicatingOnRetry() throws Exception {
        twoAccounts();
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             var sql = connection.createStatement()) {
            sql.execute("CREATE TRIGGER falha BEFORE INSERT ON transactions BEGIN SELECT RAISE(ABORT, 'test'); END");
        }
        openApp();
        operate("Deposit", "Confirm deposit", "15,00");
        waitForMessage("undone");
        assertEquals(new BigDecimal("0.00"), account(1000).getBalance());
        assertTrue(account(1000).getHistory().isEmpty());
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             var sql = connection.createStatement()) { sql.execute("DROP TRIGGER falha"); }
        click("Confirm deposit");
        waitForMessage("completed");
        assertEquals(1, account(1000).getHistory().size());
        assertEquals(new BigDecimal("15.00"), reload().findAccountByNumber(1000).orElseThrow().getBalance());
    }

    @Test void interestAndScreenChangesWorkAtTheMinimumSize() throws Exception {
        twoAccounts();
        bank.deposit(1000, new BigDecimal("1000.00"));
        openApp();
        edt(() -> window.setSize(window.getMinimumSize()));
        click("Monthly interest");
        click("Confirm interest");
        waitForMessage("Interest of R$ 5,00 applied");
        click("Monthly interest");
        click("Confirm interest");
        waitForMessage("already been applied");
        click("Cancel");
        click("Transactions");
        click("Overview");
        edt(() -> assertEquals("R$ 1.005,00", ((JLabel) name("balance")).getText()));
    }

    @Test void transferWithoutDestinationIsDisabled() throws Exception {
        bank.openSavingsAccount(new Customer("Ana", "24681357928"));
        openApp();
        click("Transfer");
        edt(() -> assertFalse(button("Confirm transfer").isEnabled()));
        click("Cancel");
        edt(() -> assertNotNull(name("balance")));
    }

    @Test void slowOperationDoesNotBlockTheEdtOrAllowDoubleSubmitOrClose() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        BankRepository repository = new BankRepository() {
            public java.util.List<StoredAccount> load() { return java.util.List.of(); }
            public void save(java.util.List<StoredAccount> accounts) {
                if (accounts.get(0).history().isEmpty()) return;
                started.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test timed out");
                } catch (InterruptedException e) { throw new IllegalStateException(e); }
            }
        };
        bank = Bank.load(Clock.systemUTC(), repository);
        bank.openSavingsAccount(new Customer("Ana", "24681357928"));
        openApp();
        try {
            operate("Deposit", "Confirm deposit", "10");
            assertTrue(started.await(3, TimeUnit.SECONDS));
            edt(() -> {
                assertFalse(button("Confirm deposit").isEnabled());
                button("Confirm deposit").doClick();
                window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
                assertTrue(window.isDisplayable());
            });
        } finally { release.countDown(); }
        waitForMessage("completed");
        assertEquals(1, account(1000).getHistory().size());
        assertEquals(new BigDecimal("10.00"), account(1000).getBalance());
    }

    @Test void emptyWindowAreaUsesTheSameBackground() throws Exception {
        openApp();
        edt(() -> {
            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(
                    JViewport.class, button("Open first account"));
            assertEquals(Theme.BACKGROUND, viewport.getBackground());
            assertFalse(name("accounts").isVisible());
        });
    }

    @Test void rendersRealScreensForVisualReview() throws Exception {
        twoAccounts();
        bank.deposit(1000, new BigDecimal("2500.00"));
        bank.withdraw(1000, new BigDecimal("120.50"));
        bank.transfer(1000, 1001, new BigDecimal("350.00"));
        openApp();
        capture("summary");
        click("Transfer");
        capture("transfer");
        click("Cancel");
        edt(() -> window.setSize(window.getMinimumSize()));
        capture("summary-compact");
        edt(() -> {
            JButton transfer = button("Transfer");
            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, transfer);
            Rectangle bounds = SwingUtilities.convertRectangle(transfer.getParent(), transfer.getBounds(), viewport);
            assertTrue(bounds.x >= 0 && bounds.x + bounds.width <= viewport.getWidth(),
                    "The Transfer action must not be cut off when the window shrinks");
        });
        click("New account");
        capture("new-account-compact");
    }

    private void capture(String name) throws Exception {
        // Renders the real Swing components, without rebuilding the interface in another format.
        edt(() -> {
            window.validate();
            java.awt.image.BufferedImage screenshot = new java.awt.image.BufferedImage(
                    window.getContentPane().getWidth(), window.getContentPane().getHeight(),
                    java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = screenshot.createGraphics();
            window.getContentPane().printAll(g);
            g.dispose();
            try {
                Path destination = Path.of("target", "visual", name + ".png");
                java.nio.file.Files.createDirectories(destination.getParent());
                javax.imageio.ImageIO.write(screenshot, "png", destination.toFile());
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        });
    }

    private void twoAccounts() {
        bank.openSavingsAccount(new Customer("Ana Pereira", "24681357928"));
        bank.openCheckingAccount(new Customer("Bruno Alves", "13579246828"), BigDecimal.ZERO);
    }
    private Bank reload() { return Bank.load(Clock.systemDefaultZone(), new SqliteRepository(file)); }
    private Account account(int number) { return bank.findAccountByNumber(number).orElseThrow(); }
    private void operate(String operation, String confirm, String amount) throws Exception {
        click(operation); fillField("amount", amount); click(confirm);
    }
    private void click(String text) throws Exception { edt(() -> button(text).doClick()); }
    private void fillField(String name, String amount) throws Exception { edt(() -> ((JTextField) name(name)).setText(amount)); }
    private JButton button(String text) { return (JButton) find(window, c -> c instanceof JButton b && text.equals(b.getText())); }
    private Component name(String name) { return find(window, c -> name.equals(c.getName())); }
    private String message() { return ((JTextArea) name("message")).getText(); }
    private static Component find(Container root, Predicate<Component> condition) {
        for (Component c : root.getComponents()) {
            if (condition.test(c)) return c;
            if (c instanceof Container child) {
                Component found = find(child, condition);
                if (found != null) return found;
            }
        }
        return null;
    }
    private void waitForMessage(String text) throws Exception { waitUntil(() -> message().contains(text)); }
    private void waitUntil(java.util.function.BooleanSupplier condition) throws Exception {
        long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        do {
            AtomicReference<Boolean> result = new AtomicReference<>(false);
            edt(() -> result.set(condition.getAsBoolean()));
            if (result.get()) return;
            Thread.sleep(25);
        } while (System.nanoTime() < limit);
        edt(() -> fail("Expected result did not appear. Message: " + message()));
    }
    private static void edt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
}
