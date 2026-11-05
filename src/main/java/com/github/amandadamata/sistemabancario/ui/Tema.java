package com.github.amandadamata.sistemabancario.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;

/** Identidade visual centralizada; o domínio não depende destas escolhas. */
public final class Tema {
    public static final Color FUNDO = Color.decode("#F8F9F7");
    public static final Color TEXTO = Color.decode("#242827");
    public static final Color DESTAQUE = Color.decode("#627D8A");
    public static final Color BORDA = Color.decode("#E5E9E7");
    // Um pouco mais escuro que a referência para legibilidade de textos pequenos.
    public static final Color SECUNDARIO = Color.decode("#626D68");
    public static final Color SUCESSO = Color.decode("#376B55");
    public static final Color ERRO = Color.decode("#A04444");

    private Tema() { }

    public static void instalar() {
        FlatLightLaf.setup();
        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        UIManager.put("Panel.background", FUNDO);
        UIManager.put("Viewport.background", FUNDO);
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("Component.focusColor", DESTAQUE);
        UIManager.put("Component.borderColor", BORDA);
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Button.background", Color.WHITE);
        UIManager.put("Button.foreground", TEXTO);
        UIManager.put("Button.margin", new Insets(12, 16, 12, 16));
        UIManager.put("TextField.margin", new Insets(10, 12, 10, 12));
        UIManager.put("ComboBox.padding", new Insets(8, 10, 8, 10));
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", TEXTO);
        UIManager.put("TableHeader.background", FUNDO);
        UIManager.put("TableHeader.foreground", SECUNDARIO);
        UIManager.put("Table.selectionBackground", Color.decode("#EAF0F2"));
        UIManager.put("Table.selectionForeground", TEXTO);
    }
}
