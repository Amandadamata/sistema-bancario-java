package com.github.amandadamata.sistemabancario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ArquivoAplicacaoTest {
    @TempDir Path pasta;

    @Test void caminhoDoMacIndependeDaPastaDeExecucao() {
        assertEquals(Path.of("/Users/teste/Library/Application Support/Linha/sistema-bancario.db"),
                ArquivoAplicacao.resolver(null, "Mac OS X", "/Users/teste"));
        assertEquals(Path.of("/home/teste/.linha/sistema-bancario.db"),
                ArquivoAplicacao.resolver(" ", "Linux", "/home/teste"));
    }

    @Test void caminhoConfiguradoPermiteAbrirDadosAntigos() {
        Path antigo = pasta.resolve("antigo.db");
        assertEquals(antigo, ArquivoAplicacao.resolver(" " + antigo + " ", "Mac OS X", "/ignorado"));
    }

    @Test void segundaInstanciaERecusadaEAReservaELiberadaAoFechar() throws Exception {
        Path db = pasta.resolve("banco.db");
        try (ArquivoAplicacao primeira = new ArquivoAplicacao(db)) {
            assertThrows(IOException.class, () -> new ArquivoAplicacao(db));
        }
        try (ArquivoAplicacao reaberta = new ArquivoAplicacao(db)) {
            assertNotNull(reaberta);
        }
    }

    @Test void linkSimbolicoUsaAMesmaReserva() throws Exception {
        Path real = Files.createDirectory(pasta.resolve("real"));
        Path link = Files.createSymbolicLink(pasta.resolve("link"), real);
        try (ArquivoAplicacao primeira = new ArquivoAplicacao(real.resolve("banco.db"))) {
            assertThrows(IOException.class, () -> new ArquivoAplicacao(link.resolve("banco.db")));
        }
    }
}
