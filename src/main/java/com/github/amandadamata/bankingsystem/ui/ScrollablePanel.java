package com.github.amandadamata.bankingsystem.ui;

import javax.swing.*;
import java.awt.*;

/** Follows the window width; only the height may need scrolling. */
final class ScrollablePanel extends JPanel implements Scrollable {
    ScrollablePanel() { super(new BorderLayout()); }
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    public int getScrollableUnitIncrement(Rectangle area, int orientation, int direction) { return 18; }
    public int getScrollableBlockIncrement(Rectangle area, int orientation, int direction) {
        return Math.max(18, area.height - 18);
    }
    public boolean getScrollableTracksViewportWidth() { return true; }
    public boolean getScrollableTracksViewportHeight() { return false; }
}
