package com.github.amandadamata.bankingsystem.ui;

import com.github.amandadamata.bankingsystem.*;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

final class OperationPanel extends JPanel {
    enum Kind {
        DEPOSIT("Deposit"), WITHDRAWAL("Withdraw"), TRANSFER("Transfer");
        final String label;
        Kind(String label) { this.label = label; }
    }

    /** Data read on the UI thread before the background work starts. */
    record Request(Kind type, int source, Integer destination, BigDecimal amount) { }

    OperationPanel(Kind type, Account account, List<Account> accounts,
            Consumer<Request> confirm, Consumer<String> error, Runnable goBack) {
        super(new BorderLayout());
        setOpaque(false);
        JPanel form = Components.column();
        Components.addTo(form, Components.text(type.label, 30, true), 14);
        Components.addTo(form, Components.explanation("Account " + account.getNumber() + " · "
                + account.getHolder().getName() + "\nAvailable: " + Money.format(account.getAvailableBalance())
                + (account instanceof CheckingAccount ? " (includes overdraft)" : "")), 28);
        JTextField amount = new JTextField();
        amount.setName("amount");
        amount.putClientProperty("JTextField.placeholderText", "0.00");
        Components.field(form, "Amount in reais (R$)", amount);
        Components.addTo(form, Components.explanation("Example: 1500.50. No thousands separators."), 24);
        JComboBox<Account> destination = Components.accounts(accounts.stream()
                .filter(c -> c.getNumber() != account.getNumber()).toList());
        destination.setName("destination");
        if (type == Kind.TRANSFER) {
            Components.field(form, "Destination account", destination);
            if (destination.getItemCount() == 0) {
                Components.addTo(form, Components.explanation(
                        "Open another account to make a transfer. Use New account in the menu."), 18);
            }
        }
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        JButton submit = Components.primaryButton("Confirm " + switch (type) {
            case DEPOSIT -> "deposit";
            case WITHDRAWAL -> "withdrawal";
            case TRANSFER -> "transfer";
        }, () -> {
            try {
                Account chosen = (Account) destination.getSelectedItem();
                confirm.accept(new Request(type, account.getNumber(), chosen == null ? null
                        : chosen.getNumber(), Money.parse(amount.getText())));
            } catch (IllegalArgumentException invalid) {
                error.accept(invalid.getMessage());
                amount.requestFocusInWindow();
            }
        });
        submit.setName("confirm");
        submit.setEnabled(type != Kind.TRANSFER || destination.getItemCount() > 0);
        amount.addActionListener(event -> submit.doClick());
        buttons.add(submit);
        buttons.add(Box.createHorizontalStrut(12));
        buttons.add(Components.button("Cancel", goBack));
        Components.addTo(form, buttons, 0);
        add(form, BorderLayout.NORTH);
    }
}
