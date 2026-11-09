package com.github.amandadamata.bankingsystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class AppDataFileTest {
    @TempDir Path folder;

    @Test void macPathDoesNotDependOnTheWorkingFolder() {
        assertEquals(Path.of("/Users/test/Library/Application Support/Linha/linha.db"),
                AppDataFile.resolve(null, "Mac OS X", "/Users/test"));
        assertEquals(Path.of("/home/test/.linha/linha.db"),
                AppDataFile.resolve(" ", "Linux", "/home/test"));
    }

    @Test void configuredPathOpensOlderData() {
        Path old = folder.resolve("old.db");
        assertEquals(old, AppDataFile.resolve(" " + old + " ", "Mac OS X", "/ignored"));
    }

    @Test void fileFromAnOlderVersionIsKeptUntilTheNewOneExists() throws IOException {
        Path current = folder.resolve("linha.db");
        Path legacy = AppDataFile.legacyFileNextTo(current);
        assertEquals(current, AppDataFile.preferExisting(current, legacy));

        Files.writeString(legacy, "");
        assertEquals(legacy, AppDataFile.preferExisting(current, legacy));

        Files.writeString(current, "");
        assertEquals(current, AppDataFile.preferExisting(current, legacy));
    }

    @Test void secondInstanceIsRejectedAndTheLockIsReleasedOnClose() throws Exception {
        Path db = folder.resolve("bank.db");
        try (AppDataFile first = new AppDataFile(db)) {
            assertThrows(IOException.class, () -> new AppDataFile(db));
        }
        try (AppDataFile reopened = new AppDataFile(db)) {
            assertNotNull(reopened);
        }
    }

    @Test void symbolicLinkUsesTheSameLock() throws Exception {
        Path real = Files.createDirectory(folder.resolve("real"));
        Path link = Files.createSymbolicLink(folder.resolve("link"), real);
        try (AppDataFile first = new AppDataFile(real.resolve("bank.db"))) {
            assertThrows(IOException.class, () -> new AppDataFile(link.resolve("bank.db")));
        }
    }
}
