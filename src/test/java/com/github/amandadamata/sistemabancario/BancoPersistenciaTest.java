package com.github.amandadamata.sistemabancario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regras do Banco ligadas à gravação, usando um repositório em memória que pode falhar de
 * propósito. Os testes com SQLite real estão em {@code persistencia.RepositorioSqliteTest}.
 */
class BancoPersistenciaTest {

    private static final Cliente ANA = new Cliente("Ana", "246.813.579-28");
    private static final Cliente BRUNO = new Cliente("Bruno", "135.792.468-28");

    private RelogioAjustavel relogio;
    private RepositorioControlado repositorio;
    private Banco banco;

    @BeforeEach
    void criarBanco() {
        relogio = RelogioAjustavel.em("2026-10-20T13:00:00Z");
        repositorio = new RepositorioControlado();
        banco = Banco.carregar(relogio, repositorio);
    }

    @Test
    void cadaOperacaoGravaAsContasAlteradas() throws SaldoInsuficienteException {
        ContaCorrente conta = banco.abrirContaCorrente(ANA, valor("100.00"));

        banco.depositar(conta.getNumero(), valor("50.00"));
        banco.sacar(conta.getNumero(), valor("20.00"));

        ContaPersistida gravada = repositorio.gravadas.get(conta.getNumero());
        assertAll(
                () -> assertEquals(valor("30.00"), gravada.saldo()),
                () -> assertEquals(2, gravada.historico().size()),
                () -> assertEquals("24681357928", gravada.titularCpf()));
    }

    @Test
    void falhaAoGravarDepositoDesfazAAlteracaoNaMemoria() {
        ContaPoupanca conta = banco.abrirContaPoupanca(ANA);
        repositorio.falharNaProximaGravacao = true;

        assertThrows(FalhaPersistenciaException.class,
                () -> banco.depositar(conta.getNumero(), valor("100.00")));

        assertAll(
                () -> assertEquals(valor("0.00"), conta.getSaldo()),
                () -> assertTrue(conta.getHistorico().isEmpty()),
                () -> assertEquals(valor("0.00"), repositorio.gravadas.get(conta.getNumero()).saldo()));
    }

    @Test
    void falhaAoGravarTransferenciaDesfazAsDuasContas() throws SaldoInsuficienteException {
        ContaPoupanca origem = banco.abrirContaPoupanca(ANA);
        ContaPoupanca destino = banco.abrirContaPoupanca(BRUNO);
        banco.depositar(origem.getNumero(), valor("100.00"));
        repositorio.falharNaProximaGravacao = true;

        assertThrows(FalhaPersistenciaException.class,
                () -> banco.transferir(origem.getNumero(), destino.getNumero(), valor("40.00")));

        assertAll(
                () -> assertEquals(valor("100.00"), origem.getSaldo()),
                () -> assertEquals(1, origem.getHistorico().size()),
                () -> assertEquals(valor("0.00"), destino.getSaldo()),
                () -> assertTrue(destino.getHistorico().isEmpty()));
    }

    @Test
    void transferenciaGravaOrigemEDestinoNaMesmaChamada() throws SaldoInsuficienteException {
        ContaPoupanca origem = banco.abrirContaPoupanca(ANA);
        ContaPoupanca destino = banco.abrirContaPoupanca(BRUNO);
        banco.depositar(origem.getNumero(), valor("100.00"));
        repositorio.chamadas.clear();

        banco.transferir(origem.getNumero(), destino.getNumero(), valor("40.00"));

        assertEquals(List.of(List.of(origem.getNumero(), destino.getNumero())), repositorio.chamadas);
    }

    @Test
    void falhaAoGravarRendimentoNaoConsomeOMes() {
        ContaPoupanca conta = banco.abrirContaPoupanca(ANA);
        banco.depositar(conta.getNumero(), valor("1000.00"));
        repositorio.falharNaProximaGravacao = true;

        assertThrows(FalhaPersistenciaException.class, () -> banco.aplicarRendimento(conta.getNumero()));
        BigDecimal rendimento = banco.aplicarRendimento(conta.getNumero());

        assertAll(
                () -> assertEquals(valor("5.00"), rendimento),
                () -> assertEquals(valor("1005.00"), conta.getSaldo()));
    }

    @Test
    void falhaAoGravarNovaContaNaoCriaContaNemConsomeNumero() {
        repositorio.falharNaProximaGravacao = true;

        assertThrows(FalhaPersistenciaException.class, () -> banco.abrirContaPoupanca(ANA));
        Conta conta = banco.abrirContaPoupanca(ANA);

        assertAll(
                () -> assertEquals(1000, conta.getNumero()),
                () -> assertEquals(1, banco.listarContas().size()));
    }

