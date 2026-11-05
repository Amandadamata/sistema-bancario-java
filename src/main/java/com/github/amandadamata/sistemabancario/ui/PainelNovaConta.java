package com.github.amandadamata.sistemabancario.ui;

import com.github.amandadamata.sistemabancario.*;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.function.Consumer;

final class PainelNovaConta extends JPanel {
    record Pedido(Cliente titular, boolean corrente, BigDecimal limite) { }

    PainelNovaConta(Consumer<Pedido> confirmar, Consumer<String> erro, Runnable voltar) {
        super(new BorderLayout());
        setOpaque(false);
        JPanel formulario = Componentes.coluna();
        Componentes.adicionar(formulario, Componentes.texto("New account", 30, true), 12);
        Componentes.adicionar(formulario, Componentes.explicacao(
                "Add the account holder to get started. Accounts are local and no real money moves."), 24);
        JTextField nome = new JTextField();
        nome.setName("nome");
        JTextField cpf = new JTextField();
        cpf.setName("cpf");
        JComboBox<String> tipo = new JComboBox<>(new String[]{"Checking Account", "Savings Account"});
        tipo.setName("tipo");
        JTextField limite = new JTextField("0.00");
        limite.setName("limite");
        Componentes.campo(formulario, "Account holder name", nome);
        Componentes.campo(formulario, "CPF (Brazilian tax ID)", cpf);
        Componentes.campo(formulario, "Account type", tipo);
        Componentes.campo(formulario, "Overdraft limit (R$)", limite);
        tipo.addActionListener(evento -> limite.setEnabled(tipo.getSelectedIndex() == 0));
        Componentes.adicionar(formulario, Componentes.explicacao(
                "Leave the limit at zero to spend only the balance. Savings accounts have no overdraft."), 22);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        botoes.setOpaque(false);
        JButton criar = Componentes.principal("Create account", () -> {
            try {
                boolean corrente = tipo.getSelectedIndex() == 0;
                confirmar.accept(new Pedido(new Cliente(nome.getText(), cpf.getText()), corrente,
                        corrente ? Moeda.converter(limite.getText()) : BigDecimal.ZERO));
            } catch (IllegalArgumentException invalido) {
                erro.accept(invalido.getMessage());
            }
        });
        criar.setName("criar");
        botoes.add(criar);
        botoes.add(Box.createHorizontalStrut(12));
        botoes.add(Componentes.botao("Cancel", voltar));
        Componentes.adicionar(formulario, botoes, 0);
        add(formulario, BorderLayout.NORTH);
    }
}
