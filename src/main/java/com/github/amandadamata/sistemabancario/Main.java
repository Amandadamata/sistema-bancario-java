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
            saida.println("Data in: " + arquivo.toAbsolutePath().normalize());
        } catch (InvalidPathException e) {
            saida.println("The variable " + VARIAVEL_ARQUIVO + " contains an invalid path.");
            return;
        } catch (FalhaPersistenciaException e) {
            saida.println("Could not open the saved data. The system will not start.");
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
        saida.println("=== BANKING SYSTEM ===\n");

        try {
            int opcao;
            do {
                exibirMenu();
                opcao = lerInteiro("Choose an option: ");

                switch (opcao) {
                    case 1 -> criarConta();
                    case 2 -> depositar();
                    case 3 -> sacar();
                    case 4 -> transferir();
                    case 5 -> exibirExtrato();
                    case 6 -> listarContas();
                    case 7 -> aplicarRendimentoPoupanca();
                    case 0 -> saida.println("Shutting down. Goodbye!");
                    default -> saida.println("Invalid option. Try again.\n");
                }
            } while (opcao != 0);
        } catch (NoSuchElementException e) {
            saida.println("\nInput ended. Shutting down safely.");
        }
    }

    private void exibirMenu() {
        saida.println("------------ MENU ------------");
        saida.println("1 - Create account");
        saida.println("2 - Deposit");
        saida.println("3 - Withdraw");
        saida.println("4 - Transfer");
        saida.println("5 - View statement");
        saida.println("6 - List accounts");
        saida.println("7 - Apply monthly interest (savings)");
        saida.println("0 - Exit");
        saida.println("-------------------------------");
    }

    private void criarConta() {
        String nome = lerTextoObrigatorio("Account holder name: ");
        Cpf cpf = lerCpf();
        Cliente cliente = new Cliente(nome, cpf);

        saida.println("Account type: 1 - Checking | 2 - Savings");
        int tipo;
        do {
            tipo = lerInteiro("Choice: ");
            if (tipo != 1 && tipo != 2) {
                saida.println("Invalid type. Enter 1 for Checking or 2 for Savings.");
            }
        } while (tipo != 1 && tipo != 2);

        try {
            Conta conta;
            if (tipo == 1) {
                BigDecimal limite = lerValor("Overdraft limit: R$ ");
                conta = banco.abrirContaCorrente(cliente, limite);
            } else {
                conta = banco.abrirContaPoupanca(cliente);
            }
            saida.println("Account created! Number: " + conta.getNumero()
                    + " (" + conta.getTipoConta() + ")\n");
        } catch (IllegalArgumentException | FalhaPersistenciaException e) {
            saida.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void depositar() {
        Optional<Conta> conta = buscarContaPorNumeroInformado();
        if (conta.isEmpty()) {
            return;
        }

        BigDecimal valor = lerValor("Deposit amount: R$ ");
        try {
            banco.depositar(conta.get().getNumero(), valor);
            saida.println("Deposit completed!\n");
        } catch (IllegalArgumentException | FalhaPersistenciaException e) {
            saida.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void sacar() {
        Optional<Conta> conta = buscarContaPorNumeroInformado();
        if (conta.isEmpty()) {
            return;
        }

        BigDecimal valor = lerValor("Withdrawal amount: R$ ");
        try {
            banco.sacar(conta.get().getNumero(), valor);
            saida.println("Withdrawal completed!\n");
        } catch (SaldoInsuficienteException | IllegalArgumentException | FalhaPersistenciaException e) {
            saida.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void transferir() {
        saida.println("FROM account:");
        Optional<Conta> origem = buscarContaPorNumeroInformado();
        if (origem.isEmpty()) {
            return;
        }

        saida.println("TO account:");
        Optional<Conta> destino = buscarContaPorNumeroInformado();
        if (destino.isEmpty()) {
            return;
        }

        BigDecimal valor = lerValor("Transfer amount: R$ ");
        try {
            banco.transferir(origem.get().getNumero(), destino.get().getNumero(), valor);
            saida.println("Transfer completed!\n");
        } catch (SaldoInsuficienteException | IllegalArgumentException | FalhaPersistenciaException e) {
            saida.println("Error: " + e.getMessage() + "\n");
        }
    }

    private void exibirExtrato() {
        Optional<Conta> encontrada = buscarContaPorNumeroInformado();
        if (encontrada.isEmpty()) {
            return;
        }

        Conta conta = encontrada.get();
        saida.println("\n===== STATEMENT — Account " + conta.getNumero() + " (" + conta.getTipoConta()
                + ") — Holder: " + conta.getTitular().getNome() + " =====");
        List<Transacao> historico = conta.getHistorico();
        if (historico.isEmpty()) {
            saida.println("No transactions yet.");
        }
        for (Transacao transacao : historico) {
            saida.println(formatarTransacao(transacao));
        }
        saida.println("Current balance: " + Moeda.formatar(conta.getSaldo()));
        if (conta instanceof ContaCorrente corrente) {
            saida.println("Overdraft limit: "
                    + Moeda.formatar(corrente.getLimiteChequeEspecial())
                    + " | Available: " + Moeda.formatar(conta.getSaldoDisponivel()));
        }
        saida.println("=================================================\n");
    }

    private String formatarTransacao(Transacao transacao) {
        String descricao = transacao.tipo().getDescricao();
        if (transacao.contaRelacionada().isPresent()) {
            String preposicao = transacao.tipo().isCredito() ? "from" : "to";
            descricao += " (" + preposicao + " account " + transacao.contaRelacionada().getAsInt() + ")";
        }
        String sinal = transacao.tipo().isCredito() ? "+" : "-";
        return String.format("[%s] %-42s %s%s | Balance after: %s",
                transacao.dataHora().format(FORMATO_DATA),
                descricao,
                sinal,
                Moeda.formatar(transacao.valor()),
                Moeda.formatar(transacao.saldoResultante()));
    }

    private void listarContas() {
        saida.println("\n===== ACCOUNTS =====");
        List<Conta> contas = banco.listarContas();
        if (contas.isEmpty()) {
            saida.println("No accounts yet.");
        }
        for (Conta conta : contas) {
            saida.printf("No. %d | %-16s | Holder: %-20s | Balance: %s%n",
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
                saida.println("Interest of " + Moeda.formatar(rendimento)
                        + " applied to account " + conta.get().getNumero() + ".\n");
            } else {
                saida.println("The current balance doesn't earn at least R$ 0,01 in interest.\n");
            }
        } catch (IllegalArgumentException | IllegalStateException | FalhaPersistenciaException e) {
            saida.println("Error: " + e.getMessage() + "\n");
        }
    }

    private Optional<Conta> buscarContaPorNumeroInformado() {
        int numero = lerInteiro("Account number: ");
        Optional<Conta> conta = banco.buscarContaPorNumero(numero);
        if (conta.isEmpty()) {
            saida.println("Account not found.\n");
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
                saida.print("Invalid input. Enter a whole number: ");
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
                saida.print(e.getMessage() + " Try again: ");
            }
        }
    }

    /** Repete a pergunta até receber um CPF válido. A mensagem de erro não repete o número. */
    private Cpf lerCpf() {
        while (true) {
            saida.print("Holder's CPF: ");
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
            saida.println("This field is required. Enter a value.");
        }
    }
}
