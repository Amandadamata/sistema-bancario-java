package com.github.amandadamata.sistemabancario;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Porta de entrada das operações bancárias.
 *
 * <p>O banco abre contas, controla a numeração e executa depósitos, saques, transferências e
 * rendimentos. Cada operação segue a mesma ordem:
 * <ol>
 *   <li>guarda o estado das contas envolvidas;</li>
 *   <li>aplica a regra de negócio na memória;</li>
 *   <li>grava todas as contas envolvidas em uma única operação do repositório;</li>
 *   <li>se a gravação falhar, devolve as contas ao estado guardado.</li>
 * </ol>
 * Assim, memória e armazenamento nunca divergem, e uma transferência nunca fica gravada pela
 * metade.
 */
public final class Banco {

    static final int PRIMEIRO_NUMERO_CONTA = 1000;

    private final Map<Integer, Conta> contas = new LinkedHashMap<>();
    private final Clock relogio;
    private final RepositorioBanco repositorio;
    private int proximoNumero = PRIMEIRO_NUMERO_CONTA;

    /** Banco somente em memória: os dados se perdem ao encerrar o programa. */
    public Banco(Clock relogio) {
        this(relogio, RepositorioBanco.semPersistencia());
    }

    private Banco(Clock relogio, RepositorioBanco repositorio) {
        this.relogio = Objects.requireNonNull(relogio, "O relógio é obrigatório.");
        this.repositorio = Objects.requireNonNull(repositorio, "O repositório é obrigatório.");
    }

    /**
     * Cria um banco com as contas já gravadas no repositório.
     *
     * @throws FalhaPersistenciaException se os dados não puderem ser lidos ou estiverem
     *                                    inconsistentes
     */
    public static Banco carregar(Clock relogio, RepositorioBanco repositorio) {
        Banco banco = new Banco(relogio, repositorio);
        for (ContaPersistida registro : repositorio.carregar()) {
            Conta conta = banco.restaurar(registro);
            if (banco.contas.putIfAbsent(conta.getNumero(), conta) != null) {
                throw new FalhaPersistenciaException("Número de conta repetido: " + conta.getNumero());
            }
            banco.proximoNumero = Math.max(banco.proximoNumero, conta.getNumero() + 1);
        }
        return banco;
    }

    public ContaCorrente abrirContaCorrente(Cliente titular, BigDecimal limiteChequeEspecial) {
        garantirCpfDoMesmoTitular(titular);
        return registrarNovaConta(new ContaCorrente(proximoNumero, titular, limiteChequeEspecial, relogio));
    }

    public ContaPoupanca abrirContaPoupanca(Cliente titular) {
        garantirCpfDoMesmoTitular(titular);
        return registrarNovaConta(new ContaPoupanca(proximoNumero, titular, relogio));
    }

    public void depositar(int numeroConta, BigDecimal valor) {
        Conta conta = contaObrigatoria(numeroConta);
        List<EstadoConta> antes = capturarEstados(List.of(conta));
        conta.depositar(valor);
        gravarOuDesfazer(List.of(conta), antes);
    }

    public void sacar(int numeroConta, BigDecimal valor) throws SaldoInsuficienteException {
        Conta conta = contaObrigatoria(numeroConta);
        List<EstadoConta> antes = capturarEstados(List.of(conta));
        conta.sacar(valor);
        gravarOuDesfazer(List.of(conta), antes);
    }

    /** Grava origem e destino juntos: ou os dois lados ficam gravados, ou nenhum. */
    public void transferir(int numeroOrigem, int numeroDestino, BigDecimal valor)
            throws SaldoInsuficienteException {
        Conta origem = contaObrigatoria(numeroOrigem);
        Conta destino = contaObrigatoria(numeroDestino);
        if (origem == destino) {
            throw new IllegalArgumentException("Não é possível transferir para a mesma conta.");
        }
        List<Conta> envolvidas = List.of(origem, destino);
        List<EstadoConta> antes = capturarEstados(envolvidas);
        origem.transferir(destino, valor);
        gravarOuDesfazer(envolvidas, antes);
    }

