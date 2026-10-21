package com.github.amandadamata.sistemabancario;

import com.github.amandadamata.sistemabancario.persistencia.RepositorioSqlite;

import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

/**
 * Interface de linha de comando.
 *
 * <p>Toda regra de negócio fica nas classes de domínio; esta classe apenas lê entradas, chama
 * as operações do {@link Banco} e exibe os resultados. A entrada e a saída são recebidas no
 * construtor para que o fluxo do menu possa ser testado sem digitar no terminal.
 */
public class Main {

    static final String VARIAVEL_ARQUIVO = "SISTEMA_BANCARIO_DB";
    static final Path ARQUIVO_PADRAO = Path.of("dados", "sistema-bancario.db");

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Banco banco;
    private final Scanner scanner;
    private final PrintStream saida;

    Main(Banco banco, InputStream entrada, PrintStream saida) {
        this.banco = banco;
        this.scanner = new Scanner(entrada, StandardCharsets.UTF_8);
        this.saida = saida;
    }

    public static void main(String[] args) {
        PrintStream saida = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        Banco banco;
        try {
            Path arquivo = resolverArquivo(System.getenv(VARIAVEL_ARQUIVO));
            banco = Banco.carregar(Clock.systemDefaultZone(), new RepositorioSqlite(arquivo));
            saida.println("Dados em: " + arquivo.toAbsolutePath().normalize());
        } catch (InvalidPathException e) {
            saida.println("A variável " + VARIAVEL_ARQUIVO + " contém um caminho inválido.");
            return;
        } catch (FalhaPersistenciaException e) {
            saida.println("Não foi possível abrir os dados salvos. O sistema não será iniciado.");
            saida.println(e.getMessage());
            return;
        }
        new Main(banco, System.in, saida).executar();
    }

    static Path resolverArquivo(String caminhoConfigurado) {
        if (caminhoConfigurado == null || caminhoConfigurado.isBlank()) {
            return ARQUIVO_PADRAO;
        }
        return Path.of(caminhoConfigurado.trim());
    }

    void executar() {
        saida.println("=== SISTEMA BANCÁRIO ===\n");

        try {
            int opcao;
            do {
                exibirMenu();
                opcao = lerInteiro("Escolha uma opção: ");

                switch (opcao) {
                    case 1 -> criarConta();
                    case 2 -> depositar();
                    case 3 -> sacar();
                    case 4 -> transferir();
                    case 5 -> exibirExtrato();
                    case 6 -> listarContas();
                    case 7 -> aplicarRendimentoPoupanca();
                    case 0 -> saida.println("Encerrando o sistema. Até logo!");
                    default -> saida.println("Opção inválida. Tente novamente.\n");
                }
            } while (opcao != 0);
        } catch (NoSuchElementException e) {
            saida.println("\nEntrada encerrada. Encerrando o sistema com segurança.");
        }
    }

    private void exibirMenu() {
        saida.println("------------ MENU ------------");
        saida.println("1 - Criar conta");
        saida.println("2 - Depositar");
        saida.println("3 - Sacar");
        saida.println("4 - Transferir");
        saida.println("5 - Ver extrato");
        saida.println("6 - Listar contas");
        saida.println("7 - Aplicar rendimento mensal (poupança)");
        saida.println("0 - Sair");
        saida.println("-------------------------------");
    }

    private void criarConta() {
        String nome = lerTextoObrigatorio("Nome do titular: ");
        Cpf cpf = lerCpf();
        Cliente cliente = new Cliente(nome, cpf);

        saida.println("Tipo de conta: 1 - Corrente | 2 - Poupança");
        int tipo;
        do {
            tipo = lerInteiro("Escolha: ");
            if (tipo != 1 && tipo != 2) {
                saida.println("Tipo inválido. Digite 1 para Corrente ou 2 para Poupança.");
            }
        } while (tipo != 1 && tipo != 2);

        try {
            Conta conta;
            if (tipo == 1) {
                BigDecimal limite = lerValor("Limite do cheque especial: R$ ");
                conta = banco.abrirContaCorrente(cliente, limite);
            } else {
                conta = banco.abrirContaPoupanca(cliente);
            }
            saida.println("Conta criada com sucesso! Número: " + conta.getNumero()
                    + " (" + conta.getTipoConta() + ")\n");
        } catch (IllegalArgumentException | FalhaPersistenciaException e) {
            saida.println("Erro: " + e.getMessage() + "\n");
        }
    }

    private void depositar() {
        Optional<Conta> conta = buscarContaPorNumeroInformado();
        if (conta.isEmpty()) {
            return;
        }

        BigDecimal valor = lerValor("Valor do depósito: R$ ");
        try {
            banco.depositar(conta.get().getNumero(), valor);
            saida.println("Depósito realizado com sucesso!\n");
        } catch (IllegalArgumentException | FalhaPersistenciaException e) {
            saida.println("Erro: " + e.getMessage() + "\n");
        }
    }

    private void sacar() {
        Optional<Conta> conta = buscarContaPorNumeroInformado();
        if (conta.isEmpty()) {
            return;
        }

        BigDecimal valor = lerValor("Valor do saque: R$ ");
        try {
            banco.sacar(conta.get().getNumero(), valor);
            saida.println("Saque realizado com sucesso!\n");
        } catch (SaldoInsuficienteException | IllegalArgumentException | FalhaPersistenciaException e) {
            saida.println("Erro: " + e.getMessage() + "\n");
        }
    }

