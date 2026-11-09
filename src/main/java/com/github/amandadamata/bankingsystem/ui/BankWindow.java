package com.github.amandadamata.bankingsystem.ui;

import com.github.amandadamata.bankingsystem.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

/** Coordinates navigation and events; all financial rules stay in Bank and Account. */
public final class BankWindow extends JFrame {
    private final Bank bank;
    private final JPanel content = new ScrollablePanel();
    private final JPanel header = new JPanel(new BorderLayout(16, 0));
    private final JTextArea message = Components.explanation("");
    private final JButton start = Components.button("Overview", this::showSummary);
    private final JButton statement = Components.button("Transactions", this::showHistory);
    private final JButton newAccount = Components.button("New account", this::showNewAccount);
    private final UiTasks tasks;
    private Account account;

    public BankWindow(Bank bank, Path file, Runnable onClose) {
        super("Linha · Banking System");
        this.bank = bank;
        this.account = bank.listAccounts().stream().findFirst().orElse(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(860, 700));
        setSize(1140, 820);
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        setContentPane(root);
        tasks = new UiTasks(root, this::showError);
        root.add(menu(), BorderLayout.WEST);
        JPanel mainArea = new JPanel(new BorderLayout(0, 22));
        mainArea.setBorder(BorderFactory.createEmptyBorder(26, 30, 16, 30));
        mainArea.add(header, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        mainArea.add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 6));
        message.setRows(2);
        message.setName("message");
        message.getAccessibleContext().setAccessibleName("Operation result");
        footer.add(message, BorderLayout.CENTER);
        JLabel local = Components.text("Study project · Data saved on this computer", 12, false);
        local.setForeground(Theme.SECONDARY);
        local.setToolTipText(file.toAbsolutePath().toString());
        footer.add(local, BorderLayout.SOUTH);
        mainArea.add(footer, BorderLayout.SOUTH);
        root.add(mainArea);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                if (tasks.busy()) {
                    message.setText("Wait for the operation to finish before closing.");
                    return;
                }
                dispose();
                onClose.run();
            }
        });
        updateHeader();
        showSummary();
    }

    private JPanel menu() {
        JPanel menu = new JPanel(new BorderLayout());
        menu.setPreferredSize(new Dimension(190, 0));
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER),
                BorderFactory.createEmptyBorder(34, 20, 26, 20)));
        JPanel items = Components.column();
        JLabel brand = Components.text("linha", 34, true);
        brand.setForeground(Theme.HIGHLIGHT);
        Components.addTo(items, brand, 4);
        JLabel subtitle = Components.text("BANKING SYSTEM", 10, true);
        subtitle.setForeground(Theme.SECONDARY);
        Components.addTo(items, subtitle, 52);
        for (JButton button : new JButton[]{start, statement, newAccount}) {
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            Components.addTo(items, button, 10);
        }
        menu.add(items, BorderLayout.NORTH);
        menu.add(Components.text("Simple. Local. Yours.", 12, false), BorderLayout.SOUTH);
        return menu;
    }

    private void updateHeader() {
        header.removeAll();
        JLabel greeting = Components.text(account == null ? "Welcome to Linha."
                : "Hi, " + account.getHolder().getName().split("\\s+")[0] + ".", 18, true);
        header.add(greeting, BorderLayout.CENTER);
        JComboBox<Account> selector = Components.accounts(bank.listAccounts());
        selector.setName("accounts");
        selector.getAccessibleContext().setAccessibleName("Selected account");
        selector.setPreferredSize(new Dimension(340, 44));
        selector.setSelectedItem(account);
        selector.setVisible(account != null);
        selector.addActionListener(event -> {
            account = (Account) selector.getSelectedItem();
            updateHeader();
            showSummary();
        });
        header.add(selector, BorderLayout.EAST);
        statement.setEnabled(account != null);
        header.revalidate();
        header.repaint();
    }

    private void showPage(JComponent panel, String page) {
        content.removeAll();
        content.setName(page);
        content.add(panel, BorderLayout.NORTH);
        message.setText("");
        start.setBackground(page.equals("summary") ? Color.decode("#EAF0F2") : Color.WHITE);
        statement.setBackground(page.equals("statement") ? Color.decode("#EAF0F2") : Color.WHITE);
        newAccount.setBackground(page.equals("new-account") ? Color.decode("#EAF0F2") : Color.WHITE);
        content.revalidate();
        content.repaint();
        SwingUtilities.invokeLater(() -> {
            content.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
            Component focus = findField(panel);
            if (focus != null) focus.requestFocusInWindow();
        });
    }

    private static Component findField(Container panel) {
        for (Component c : panel.getComponents()) {
            if (c instanceof JTextField) return c;
            if (c instanceof Container child) {
                Component found = findField(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void showSummary() {
        if (account == null) {
            JPanel empty = Components.column();
            Components.addTo(empty, Components.text("A place to start.", 30, true), 18);
            Components.addTo(empty, Components.explanation(
                    "Open your first account to track your balance, make transactions and see your history."), 28);
            Components.addTo(empty, Components.primaryButton("Open first account", this::showNewAccount), 0);
            showPage(empty, "empty");
        } else {
            showPage(new SummaryPanel(account, this::showOperation, this::showHistory,
                    this::showInterest), "summary");
        }
    }

    private void showHistory() {
        if (account != null) showPage(new HistoryPanel(account, true), "statement");
    }

    private void showOperation(OperationPanel.Kind type) {
        showPage(new OperationPanel(type, account, bank.listAccounts(), request -> {
            message.setForeground(Theme.SECONDARY);
            message.setText("Saving…");
            tasks.runMenu(() -> {
                switch (request.type()) {
                    case DEPOSIT -> bank.deposit(request.source(), request.amount());
                    case WITHDRAWAL -> bank.withdraw(request.source(), request.amount());
                    case TRANSFER -> {
                        if (request.destination() == null) throw new IllegalArgumentException("Select the destination account.");
                        bank.transfer(request.source(), request.destination(), request.amount());
                    }
                }
                String amount = Money.format(request.amount());
                return switch (request.type()) {
                    case DEPOSIT -> "Deposit of " + amount + " completed.";
                    case WITHDRAWAL -> "Withdrawal of " + amount + " completed.";
                    case TRANSFER -> "Transfer of " + amount + " completed.";
                };
            }, this::finish);
        }, this::showError, this::showSummary), "operation");
    }

    private void showNewAccount() {
        showPage(new NewAccountPanel(request -> {
            message.setForeground(Theme.SECONDARY);
            message.setText("Saving account…");
            tasks.runMenu(() -> request.checking()
                    ? bank.openCheckingAccount(request.holder(), request.limit())
                    : bank.openSavingsAccount(request.holder()), created -> {
                account = created;
                updateHeader();
                finish("Account " + created.getNumber() + " created.");
            });
        }, this::showError, this::showSummary), "new-account");
    }

    private void showInterest() {
        JPanel panel = Components.column();
        Components.addTo(panel, Components.text("Monthly interest", 30, true), 18);
        Components.addTo(panel, Components.explanation(
                "Applies 0.5% to the current savings balance, once a month.\n"
                + "This is a simplified rule for this project, not a real savings formula."), 24);
        int number = account.getNumber();
        Components.addTo(panel, Components.primaryButton("Confirm interest", () ->
                tasks.runMenu(() -> bank.applyInterest(number), amount -> finish(amount.signum() > 0
                        ? "Interest of " + Money.format(amount) + " applied."
                        : "The balance doesn't earn R$ 0,01 in interest yet."))), 12);
        Components.addTo(panel, Components.button("Cancel", this::showSummary), 0);
        showPage(panel, "interest");
    }

    private void finish(String text) {
        showSummary();
        message.setForeground(Theme.SUCCESS);
        message.setText(text);
    }

    private void showError(String text) {
        message.setForeground(Theme.ERROR);
        message.setText(text);
    }
}
