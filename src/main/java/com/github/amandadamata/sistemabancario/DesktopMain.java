package com.github.amandadamata.sistemabancario;

import com.github.amandadamata.sistemabancario.persistencia.RepositorioSqlite;
import com.github.amandadamata.sistemabancario.ui.JanelaBanco;
import com.github.amandadamata.sistemabancario.ui.Tema;
import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;

/** Ponto de entrada do JAR: abre a janela ou, com --terminal, o menu original de {@link Main}. */
public final class DesktopMain {
    private DesktopMain() { }

    public static void main(String[] args) {
        if (modoTerminal(args)) {
            Main.main(args);
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Linha needs a graphical environment to open the window.");
            return;
        }
        System.setProperty("apple.awt.application.name", "Linha");
        ArquivoAplicacao trava = null;
        try {
            Path arquivo = ArquivoAplicacao.resolver(System.getenv("SISTEMA_BANCARIO_DB"),
                    System.getProperty("os.name"), System.getProperty("user.home"));
            trava = new ArquivoAplicacao(arquivo);
            Banco banco = Banco.carregar(Clock.systemDefaultZone(), new RepositorioSqlite(arquivo));
            ArquivoAplicacao reserva = trava;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> liberar(reserva), "liberar-arquivo"));
            // A leitura inicial ocorre na thread principal; a janela só nasce na EDT.
            SwingUtilities.invokeLater(() -> {
                Tema.instalar();
                new JanelaBanco(banco, arquivo, () -> liberar(reserva)).setVisible(true);
            });
        } catch (IOException | RuntimeException falha) {
            if (trava != null) liberar(trava);
            SwingUtilities.invokeLater(() -> {
                Tema.instalar();
                JTextArea mensagem = new JTextArea("Could not open the data.\n" + falha.getMessage());
                mensagem.setEditable(false);
                mensagem.setLineWrap(true);
                mensagem.setWrapStyleWord(true);
                mensagem.setColumns(46);
                mensagem.setRows(5);
                JOptionPane.showMessageDialog(null, mensagem, "Linha · Could not start", JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    static boolean modoTerminal(String[] args) {
        return args.length > 0 && "--terminal".equals(args[0]);
    }

    private static void liberar(ArquivoAplicacao arquivo) {
        try { arquivo.close(); }
        catch (IOException falha) { System.err.println("Could not release the file lock."); }
    }
}
