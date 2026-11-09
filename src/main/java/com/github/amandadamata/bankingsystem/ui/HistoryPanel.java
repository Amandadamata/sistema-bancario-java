package com.github.amandadamata.bankingsystem.ui;

import com.github.amandadamata.bankingsystem.Account;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

final class HistoryPanel extends JPanel {
    HistoryPanel(Account account, boolean full) {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        add(Components.text(full ? "Transactions" : "Recent transactions", 20, true), BorderLayout.NORTH);
        HistoryTableModel model = new HistoryTableModel(account, full ? Integer.MAX_VALUE : 5);
        if (model.getRowCount() == 0) {
            JTextArea empty = Components.explanation("No transactions yet.\nMake a deposit to start your history.");
            empty.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));
            add(empty);
            return;
        }
        JTable historyTable = new JTable(model);
        historyTable.setName("history");
        historyTable.getAccessibleContext().setAccessibleName("Transaction history, newest first");
        historyTable.setRowHeight(44);
        historyTable.setShowGrid(false);
        historyTable.setIntercellSpacing(new Dimension(0, 0));
        historyTable.setFillsViewportHeight(true);
        historyTable.getTableHeader().setReorderingAllowed(false);
        historyTable.getTableHeader().setPreferredSize(new Dimension(0, 40));
        historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {170, 270, 155, 155};
        for (int i = 0; i < widths.length; i++) historyTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        historyTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                putClientProperty("html.disable", true);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                setHorizontalAlignment(column >= 2 ? RIGHT : LEFT);
                setForeground(column == 2 && model.input(row) ? Theme.SUCCESS : Theme.TEXT);
                return this;
            }
        });
        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.setPreferredSize(new Dimension(500, full ? 390 : 40 + 44 * model.getRowCount() + 18));
        scroll.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                historyTable.setAutoResizeMode(scroll.getViewport().getWidth() >= 750
                        ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : JTable.AUTO_RESIZE_OFF);
            }
        });
        add(scroll);
    }
}