    /**
     * @return o valor creditado (zero se o saldo for pequeno demais)
     * @throws IllegalArgumentException se a conta não for poupança
     * @throws IllegalStateException se o rendimento do mês já foi aplicado
     */
    public BigDecimal aplicarRendimento(int numeroConta) {
        if (!(contaObrigatoria(numeroConta) instanceof ContaPoupanca poupanca)) {
            throw new IllegalArgumentException("Essa operação só está disponível para contas poupança.");
        }
        List<EstadoConta> antes = capturarEstados(List.of(poupanca));
        BigDecimal rendimento = poupanca.aplicarRendimento();
        gravarOuDesfazer(List.of(poupanca), antes);
        return rendimento;
    }

    public Optional<Conta> buscarContaPorNumero(int numero) {
        return Optional.ofNullable(contas.get(numero));
    }

    /** Contas em ordem de abertura. A lista retornada não pode ser alterada. */
    public List<Conta> listarContas() {
        return List.copyOf(contas.values());
    }

    private static List<EstadoConta> capturarEstados(List<Conta> envolvidas) {
        List<EstadoConta> estados = new ArrayList<>();
        for (Conta conta : envolvidas) {
            estados.add(conta.capturarEstado());
        }
        return estados;
    }

    /**
     * Grava as contas alteradas. Se a gravação falhar, o repositório não gravou nada, então
     * as contas voltam ao estado anterior e a memória continua igual ao armazenamento.
     *
     * <p>Se a regra de negócio recusar a operação (por exemplo, saldo insuficiente), este
     * método nem é chamado: a recusa acontece antes de qualquer alteração.
     */
    private void gravarOuDesfazer(List<Conta> envolvidas, List<EstadoConta> estadosAnteriores) {
        try {
            repositorio.salvar(envolvidas.stream().map(Conta::paraRegistro).toList());
        } catch (RuntimeException falha) {
            for (int i = 0; i < envolvidas.size(); i++) {
                envolvidas.get(i).restaurarEstado(estadosAnteriores.get(i));
            }
            throw new FalhaPersistenciaException(
                    "Não foi possível gravar a operação; ela foi desfeita e nada foi alterado.", falha);
        }
    }

    /**
     * O número só é consumido depois que a conta foi criada e gravada com sucesso; uma abertura
     * recusada não deixa lacunas na numeração.
     */
    private <T extends Conta> T registrarNovaConta(T conta) {
        try {
            repositorio.salvar(List.of(conta.paraRegistro()));
        } catch (RuntimeException falha) {
            throw new FalhaPersistenciaException(
                    "Não foi possível gravar a nova conta; ela não foi criada.", falha);
        }
        contas.put(conta.getNumero(), conta);
        proximoNumero++;
        return conta;
    }

    /** Um CPF identifica um único titular: não pode aparecer com dois nomes diferentes. */
    private void garantirCpfDoMesmoTitular(Cliente titular) {
        Objects.requireNonNull(titular, "O titular é obrigatório.");
        for (Conta conta : contas.values()) {
            Cliente existente = conta.getTitular();
            if (existente.getCpf().equals(titular.getCpf())
                    && !existente.getNome().equalsIgnoreCase(titular.getNome())) {
                throw new IllegalArgumentException(
                        "Este CPF já está cadastrado para outro titular.");
            }
        }
    }

    private Conta contaObrigatoria(int numero) {
        return buscarContaPorNumero(numero)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + numero));
    }

    /** Reconstrói uma conta gravada e confere se os dados fazem sentido. */
    private Conta restaurar(ContaPersistida registro) {
        try {
            Cliente titular = new Cliente(registro.titularNome(), registro.titularCpf());
            Conta conta = switch (registro.tipo()) {
                case CORRENTE -> new ContaCorrente(
                        registro.numero(), titular, registro.limiteChequeEspecial(), relogio);
                case POUPANCA -> {
                    ContaPoupanca poupanca = new ContaPoupanca(registro.numero(), titular, relogio);
                    poupanca.restaurarUltimoMesComRendimento(registro.ultimoMesComRendimento());
                    yield poupanca;
                }
            };
            conta.restaurarHistorico(registro.historico(), registro.saldo());
            return conta;
        } catch (IllegalArgumentException | NullPointerException invalido) {
            throw new FalhaPersistenciaException("Dados inválidos na conta " + registro.numero()
                    + ": " + invalido.getMessage(), invalido);
        }
    }
}
