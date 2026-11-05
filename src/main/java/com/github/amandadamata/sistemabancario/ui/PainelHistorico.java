package com.github.amandadamata.sistemabancario.ui;

import com.github.amandadamata.sistemabancario.Conta;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

final class PainelHistorico extends JPanel {
    PainelHistorico(Conta conta, boolean completo) {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        add(Componentes.texto(completo ? "Movimentações" : "Últimas movimentações", 20, true), BorderLayout.NORTH);
        ModeloHistorico modelo = new ModeloHistorico(conta, completo ? Integer.MAX_VALUE : 5);
        if (modelo.getRowCount() == 0) {
            JTextArea vazio = Componentes.explicacao("Nenhuma movimentação por enquanto.\nFaça um depósito para começar seu histórico.");
            vazio.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));
            add(vazio);
            return;
        }
        JTable tabela = new JTable(modelo);
        tabela.setName("historico");
        tabela.getAccessibleContext().setAccessibleName("Histórico de movimentações, mais recentes primeiro");
        tabela.setRowHeight(44);
        tabela.setShowGrid(false);
        tabela.setIntercellSpacing(new Dimension(0, 0));
        tabela.setFillsViewportHeight(true);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tabela.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] larguras = {170, 270, 155, 155};
        for (int i = 0; i < larguras.length; i++) tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        tabela.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                putClientProperty("html.disable", true);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                setHorizontalAlignment(column >= 2 ? RIGHT : LEFT);
                setForeground(column == 2 && modelo.entrada(row) ? Tema.SUCESSO : Tema.TEXTO);
                return this;
            }
        });
        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setBorder(BorderFactory.createLineBorder(Tema.BORDA));
        rolagem.setPreferredSize(new Dimension(500, completo ? 390 : 40 + 44 * modelo.getRowCount() + 18));
        rolagem.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                tabela.setAutoResizeMode(rolagem.getViewport().getWidth() >= 750
                        ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : JTable.AUTO_RESIZE_OFF);
            }
        });
        add(rolagem);
    }
}