    private void transferir() {
        saida.println("Conta de ORIGEM:");
        Optional<Conta> origem = buscarContaPorNumeroInformado();
        if (origem.isEmpty()) {
            return;
        }

        saida.println("Conta de DESTINO:");
        Optional<Conta> destino = buscarContaPorNumeroInformado();
        if (destino.isEmpty()) {
            return;
        }

        BigDecimal valor = lerValor("Valor da transferência: R$ ");
        try {
            banco.transferir(origem.get().getNumero(), destino.get().getNumero(), valor);
            saida.println("Transferência realizada com sucesso!\n");
        } catch (SaldoInsuficienteException | IllegalArgumentException | FalhaPersistenciaException e) {
            saida.println("Erro: " + e.getMessage() + "\n");
        }
    }

    private void exibirExtrato() {
        Optional<Conta> encontrada = buscarContaPorNumeroInformado();
        if (encontrada.isEmpty()) {
            return;
        }

        Conta conta = encontrada.get();
        saida.println("\n===== EXTRATO — Conta " + conta.getNumero() + " (" + conta.getTipoConta()
                + ") — Titular: " + conta.getTitular().getNome() + " =====");
        List<Transacao> historico = conta.getHistorico();
        if (historico.isEmpty()) {
            saida.println("Nenhuma movimentação registrada ainda.");
        }
        for (Transacao transacao : historico) {
            saida.println(formatarTransacao(transacao));
        }
        saida.println("Saldo atual: " + Moeda.formatar(conta.getSaldo()));
        if (conta instanceof ContaCorrente corrente) {
            saida.println("Limite do cheque especial: "
                    + Moeda.formatar(corrente.getLimiteChequeEspecial())
                    + " | Disponível: " + Moeda.formatar(conta.getSaldoDisponivel()));
        }
        saida.println("=================================================\n");
    }

    private String formatarTransacao(Transacao transacao) {
        String descricao = transacao.tipo().getDescricao();
        if (transacao.contaRelacionada().isPresent()) {
            String preposicao = transacao.tipo().isCredito() ? "da" : "para a";
            descricao += " (" + preposicao + " conta " + transacao.contaRelacionada().getAsInt() + ")";
        }
        String sinal = transacao.tipo().isCredito() ? "+" : "-";
        return String.format("[%s] %-42s %s%s | Saldo após: %s",
                transacao.dataHora().format(FORMATO_DATA),
                descricao,
                sinal,
                Moeda.formatar(transacao.valor()),
                Moeda.formatar(transacao.saldoResultante()));
    }

    private void listarContas() {
        saida.println("\n===== CONTAS CADASTRADAS =====");
        List<Conta> contas = banco.listarContas();
        if (contas.isEmpty()) {
            saida.println("Nenhuma conta cadastrada.");
        }
        for (Conta conta : contas) {
            saida.printf("Nº %d | %-14s | Titular: %-20s | Saldo: %s%n",
                    conta.getNumero(), conta.getTipoConta(), conta.getTitular().getNome(),
                    Moeda.formatar(conta.getSaldo()));
        }
        saida.println();
    }

    private void aplicarRendimentoPoupanca() {
        Optional<Conta> conta = buscarContaPorNumeroInformado();
        if (conta.isEmpty()) {
            return;
        }

        try {
            BigDecimal rendimento = banco.aplicarRendimento(conta.get().getNumero());
            if (rendimento.signum() > 0) {
                saida.println("Rendimento de " + Moeda.formatar(rendimento)
                        + " aplicado à conta " + conta.get().getNumero() + ".\n");
            } else {
                saida.println("O saldo atual não gera rendimento de pelo menos R$ 0,01.\n");
            }
        } catch (IllegalArgumentException | IllegalStateException | FalhaPersistenciaException e) {
            saida.println("Erro: " + e.getMessage() + "\n");
        }
    }

    private Optional<Conta> buscarContaPorNumeroInformado() {
        int numero = lerInteiro("Número da conta: ");
        Optional<Conta> conta = banco.buscarContaPorNumero(numero);
        if (conta.isEmpty()) {
            saida.println("Conta não encontrada.\n");
        }
        return conta;
    }

    private int lerInteiro(String mensagem) {
        saida.print(mensagem);
        while (true) {
            String entrada = scanner.nextLine().trim();
            try {
                return Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                saida.print("Entrada inválida. Digite um número inteiro: ");
            }
        }
    }

    /** Lê um valor monetário. A validação de regra de negócio (ex: positivo) fica no domínio. */
    private BigDecimal lerValor(String mensagem) {
        saida.print(mensagem);
        while (true) {
            try {
                return Moeda.converter(scanner.nextLine());
            } catch (IllegalArgumentException e) {
                saida.print(e.getMessage() + " Tente novamente: ");
            }
        }
    }

    /** Repete a pergunta até receber um CPF válido. A mensagem de erro não repete o número. */
    private Cpf lerCpf() {
        while (true) {
            saida.print("CPF do titular: ");
            try {
                return Cpf.de(scanner.nextLine());
            } catch (IllegalArgumentException e) {
                saida.println(e.getMessage());
            }
        }
    }

    private String lerTextoObrigatorio(String mensagem) {
        while (true) {
            saida.print(mensagem);
            String valor = scanner.nextLine().trim();
            if (!valor.isEmpty()) {
                return valor;
            }
            saida.println("Este campo é obrigatório. Digite um valor.");
        }
    }
}
