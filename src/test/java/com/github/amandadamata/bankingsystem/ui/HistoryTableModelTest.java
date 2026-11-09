package com.github.amandadamata.bankingsystem.ui;

import com.github.amandadamata.bankingsystem.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class HistoryTableModelTest {
    @Test void showsNewestFirstWithSignsDatesAndAccounts() throws Exception {
        Bank bank = new Bank(Clock.fixed(Instant.parse("2026-10-24T12:30:15Z"), ZoneOffset.UTC));
        Account account = bank.openSavingsAccount(new Customer("Ana", "24681357928"));
        bank.openSavingsAccount(account.getHolder());
        bank.deposit(1000, new BigDecimal("100.50"));
        bank.transfer(1000, 1001, new BigDecimal("25.10"));
        HistoryTableModel model = new HistoryTableModel(account, 5);
        assertEquals("24/10/2026 12:30:15", model.getValueAt(0, 0));
        assertEquals("Transfer sent · account 1001", model.getValueAt(0, 1));
        assertEquals("− R$ 25,10", model.getValueAt(0, 2));
        assertEquals("R$ 75,40", model.getValueAt(0, 3));
        assertEquals("+ R$ 100,50", model.getValueAt(1, 2));
        assertFalse(model.input(0));
        assertTrue(model.input(1));
        assertFalse(model.isCellEditable(0, 2));
        assertEquals(TransactionType.DEPOSIT, account.getHistory().get(0).type());
        assertEquals(1, new HistoryTableModel(account, 1).getRowCount());
    }

    @Test void accountWithoutOperationsHasAnEmptyStatement() {
        Bank bank = new Bank(Clock.systemUTC());
        Account account = bank.openSavingsAccount(new Customer("Ana", "24681357928"));
        assertEquals(0, new HistoryTableModel(account, 5).getRowCount());
    }
}
