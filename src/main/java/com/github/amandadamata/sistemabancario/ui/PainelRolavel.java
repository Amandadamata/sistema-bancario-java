package com.github.amandadamata.sistemabancario.ui;

import javax.swing.*;
import java.awt.*;

/** Acompanha a largura da janela; apenas a altura pode precisar de rolagem. */
final class PainelRolavel extends JPanel implements Scrollable {
    PainelRolavel() { super(new BorderLayout()); }
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    public int getScrollableUnitIncrement(Rectangle area, int orientacao, int direcao) { return 18; }
    public int getScrollableBlockIncrement(Rectangle area, int orientacao, int direcao) {
        return Math.max(18, area.height - 18);
    }
    public boolean getScrollableTracksViewportWidth() { return true; }
    public boolean getScrollableTracksViewportHeight() { return false; }
}
