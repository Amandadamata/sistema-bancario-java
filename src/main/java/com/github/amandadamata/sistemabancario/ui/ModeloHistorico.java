package com.github.amandadamata.sistemabancario.ui;

import com.github.amandadamata.sistemabancario.*;
import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Traduz o extrato existente para linhas de uma JTable, sem alterar o domínio. */
final class ModeloHistorico extends AbstractTableModel {
    private static final String[] COLUNAS = {"Date and time", "Transaction", "Amount", "Balance after"};
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final List<Transacao> linhas;

    ModeloHistorico(Conta conta, int limite) {
        List<Transacao> recentes = new ArrayList<>(conta.getHistorico());
        Collections.reverse(recentes);
        linhas = List.copyOf(recentes.subList(0, Math.min(limite, recentes.size())));
    }

    public int getRowCount() { return linhas.size(); }
    public int getColumnCount() { return COLUNAS.length; }
    public String getColumnName(int coluna) { return COLUNAS[coluna]; }
    boolean entrada(int linha) { return linhas.get(linha).tipo().isCredito(); }

    public Object getValueAt(int linha, int coluna) {
        Transacao transacao = linhas.get(linha);
        return switch (coluna) {
            case 0 -> transacao.dataHora().format(DATA);
            case 1 -> transacao.tipo().getDescricao() + (transacao.contaRelacionada().isPresent()
                    ? " · account " + transacao.contaRelacionada().getAsInt() : "");
            case 2 -> (transacao.tipo().isCredito() ? "+ " : "− ") + Moeda.formatar(transacao.valor());
            case 3 -> Moeda.formatar(transacao.saldoResultante());
            default -> throw new IndexOutOfBoundsException(coluna);
        };
    }
}
