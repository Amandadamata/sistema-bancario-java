package com.github.amandadamata.bankingsystem.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;

/** Visual identity in one place; the domain does not depend on these choices. */
public final class Theme {
    public static final Color BACKGROUND = Color.decode("#F8F9F7");
    public static final Color TEXT = Color.decode("#242827");
    public static final Color HIGHLIGHT = Color.decode("#627D8A");
    public static final Color BORDER = Color.decode("#E5E9E7");
    // A little darker than the reference so small text stays readable.
    public static final Color SECONDARY = Color.decode("#626D68");
    public static final Color SUCCESS = Color.decode("#376B55");
    public static final Color ERROR = Color.decode("#A04444");

    private Theme() { }

    public static void install() {
        FlatLightLaf.setup();
        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Viewport.background", BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Component.focusColor", HIGHLIGHT);
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Button.background", Color.WHITE);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.margin", new Insets(12, 16, 12, 16));
        UIManager.put("TextField.margin", new Insets(10, 12, 10, 12));
        UIManager.put("ComboBox.padding", new Insets(8, 10, 8, 10));
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("TableHeader.background", BACKGROUND);
        UIManager.put("TableHeader.foreground", SECONDARY);
        UIManager.put("Table.selectionBackground", Color.decode("#EAF0F2"));
        UIManager.put("Table.selectionForeground", TEXT);
    }
}
