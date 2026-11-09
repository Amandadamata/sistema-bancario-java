package com.github.amandadamata.bankingsystem;

import com.github.amandadamata.bankingsystem.persistence.SqliteRepository;
import com.github.amandadamata.bankingsystem.ui.BankWindow;
import com.github.amandadamata.bankingsystem.ui.Theme;
import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;

/** JAR entry point: opens the window or, with --terminal, the original menu in {@link Main}. */
public final class DesktopMain {
    private DesktopMain() { }

    public static void main(String[] args) {
        if (terminalMode(args)) {
            Main.main(args);
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Linha needs a graphical environment to open the window.");
            return;
        }
        System.setProperty("apple.awt.application.name", "Linha");
        AppDataFile lock = null;
        try {
            String configured = AppDataFile.configuredPath();
            Path resolved = AppDataFile.resolve(configured, System.getProperty("os.name"), System.getProperty("user.home"));
            Path file = configured == null || configured.isBlank()
                    ? AppDataFile.preferExisting(resolved, AppDataFile.legacyFileNextTo(resolved)) : resolved;
            lock = new AppDataFile(file);
            Bank bank = Bank.load(Clock.systemDefaultZone(), new SqliteRepository(file));
            AppDataFile reservation = lock;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> release(reservation), "release-file"));
            // The initial load runs on the main thread; the window is only created on the EDT.
            SwingUtilities.invokeLater(() -> {
                Theme.install();
                new BankWindow(bank, file, () -> release(reservation)).setVisible(true);
            });
        } catch (IOException | RuntimeException failure) {
            if (lock != null) release(lock);
            SwingUtilities.invokeLater(() -> {
                Theme.install();
                JTextArea message = new JTextArea("Could not open the data.\n" + failure.getMessage());
                message.setEditable(false);
                message.setLineWrap(true);
                message.setWrapStyleWord(true);
                message.setColumns(46);
                message.setRows(5);
                JOptionPane.showMessageDialog(null, message, "Linha · Could not start", JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    static boolean terminalMode(String[] args) {
        return args.length > 0 && "--terminal".equals(args[0]);
    }

    private static void release(AppDataFile file) {
        try { file.close(); }
        catch (IOException failure) { System.err.println("Could not release the file lock."); }
    }
}
