package com.github.amandadamata.sistemabancario.ui;

import com.github.amandadamata.sistemabancario.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

/** Coordena navegação e eventos; todas as regras financeiras continuam em Banco e Conta. */
public final class JanelaBanco extends JFrame {
    private final Banco banco;
    private final JPanel conteudo = new PainelRolavel();
    private final JPanel cabecalho = new JPanel(new BorderLayout(16, 0));
    private final JTextArea mensagem = Componentes.explicacao("");
    private final JButton inicio = Componentes.botao("Overview", this::mostrarResumo);
    private final JButton extrato = Componentes.botao("Transactions", this::mostrarHistorico);
    private final JButton novaConta = Componentes.botao("New account", this::mostrarNovaConta);
    private final TarefasUi tarefas;
    private Conta conta;

    public JanelaBanco(Banco banco, Path arquivo, Runnable aoFechar) {
        super("Linha · Banking System");
        this.banco = banco;
        this.conta = banco.listarContas().stream().findFirst().orElse(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(860, 700));
        setSize(1140, 820);
        setLocationRelativeTo(null);
        JPanel raiz = new JPanel(new BorderLayout());
        setContentPane(raiz);
        tarefas = new TarefasUi(raiz, this::mostrarErro);
        raiz.add(menu(), BorderLayout.WEST);
        JPanel principal = new JPanel(new BorderLayout(0, 22));
        principal.setBorder(BorderFactory.createEmptyBorder(26, 30, 16, 30));
        principal.add(cabecalho, BorderLayout.NORTH);
        JScrollPane rolagem = new JScrollPane(conteudo,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        rolagem.getVerticalScrollBar().setUnitIncrement(18);
        principal.add(rolagem, BorderLayout.CENTER);
        JPanel rodape = new JPanel(new BorderLayout(0, 6));
        mensagem.setRows(2);
        mensagem.setName("mensagem");
        mensagem.getAccessibleContext().setAccessibleName("Operation result");
        rodape.add(mensagem, BorderLayout.CENTER);
        JLabel local = Componentes.texto("Study project · Data saved on this computer", 12, false);
        local.setForeground(Tema.SECUNDARIO);
        local.setToolTipText(arquivo.toAbsolutePath().toString());
        rodape.add(local, BorderLayout.SOUTH);
        principal.add(rodape, BorderLayout.SOUTH);
        raiz.add(principal);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evento) {
                if (tarefas.ocupada()) {
                    mensagem.setText("Wait for the operation to finish before closing.");
                    return;
                }
                dispose();
                aoFechar.run();
            }
        });
        atualizarCabecalho();
        mostrarResumo();
    }

    private JPanel menu() {
        JPanel menu = new JPanel(new BorderLayout());
        menu.setPreferredSize(new Dimension(190, 0));
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.BORDA),
                BorderFactory.createEmptyBorder(34, 20, 26, 20)));
        JPanel itens = Componentes.coluna();
        JLabel marca = Componentes.texto("linha", 34, true);
        marca.setForeground(Tema.DESTAQUE);
        Componentes.adicionar(itens, marca, 4);
        JLabel subtitulo = Componentes.texto("BANKING SYSTEM", 10, true);
        subtitulo.setForeground(Tema.SECUNDARIO);
        Componentes.adicionar(itens, subtitulo, 52);
        for (JButton botao : new JButton[]{inicio, extrato, novaConta}) {
            botao.setHorizontalAlignment(SwingConstants.LEFT);
            botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            Componentes.adicionar(itens, botao, 10);
        }
        menu.add(itens, BorderLayout.NORTH);
        menu.add(Componentes.texto("Simple. Local. Yours.", 12, false), BorderLayout.SOUTH);
        return menu;
    }

    private void atualizarCabecalho() {
        cabecalho.removeAll();
        JLabel saudacao = Componentes.texto(conta == null ? "Welcome to Linha."
                : "Hi, " + conta.getTitular().getNome().split("\\s+")[0] + ".", 18, true);
        cabecalho.add(saudacao, BorderLayout.CENTER);
        JComboBox<Conta> seletor = Componentes.contas(banco.listarContas());
        seletor.setName("contas");
        seletor.getAccessibleContext().setAccessibleName("Selected account");
        seletor.setPreferredSize(new Dimension(340, 44));
        seletor.setSelectedItem(conta);
        seletor.setVisible(conta != null);
        seletor.addActionListener(evento -> {
            conta = (Conta) seletor.getSelectedItem();
            atualizarCabecalho();
            mostrarResumo();
        });
        cabecalho.add(seletor, BorderLayout.EAST);
        extrato.setEnabled(conta != null);
        cabecalho.revalidate();
        cabecalho.repaint();
    }

    private void exibir(JComponent painel, String pagina) {
        conteudo.removeAll();
        conteudo.setName(pagina);
        conteudo.add(painel, BorderLayout.NORTH);
        mensagem.setText("");
        inicio.setBackground(pagina.equals("resumo") ? Color.decode("#EAF0F2") : Color.WHITE);
        extrato.setBackground(pagina.equals("extrato") ? Color.decode("#EAF0F2") : Color.WHITE);
        novaConta.setBackground(pagina.equals("nova-conta") ? Color.decode("#EAF0F2") : Color.WHITE);
        conteudo.revalidate();
        conteudo.repaint();
        SwingUtilities.invokeLater(() -> {
            conteudo.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
            Component foco = encontrarCampo(painel);
            if (foco != null) foco.requestFocusInWindow();
        });
    }

    private static Component encontrarCampo(Container painel) {
        for (Component c : painel.getComponents()) {
            if (c instanceof JTextField) return c;
            if (c instanceof Container filho) {
                Component encontrado = encontrarCampo(filho);
                if (encontrado != null) return encontrado;
            }
        }
        return null;
    }

    private void mostrarResumo() {
        if (conta == null) {
            JPanel vazio = Componentes.coluna();
            Componentes.adicionar(vazio, Componentes.texto("A place to start.", 30, true), 18);
            Componentes.adicionar(vazio, Componentes.explicacao(
                    "Open your first account to track your balance, make transactions and see your history."), 28);
            Componentes.adicionar(vazio, Componentes.principal("Open first account", this::mostrarNovaConta), 0);
            exibir(vazio, "vazio");
        } else {
            exibir(new PainelResumo(conta, this::mostrarOperacao, this::mostrarHistorico,
                    this::mostrarRendimento), "resumo");
        }
    }

    private void mostrarHistorico() {
        if (conta != null) exibir(new PainelHistorico(conta, true), "extrato");
    }

    private void mostrarOperacao(PainelOperacao.Tipo tipo) {
        exibir(new PainelOperacao(tipo, conta, banco.listarContas(), pedido -> {
            mensagem.setForeground(Tema.SECUNDARIO);
            mensagem.setText("Saving…");
            tarefas.executar(() -> {
                switch (pedido.tipo()) {
                    case DEPOSITO -> banco.depositar(pedido.origem(), pedido.valor());
                    case SAQUE -> banco.sacar(pedido.origem(), pedido.valor());
                    case TRANSFERENCIA -> {
                        if (pedido.destino() == null) throw new IllegalArgumentException("Select the destination account.");
                        banco.transferir(pedido.origem(), pedido.destino(), pedido.valor());
                    }
                }
                String valor = Moeda.formatar(pedido.valor());
                return switch (pedido.tipo()) {
                    case DEPOSITO -> "Deposit of " + valor + " completed.";
                    case SAQUE -> "Withdrawal of " + valor + " completed.";
                    case TRANSFERENCIA -> "Transfer of " + valor + " completed.";
                };
            }, this::concluir);
        }, this::mostrarErro, this::mostrarResumo), "operacao");
    }

    private void mostrarNovaConta() {
        exibir(new PainelNovaConta(pedido -> {
            mensagem.setForeground(Tema.SECUNDARIO);
            mensagem.setText("Saving account…");
            tarefas.executar(() -> pedido.corrente()
                    ? banco.abrirContaCorrente(pedido.titular(), pedido.limite())
                    : banco.abrirContaPoupanca(pedido.titular()), nova -> {
                conta = nova;
                atualizarCabecalho();
                concluir("Account " + nova.getNumero() + " created.");
            });
        }, this::mostrarErro, this::mostrarResumo), "nova-conta");
    }

    private void mostrarRendimento() {
        JPanel painel = Componentes.coluna();
        Componentes.adicionar(painel, Componentes.texto("Monthly interest", 30, true), 18);
        Componentes.adicionar(painel, Componentes.explicacao(
                "Applies 0.5% to the current savings balance, once a month.\n"
                + "This is a simplified rule for this project, not a real savings formula."), 24);
        int numero = conta.getNumero();
        Componentes.adicionar(painel, Componentes.principal("Confirm interest", () ->
                tarefas.executar(() -> banco.aplicarRendimento(numero), valor -> concluir(valor.signum() > 0
                        ? "Interest of " + Moeda.formatar(valor) + " applied."
                        : "The balance doesn't earn R$ 0,01 in interest yet."))), 12);
        Componentes.adicionar(painel, Componentes.botao("Cancel", this::mostrarResumo), 0);
        exibir(painel, "rendimento");
    }

    private void concluir(String texto) {
        mostrarResumo();
        mensagem.setForeground(Tema.SUCESSO);
        mensagem.setText(texto);
    }

    private void mostrarErro(String texto) {
        mensagem.setForeground(Tema.ERRO);
        mensagem.setText(texto);
    }
}
