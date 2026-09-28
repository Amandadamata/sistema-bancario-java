package com.github.amandadamata.sistemabancario;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final Banco banco = new Banco();

    public static void main(String[] args) {
        System.out.println("=== SISTEMA BANCÁRIO ===\n");

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
                    case 0 -> System.out.println("Encerrando o sistema. Até logo!");
                    default -> System.out.println("Opção inválida. Tente novamente.\n");
                }
            } while (opcao != 0);
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada encerrada. Encerrando o sistema com segurança.");
        } finally {
            scanner.close();
        }
    }

    private static void exibirMenu() {
        System.out.println("------------ MENU ------------");
        System.out.println("1 - Criar conta");
        System.out.println("2 - Depositar");
        System.out.println("3 - Sacar");
        System.out.println("4 - Transferir");
        System.out.println("5 - Ver extrato");
        System.out.println("6 - Listar contas");
        System.out.println("7 - Aplicar rendimento (poupança)");
        System.out.println("0 - Sair");
        System.out.println("-------------------------------");
    }

    private static void criarConta() {
        String nome = lerTextoObrigatorio("Nome do titular: ");
        String cpf = lerTextoObrigatorio("CPF do titular: ");
        Cliente cliente = new Cliente(nome, cpf);

        System.out.println("Tipo de conta: 1 - Corrente | 2 - Poupança");
        int tipo;
        do {
            tipo = lerInteiro("Escolha: ");
            if (tipo != 1 && tipo != 2) {
                System.out.println("Tipo inválido. Digite 1 para Corrente ou 2 para Poupança.");
            }
        } while (tipo != 1 && tipo != 2);

        Conta conta;
        if (tipo == 1) {
            double limite;
            do {
                limite = lerDouble("Limite do cheque especial: R$ ");
                if (limite < 0) {
                    System.out.println("O limite do cheque especial não pode ser negativo.");
                }
            } while (limite < 0);
            conta = new ContaCorrente(cliente, limite);
        } else {
            conta = new ContaPoupanca(cliente);
        }

        banco.adicionarConta(conta);
        System.out.println("Conta criada com sucesso! Número: " + conta.getNumero()
                + " (" + conta.getTipoConta() + ")\n");
    }

    private static void depositar() {
        Conta conta = buscarContaPorNumeroInformado();
        if (conta == null) {
            return;
        }

        double valor = lerDouble("Valor do depósito: R$ ");
        try {
            conta.depositar(valor);
            System.out.println("Depósito realizado com sucesso!\n");
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage() + "\n");
        }
    }

    private static void sacar() {
        Conta conta = buscarContaPorNumeroInformado();
        if (conta == null) {
            return;
        }

        double valor = lerDouble("Valor do saque: R$ ");
        try {
            conta.sacar(valor);
            System.out.println("Saque realizado com sucesso!\n");
        } catch (SaldoInsuficienteException | IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage() + "\n");
        }
    }

    private static void transferir() {
        System.out.println("Conta de ORIGEM:");
        Conta origem = buscarContaPorNumeroInformado();
        if (origem == null) {
            return;
        }

        System.out.println("Conta de DESTINO:");
        Conta destino = buscarContaPorNumeroInformado();
        if (destino == null) {
            return;
        }

        if (origem == destino) {
            System.out.println("Não é possível transferir para a mesma conta.\n");
            return;
        }

        double valor = lerDouble("Valor da transferência: R$ ");
        try {
            origem.transferir(destino, valor);
            System.out.println("Transferência realizada com sucesso!\n");
        } catch (SaldoInsuficienteException | IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage() + "\n");
        }
    }

    private static void exibirExtrato() {
        Conta conta = buscarContaPorNumeroInformado();
        if (conta != null) {
            conta.exibirExtrato();
        }
    }

    private static void listarContas() {
        System.out.println("\n===== CONTAS CADASTRADAS =====");
        List<Conta> contas = banco.listarContas();
        if (contas.isEmpty()) {
            System.out.println("Nenhuma conta cadastrada.");
        }
        for (Conta conta : contas) {
            System.out.printf(Locale.US, "Nº %d | %-14s | Titular: %-20s | Saldo: R$ %.2f%n",
                    conta.getNumero(), conta.getTipoConta(), conta.getTitular().getNome(),
                    conta.getSaldo());
        }
        System.out.println();
    }

    private static void aplicarRendimentoPoupanca() {
        Conta conta = buscarContaPorNumeroInformado();
        if (conta == null) {
            return;
        }

        if (conta instanceof ContaPoupanca poupanca) {
            try {
                double rendimento = poupanca.aplicarRendimento();
                if (rendimento > 0) {
                    System.out.printf(Locale.US,
                            "Rendimento de R$ %.2f aplicado à conta %d (Poupança).%n%n",
                            rendimento, poupanca.getNumero());
                } else {
                    System.out.println("A conta não possui saldo positivo para rendimento.\n");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Erro: " + e.getMessage() + "\n");
            }
        } else {
            System.out.println("Essa operação só está disponível para contas poupança.\n");
        }
    }

    private static Conta buscarContaPorNumeroInformado() {
        int numero = lerInteiro("Número da conta: ");
        Conta conta = banco.buscarContaPorNumero(numero);
        if (conta == null) {
            System.out.println("Conta não encontrada.\n");
        }
        return conta;
    }

    private static int lerInteiro(String mensagem) {
        System.out.print(mensagem);
        while (true) {
            String entrada = scanner.nextLine().trim();
            try {
                return Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                System.out.print("Entrada inválida. Digite um número inteiro: ");
            }
        }
    }

    private static double lerDouble(String mensagem) {
        System.out.print(mensagem);
        while (true) {
            String entrada = scanner.nextLine().trim();
            try {
                double valor = Double.parseDouble(entrada);
                if (Double.isFinite(valor)) {
                    return valor;
                }
            } catch (NumberFormatException e) {
                System.out.print("Entrada inválida. Digite um número (ex: 100.50): ");
                continue;
            }

            System.out.print("Entrada inválida. Digite um número finito (ex: 100.50): ");
        }
    }

    private static String lerTextoObrigatorio(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String valor = scanner.nextLine().trim();
            if (!valor.isEmpty()) {
                return valor;
            }
            System.out.println("Este campo é obrigatório. Digite um valor.");
        }
    }
}
