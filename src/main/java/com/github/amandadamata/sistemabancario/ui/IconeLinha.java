package com.github.amandadamata.sistemabancario.ui;

import javax.swing.*;
import java.awt.*;

/** Ícones vetoriais pequenos, sem imagens externas nem dependência de fontes de símbolos. */
final class IconeLinha implements Icon {
    private final int direcao;
    IconeLinha(int direcao) { this.direcao = direcao; }
    public int getIconWidth() { return 22; }
    public int getIconHeight() { return 22; }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D desenho = (Graphics2D) g.create();
        desenho.translate(x, y);
        desenho.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        desenho.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        desenho.setColor(c.isEnabled() ? Tema.DESTAQUE : Tema.SECUNDARIO);
        if (direcao == 0) {
            desenho.drawLine(3, 7, 18, 7);
            desenho.drawLine(14, 3, 18, 7);
            desenho.drawLine(18, 7, 14, 11);
            desenho.drawLine(18, 15, 3, 15);
            desenho.drawLine(7, 11, 3, 15);
            desenho.drawLine(3, 15, 7, 19);
        } else {
            if (direcao < 0) { desenho.translate(22, 22); desenho.rotate(Math.PI); }
            desenho.drawLine(11, 3, 11, 17);
            desenho.drawLine(6, 12, 11, 17);
            desenho.drawLine(11, 17, 16, 12);
        }
        desenho.dispose();
    }
}
