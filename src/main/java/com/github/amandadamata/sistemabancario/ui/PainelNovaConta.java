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
        Componentes.adicionar(formulario, Componentes.texto("Nova conta", 30, true), 12);
        Componentes.adicionar(formulario, Componentes.explicacao(
                "Cadastre o titular para começar. As contas são locais e não movimentam dinheiro real."), 24);
        JTextField nome = new JTextField();
        nome.setName("nome");
        JTextField cpf = new JTextField();
        cpf.setName("cpf");
        JComboBox<String> tipo = new JComboBox<>(new String[]{"Conta Corrente", "Conta Poupança"});
        tipo.setName("tipo");
        JTextField limite = new JTextField("0,00");
        limite.setName("limite");
        Componentes.campo(formulario, "Nome do titular", nome);
        Componentes.campo(formulario, "CPF", cpf);
        Componentes.campo(formulario, "Tipo de conta", tipo);
        Componentes.campo(formulario, "Limite do cheque especial (R$)", limite);
        tipo.addActionListener(evento -> limite.setEnabled(tipo.getSelectedIndex() == 0));
        Componentes.adicionar(formulario, Componentes.explicacao(
                "Deixe o limite em zero para gastar apenas o saldo. A poupança não usa cheque especial."), 22);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        botoes.setOpaque(false);
        JButton criar = Componentes.principal("Criar conta", () -> {
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
        botoes.add(Componentes.botao("Cancelar", voltar));
        Componentes.adicionar(formulario, botoes, 0);
        add(formulario, BorderLayout.NORTH);
    }
}
