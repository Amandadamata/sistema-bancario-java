package com.github.amandadamata.bankingsystem.ui;

import com.github.amandadamata.bankingsystem.*;
import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Turns the existing statement into JTable rows, without changing the domain. */
final class HistoryTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"Date and time", "Transaction", "Amount", "Balance after"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final List<Transaction> rows;

    HistoryTableModel(Account account, int limit) {
        List<Transaction> recent = new ArrayList<>(account.getHistory());
        Collections.reverse(recent);
        rows = List.copyOf(recent.subList(0, Math.min(limit, recent.size())));
    }

    public int getRowCount() { return rows.size(); }
    public int getColumnCount() { return COLUMNS.length; }
    public String getColumnName(int column) { return COLUMNS[column]; }
    boolean input(int line) { return rows.get(line).type().isCredit(); }

    public Object getValueAt(int line, int column) {
        Transaction transaction = rows.get(line);
        return switch (column) {
            case 0 -> transaction.dateTime().format(DATE_FORMAT);
            case 1 -> transaction.type().getDescription() + (transaction.relatedAccount().isPresent()
                    ? " · account " + transaction.relatedAccount().getAsInt() : "");
            case 2 -> (transaction.type().isCredit() ? "+ " : "− ") + Money.format(transaction.amount());
            case 3 -> Money.format(transaction.resultingBalance());
            default -> throw new IndexOutOfBoundsException(column);
        };
    }
}
