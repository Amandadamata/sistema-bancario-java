package com.github.amandadamata.bankingsystem;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DesktopMainTest {
    @Test void noArgumentsOpensTheWindow() {
        assertFalse(DesktopMain.terminalMode(new String[0]));
    }

    @Test void terminalOptionOpensTheMenu() {
        assertTrue(DesktopMain.terminalMode(new String[]{"--terminal"}));
    }
}
