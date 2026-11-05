package com.github.amandadamata.sistemabancario.ui;

import com.github.amandadamata.sistemabancario.*;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

final class PainelResumo extends JPanel {
    PainelResumo(Conta conta, Consumer<PainelOperacao.Tipo> operar, Runnable extrato, Runnable rendimento) {
        super(new BorderLayout(0, 18));
        setOpaque(false);
        JPanel topo = Componentes.coluna();
        Componentes.adicionar(topo, Componentes.texto("Sua conta, com clareza.", 27, true), 18);
        JPanel saldo = Componentes.coluna();
        saldo.setOpaque(true);
        saldo.setBackground(Color.WHITE);
        saldo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Tema.BORDA),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)));
        Componentes.adicionar(saldo, Componentes.texto("Saldo em conta", 14, false), 8);
        JLabel valor = Componentes.texto(Moeda.formatar(conta.getSaldo()), 38, true);
        valor.setName("saldo");
        Componentes.adicionar(saldo, valor, 12);
        String detalhe = "Disponível para usar: " + Moeda.formatar(conta.getSaldoDisponivel());
        if (conta instanceof ContaCorrente corrente) {
            detalhe += "  •  Inclui limite de " + Moeda.formatar(corrente.getLimiteChequeEspecial());
        }
        Componentes.adicionar(saldo, Componentes.explicacao(detalhe), 0);
        Componentes.adicionar(topo, saldo, 18);
        JPanel acoes = new JPanel(new GridLayout(1, 3, 12, 0));
        acoes.setOpaque(false);
        int[] direcoes = {1, -1, 0};
        int indice = 0;
        for (PainelOperacao.Tipo tipo : PainelOperacao.Tipo.values()) {
            JButton botao = Componentes.botao(tipo.rotulo, () -> operar.accept(tipo));
            botao.setIcon(new IconeLinha(direcoes[indice++]));
            botao.setIconTextGap(12);
            acoes.add(botao);
        }
        Componentes.adicionar(topo, acoes, 0);
        add(topo, BorderLayout.NORTH);
        add(new PainelHistorico(conta, false), BorderLayout.CENTER);
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rodape.setOpaque(false);
        rodape.add(Componentes.botao("Ver todas as movimentações", extrato));
        if (conta instanceof ContaPoupanca) {
            rodape.add(Box.createHorizontalStrut(12));
            rodape.add(Componentes.botao("Rendimento mensal", rendimento));
        }
        add(rodape, BorderLayout.SOUTH);
    }
}
