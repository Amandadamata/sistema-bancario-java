package com.github.amandadamata.bankingsystem.ui;

import com.github.amandadamata.bankingsystem.*;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.function.Consumer;

final class NewAccountPanel extends JPanel {
    record Request(Customer holder, boolean checking, BigDecimal limit) { }

    NewAccountPanel(Consumer<Request> confirm, Consumer<String> error, Runnable goBack) {
        super(new BorderLayout());
        setOpaque(false);
        JPanel form = Components.column();
        Components.addTo(form, Components.text("New account", 30, true), 12);
        Components.addTo(form, Components.explanation(
                "Add the account holder to get started. Accounts are local and no real money moves."), 24);
        JTextField name = new JTextField();
        name.setName("name");
        JTextField cpf = new JTextField();
        cpf.setName("cpf");
        JComboBox<String> type = new JComboBox<>(new String[]{"Checking Account", "Savings Account"});
        type.setName("type");
        JTextField limit = new JTextField("0.00");
        limit.setName("limit");
        Components.field(form, "Account holder name", name);
        Components.field(form, "CPF (Brazilian tax ID)", cpf);
        Components.field(form, "Account type", type);
        Components.field(form, "Overdraft limit (R$)", limit);
        type.addActionListener(event -> limit.setEnabled(type.getSelectedIndex() == 0));
        Components.addTo(form, Components.explanation(
                "Leave the limit at zero to spend only the balance. Savings accounts have no overdraft."), 22);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        JButton createButton = Components.primaryButton("Create account", () -> {
            try {
                boolean checking = type.getSelectedIndex() == 0;
                confirm.accept(new Request(new Customer(name.getText(), cpf.getText()), checking,
                        checking ? Money.parse(limit.getText()) : BigDecimal.ZERO));
            } catch (IllegalArgumentException invalid) {
                error.accept(invalid.getMessage());
            }
        });
        createButton.setName("create");
        buttons.add(createButton);
        buttons.add(Box.createHorizontalStrut(12));
        buttons.add(Components.button("Cancel", goBack));
        Components.addTo(form, buttons, 0);
        add(form, BorderLayout.NORTH);
    }
}
