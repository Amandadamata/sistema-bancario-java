package com.github.amandadamata.bankingsystem.ui;

import com.github.amandadamata.bankingsystem.Account;
import javax.swing.*;
import java.awt.*;

final class Components {
    private Components() { }

    static JLabel text(String text, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.putClientProperty("html.disable", true);
        label.setFont(label.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, size));
        return label;
    }

    static JTextArea explanation(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        // Without FlatLaf's default margin, the text lines up with titles and fields.
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setForeground(Theme.SECONDARY);
        return area;
    }

    static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    static void addTo(JPanel column, JComponent component, int spaceAfter) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(component);
        if (spaceAfter > 0) column.add(Box.createVerticalStrut(spaceAfter));
    }

    static JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    static JButton primaryButton(String text, Runnable action) {
        JButton button = button(text, action);
        button.setBackground(Theme.HIGHLIGHT);
        button.setForeground(Color.WHITE);
        return button;
    }

    static void field(JPanel column, String caption, JComponent field) {
        JLabel label = text(caption, 14, true);
        label.setLabelFor(field);
        field.getAccessibleContext().setAccessibleName(caption);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        addTo(column, label, 8);
        addTo(column, field, 20);
    }

    static JComboBox<Account> accounts(java.util.List<Account> accounts) {
        JComboBox<Account> combo = new JComboBox<>(accounts.toArray(Account[]::new));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                label.putClientProperty("html.disable", true);
                if (value instanceof Account account) {
                    label.setText(account.getNumber() + " · " + account.getHolder().getName()
                            + " · " + account.getAccountType().replace("Account ", ""));
                }
                return label;
            }
        });
        return combo;
    }
}
