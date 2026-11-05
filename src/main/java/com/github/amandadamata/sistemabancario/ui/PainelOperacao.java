package com.github.amandadamata.sistemabancario.ui;

import com.github.amandadamata.sistemabancario.*;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

final class PainelOperacao extends JPanel {
    enum Tipo {
        DEPOSITO("Deposit"), SAQUE("Withdraw"), TRANSFERENCIA("Transfer");
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
        Componentes.adicionar(formulario, Componentes.explicacao("Account " + conta.getNumero() + " · "
                + conta.getTitular().getNome() + "\nAvailable: " + Moeda.formatar(conta.getSaldoDisponivel())
                + (conta instanceof ContaCorrente ? " (includes overdraft)" : "")), 28);
        JTextField valor = new JTextField();
        valor.setName("valor");
        valor.putClientProperty("JTextField.placeholderText", "0.00");
        Componentes.campo(formulario, "Amount in reais (R$)", valor);
        Componentes.adicionar(formulario, Componentes.explicacao("Example: 1500.50. No thousands separators."), 24);
        JComboBox<Conta> destino = Componentes.contas(contas.stream()
                .filter(c -> c.getNumero() != conta.getNumero()).toList());
        destino.setName("destino");
        if (tipo == Tipo.TRANSFERENCIA) {
            Componentes.campo(formulario, "Destination account", destino);
            if (destino.getItemCount() == 0) {
                Componentes.adicionar(formulario, Componentes.explicacao(
                        "Open another account to make a transfer. Use New account in the menu."), 18);
            }
        }
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        botoes.setOpaque(false);
        JButton enviar = Componentes.principal("Confirm " + switch (tipo) {
            case DEPOSITO -> "deposit";
            case SAQUE -> "withdrawal";
            case TRANSFERENCIA -> "transfer";
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
        botoes.add(Componentes.botao("Cancel", voltar));
        Componentes.adicionar(formulario, botoes, 0);
        add(formulario, BorderLayout.NORTH);
    }
}
