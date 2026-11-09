package com.github.amandadamata.bankingsystem.ui;

import com.github.amandadamata.bankingsystem.*;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

final class SummaryPanel extends JPanel {
    SummaryPanel(Account account, Consumer<OperationPanel.Kind> operate, Runnable statement, Runnable interest) {
        super(new BorderLayout(0, 18));
        setOpaque(false);
        JPanel top = Components.column();
        Components.addTo(top, Components.text("Your account at a glance.", 27, true), 18);
        JPanel balance = Components.column();
        balance.setOpaque(true);
        balance.setBackground(Color.WHITE);
        balance.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)));
        Components.addTo(balance, Components.text("Balance", 14, false), 8);
        JLabel amount = Components.text(Money.format(account.getBalance()), 38, true);
        amount.setName("balance");
        Components.addTo(balance, amount, 12);
        String detail = "Available to use: " + Money.format(account.getAvailableBalance());
        if (account instanceof CheckingAccount checking) {
            detail += "  •  Includes a limit of " + Money.format(checking.getOverdraftLimit());
        }
        Components.addTo(balance, Components.explanation(detail), 0);
        Components.addTo(top, balance, 18);
        JPanel actions = new JPanel(new GridLayout(1, 3, 12, 0));
        actions.setOpaque(false);
        int[] directions = {1, -1, 0};
        int index = 0;
        for (OperationPanel.Kind type : OperationPanel.Kind.values()) {
            JButton button = Components.button(type.label, () -> operate.accept(type));
            button.setIcon(new ArrowIcon(directions[index++]));
            button.setIconTextGap(12);
            actions.add(button);
        }
        Components.addTo(top, actions, 0);
        add(top, BorderLayout.NORTH);
        add(new HistoryPanel(account, false), BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        footer.add(Components.button("View all transactions", statement));
        if (account instanceof SavingsAccount) {
            footer.add(Box.createHorizontalStrut(12));
            footer.add(Components.button("Monthly interest", interest));
        }
        add(footer, BorderLayout.SOUTH);
    }
}
