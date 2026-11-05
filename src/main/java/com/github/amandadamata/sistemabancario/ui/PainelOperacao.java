package com.github.amandadamata.sistemabancario.ui;

import com.github.amandadamata.sistemabancario.*;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

final class PainelOperacao extends JPanel {
    enum Tipo {
        DEPOSITO("Depositar"), SAQUE("Sacar"), TRANSFERENCIA("Transferir");
        final String rotulo;
        Tipo(String rotulo) { this.rotulo = rotulo; }
    }

    /** Dados lidos na thread da interface antes de iniciar o trabalho em segundo plano. */
    record Pedido(Tipo tipo, int origem, Integer destino, BigDecimal valor) { }

    PainelOperacao(Tipo tipo, Conta conta, List<Conta> contas,
            Consumer<Pedido> confirmar, Consumer<String> erro, Runnable voltar) {
        super(new BorderLayout());
        setOpaque(false);
        JPanel formulario = Componentes.coluna();
        Componentes.adicionar(formulario, Componentes.texto(tipo.rotulo, 30, true), 14);
        Componentes.adicionar(formulario, Componentes.explicacao("Conta " + conta.getNumero() + " · "
                + conta.getTitular().getNome() + "\nDisponível: " + Moeda.formatar(conta.getSaldoDisponivel())
                + (conta instanceof ContaCorrente ? " (inclui cheque especial)" : "")), 28);
        JTextField valor = new JTextField();
        valor.setName("valor");
        valor.putClientProperty("JTextField.placeholderText", "0,00");
        Componentes.campo(formulario, "Valor em reais", valor);
        Componentes.adicionar(formulario, Componentes.explicacao("Exemplo: 1500,50. Digite sem separador de milhar."), 24);
        JComboBox<Conta> destino = Componentes.contas(contas.stream()
                .filter(c -> c.getNumero() != conta.getNumero()).toList());
        destino.setName("destino");
        if (tipo == Tipo.TRANSFERENCIA) {
            Componentes.campo(formulario, "Conta de destino", destino);
            if (destino.getItemCount() == 0) {
                Componentes.adicionar(formulario, Componentes.explicacao(
                        "Abra outra conta para poder transferir. Use Nova conta no menu."), 18);
            }
        }
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        botoes.setOpaque(false);
        JButton enviar = Componentes.principal("Confirmar " + switch (tipo) {
            case DEPOSITO -> "depósito";
            case SAQUE -> "saque";
            case TRANSFERENCIA -> "transferência";
        }, () -> {
            try {
                Conta selecionada = (Conta) destino.getSelectedItem();
                confirmar.accept(new Pedido(tipo, conta.getNumero(), selecionada == null ? null
                        : selecionada.getNumero(), Moeda.converter(valor.getText())));
            } catch (IllegalArgumentException invalido) {
                erro.accept(invalido.getMessage());
                valor.requestFocusInWindow();
            }
        });
        enviar.setName("confirmar");
        enviar.setEnabled(tipo != Tipo.TRANSFERENCIA || destino.getItemCount() > 0);
        valor.addActionListener(evento -> enviar.doClick());
        botoes.add(enviar);
        botoes.add(Box.createHorizontalStrut(12));
        botoes.add(Componentes.botao("Cancelar", voltar));
        Componentes.adicionar(formulario, botoes, 0);
        add(formulario, BorderLayout.NORTH);
    }
}
