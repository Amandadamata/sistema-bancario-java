package com.github.amandadamata.bankingsystem;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Stable file location for launching from Finder, plus a lock between desktop instances. */
final class AppDataFile implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    /** Environment variable for a custom data file. Versions up to 1.3 read SISTEMA_BANCARIO_DB. */
    static final String FILE_VARIABLE = "LINHA_DB";
    private static final String LEGACY_FILE_VARIABLE = "SISTEMA_BANCARIO_DB";
    private static final String LEGACY_FILE_NAME = "sistema-bancario.db";

    /** The configured path, accepting the old variable name so existing setups keep working. */
    static String configuredPath() {
        String current = System.getenv(FILE_VARIABLE);
        return current != null && !current.isBlank() ? current : System.getenv(LEGACY_FILE_VARIABLE);
    }

    /**
     * Keeps using the file created by versions up to 1.3 when it exists and the new one does not,
     * so an update never hides the saved accounts.
     */
    static Path preferExisting(Path current, Path legacy) {
        return Files.notExists(current) && Files.exists(legacy) ? legacy : current;
    }

    static Path legacyFileNextTo(Path current) {
        return current.resolveSibling(LEGACY_FILE_NAME);
    }

    static Path resolve(String configured, String osName, String userHome) {
        if (configured != null && !configured.isBlank()) return Path.of(configured.trim()).toAbsolutePath();
        Path base = Path.of(userHome);
        return (osName.startsWith("Mac") ? base.resolve("Library/Application Support/Linha")
                : base.resolve(".linha")).resolve("linha.db");
    }

    AppDataFile(Path file) throws IOException {
        Path absolute = file.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        // Resolves symbolic links so that two paths to the same file share the same lock.
        Path real = Files.exists(absolute) ? absolute.toRealPath()
                : absolute.getParent().toRealPath().resolve(absolute.getFileName());
        channel = FileChannel.open(real.resolveSibling(real.getFileName() + ".lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = channel.tryLock();
        } catch (IOException | OverlappingFileLockException failure) {
            channel.close();
            throw new IOException("Could not lock the file. Check whether Linha is already open.", failure);
        }
        if (acquired == null) {
            channel.close();
            throw new IOException("Linha is already using this file. Close the other window first.");
        }
        lock = acquired;
    }

    public void close() throws IOException {
        try { if (lock.isValid()) lock.release(); }
        finally { channel.close(); }
    }
}
