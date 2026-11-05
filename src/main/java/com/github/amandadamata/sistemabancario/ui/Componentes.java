package com.github.amandadamata.sistemabancario.ui;

import com.github.amandadamata.sistemabancario.Conta;
import javax.swing.*;
import java.awt.*;

final class Componentes {
    private Componentes() { }

    static JLabel texto(String texto, int tamanho, boolean negrito) {
        JLabel label = new JLabel(texto);
        label.putClientProperty("html.disable", true);
        label.setFont(label.getFont().deriveFont(negrito ? Font.BOLD : Font.PLAIN, tamanho));
        return label;
    }

    static JTextArea explicacao(String texto) {
        JTextArea area = new JTextArea(texto);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        // Sem a margem padrão do FlatLaf, o texto alinha com títulos e campos.
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setForeground(Tema.SECUNDARIO);
        return area;
    }

    static JPanel coluna() {
        JPanel painel = new JPanel();
        painel.setOpaque(false);
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        return painel;
    }

    static void adicionar(JPanel coluna, JComponent componente, int espacoDepois) {
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        coluna.add(componente);
        if (espacoDepois > 0) coluna.add(Box.createVerticalStrut(espacoDepois));
    }

    static JButton botao(String texto, Runnable acao) {
        JButton botao = new JButton(texto);
        botao.addActionListener(evento -> acao.run());
        return botao;
    }

    static JButton principal(String texto, Runnable acao) {
        JButton botao = botao(texto, acao);
        botao.setBackground(Tema.DESTAQUE);
        botao.setForeground(Color.WHITE);
        return botao;
    }

    static void campo(JPanel coluna, String rotulo, JComponent campo) {
        JLabel label = texto(rotulo, 14, true);
        label.setLabelFor(campo);
        campo.getAccessibleContext().setAccessibleName(rotulo);
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        adicionar(coluna, label, 8);
        adicionar(coluna, campo, 20);
    }

    static JComboBox<Conta> contas(java.util.List<Conta> contas) {
        JComboBox<Conta> combo = new JComboBox<>(contas.toArray(Conta[]::new));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                label.putClientProperty("html.disable", true);
                if (value instanceof Conta conta) {
                    label.setText(conta.getNumero() + " · " + conta.getTitular().getNome()
                            + " · " + conta.getTipoConta().replace("Conta ", ""));
                }
                return label;
            }
        });
        return combo;
    }
}
