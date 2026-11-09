package com.github.amandadamata.bankingsystem.ui;

import javax.swing.*;
import java.awt.*;

/** Small vector icons, with no external images and no dependency on symbol fonts. */
final class ArrowIcon implements Icon {
    private final int direction;
    ArrowIcon(int direction) { this.direction = direction; }
    public int getIconWidth() { return 22; }
    public int getIconHeight() { return 22; }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D drawing = (Graphics2D) g.create();
        drawing.translate(x, y);
        drawing.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawing.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawing.setColor(c.isEnabled() ? Theme.HIGHLIGHT : Theme.SECONDARY);
        if (direction == 0) {
            drawing.drawLine(3, 7, 18, 7);
            drawing.drawLine(14, 3, 18, 7);
            drawing.drawLine(18, 7, 14, 11);
            drawing.drawLine(18, 15, 3, 15);
            drawing.drawLine(7, 11, 3, 15);
            drawing.drawLine(3, 15, 7, 19);
        } else {
            if (direction < 0) { drawing.translate(22, 22); drawing.rotate(Math.PI); }
            drawing.drawLine(11, 3, 11, 17);
            drawing.drawLine(6, 12, 11, 17);
            drawing.drawLine(11, 17, 16, 12);
        }
        drawing.dispose();
    }
}
