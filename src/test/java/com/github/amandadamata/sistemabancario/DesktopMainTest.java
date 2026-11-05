package com.github.amandadamata.sistemabancario;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DesktopMainTest {
    @Test void semArgumentosAbreAJanela() {
        assertFalse(DesktopMain.modoTerminal(new String[0]));
    }

    @Test void opcaoTerminalAbreOMenu() {
        assertTrue(DesktopMain.modoTerminal(new String[]{"--terminal"}));
    }
}
