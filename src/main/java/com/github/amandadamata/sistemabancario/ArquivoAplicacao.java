package com.github.amandadamata.sistemabancario;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Caminho estável para abrir pelo Finder e trava entre instâncias desktop. */
final class ArquivoAplicacao implements AutoCloseable {
    private final FileChannel canal;
    private final FileLock trava;

    static Path resolver(String configurado, String sistema, String pastaUsuario) {
        if (configurado != null && !configurado.isBlank()) return Path.of(configurado.trim()).toAbsolutePath();
        Path base = Path.of(pastaUsuario);
        return (sistema.startsWith("Mac") ? base.resolve("Library/Application Support/Linha")
                : base.resolve(".linha")).resolve("sistema-bancario.db");
    }

    ArquivoAplicacao(Path arquivo) throws IOException {
        Path absoluto = arquivo.toAbsolutePath();
        Files.createDirectories(absoluto.getParent());
        // Resolve links simbólicos para que duas referências ao mesmo arquivo usem a mesma trava.
        Path real = Files.exists(absoluto) ? absoluto.toRealPath()
                : absoluto.getParent().toRealPath().resolve(absoluto.getFileName());
        canal = FileChannel.open(real.resolveSibling(real.getFileName() + ".lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock obtida;
        try {
            obtida = canal.tryLock();
        } catch (IOException | OverlappingFileLockException falha) {
            canal.close();
            throw new IOException("Não foi possível reservar o arquivo. Verifique se o Linha já está aberto.", falha);
        }
        if (obtida == null) {
            canal.close();
            throw new IOException("O Linha já está usando este arquivo. Feche a outra janela antes de continuar.");
        }
        trava = obtida;
    }

    public void close() throws IOException {
        try { if (trava.isValid()) trava.release(); }
        finally { canal.close(); }
    }
}