    @Test
    void operacaoRecusadaPelaRegraNaoChegaAGravar() {
        ContaPoupanca conta = banco.abrirContaPoupanca(ANA);
        repositorio.chamadas.clear();

        assertThrows(SaldoInsuficienteException.class, () -> banco.sacar(conta.getNumero(), valor("0.01")));

        assertTrue(repositorio.chamadas.isEmpty());
    }

    @Test
    void operacaoEmContaInexistenteERecusada() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> banco.depositar(9999, valor("10.00")));

        assertAll(
                () -> assertEquals("Conta não encontrada: 9999", erro.getMessage()),
                () -> assertTrue(repositorio.chamadas.isEmpty()));
    }

    @Test
    void carregarRestauraContasHistoricoNumeracaoERendimento() throws SaldoInsuficienteException {
        ContaCorrente corrente = banco.abrirContaCorrente(ANA, valor("200.00"));
        ContaPoupanca poupanca = banco.abrirContaPoupanca(BRUNO);
        banco.depositar(poupanca.getNumero(), valor("1000.00"));
        banco.aplicarRendimento(poupanca.getNumero());
        banco.transferir(corrente.getNumero(), poupanca.getNumero(), valor("150.00"));

        Banco reiniciado = Banco.carregar(relogio, repositorio);

        Conta correnteLida = reiniciado.buscarContaPorNumero(corrente.getNumero()).orElseThrow();
        Conta poupancaLida = reiniciado.buscarContaPorNumero(poupanca.getNumero()).orElseThrow();
        assertAll(
                () -> assertEquals(valor("-150.00"), correnteLida.getSaldo()),
                () -> assertEquals(valor("50.00"), correnteLida.getSaldoDisponivel()),
                () -> assertEquals(corrente.getHistorico(), correnteLida.getHistorico()),
                () -> assertEquals(valor("1155.00"), poupancaLida.getSaldo()),
                () -> assertEquals(poupanca.getHistorico(), poupancaLida.getHistorico()),
                () -> assertThrows(IllegalStateException.class,
                        () -> reiniciado.aplicarRendimento(poupanca.getNumero())),
                () -> assertEquals(1002, reiniciado.abrirContaPoupanca(ANA).getNumero()));
    }

    @Test
    void carregarRecusaSaldoQueNaoCorrespondeAoHistorico() {
        banco.abrirContaPoupanca(ANA);
        banco.depositar(1000, valor("10.00"));
        ContaPersistida original = repositorio.gravadas.get(1000);
        repositorio.gravadas.put(1000, new ContaPersistida(1000, TipoConta.POUPANCA, "Ana", "24681357928",
                null, valor("999.00"), null, original.historico()));

        FalhaPersistenciaException erro = assertThrows(FalhaPersistenciaException.class,
                () -> Banco.carregar(relogio, repositorio));

        assertTrue(erro.getMessage().contains("conta 1000"), erro.getMessage());
    }

    @Test
    void carregarRecusaCpfInvalido() {
        repositorio.gravadas.put(1000, new ContaPersistida(1000, TipoConta.POUPANCA, "Ana", "11111111111",
                null, valor("0.00"), null, List.of()));

        assertThrows(FalhaPersistenciaException.class, () -> Banco.carregar(relogio, repositorio));
    }

    @Test
    void cpfNaoPodePertencerADoisTitulares() {
        banco.abrirContaPoupanca(ANA);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> banco.abrirContaPoupanca(new Cliente("Outra Pessoa", "246.813.579-28")));
        Conta segundaContaDaAna = banco.abrirContaCorrente(new Cliente("ana", "24681357928"), valor("0"));

        assertAll(
                () -> assertEquals("Este CPF já está cadastrado para outro titular.", erro.getMessage()),
                () -> assertEquals(1001, segundaContaDaAna.getNumero()));
    }

    private static BigDecimal valor(String texto) {
        return new BigDecimal(texto);
    }

    /** Guarda as contas em um mapa e pode simular uma falha na próxima gravação. */
    private static final class RepositorioControlado implements RepositorioBanco {
        private final Map<Integer, ContaPersistida> gravadas = new LinkedHashMap<>();
        private final List<List<Integer>> chamadas = new ArrayList<>();
        private boolean falharNaProximaGravacao;

        @Override
        public List<ContaPersistida> carregar() {
            return List.copyOf(gravadas.values());
        }

        @Override
        public void salvar(List<ContaPersistida> contas) {
            if (falharNaProximaGravacao) {
                falharNaProximaGravacao = false;
                throw new FalhaPersistenciaException("Falha simulada.");
            }
            chamadas.add(contas.stream().map(ContaPersistida::numero).toList());
            contas.forEach(conta -> gravadas.put(conta.numero(), conta));
        }
    }
}
