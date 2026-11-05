package com.github.amandadamata.sistemabancario.ui;

import com.github.amandadamata.sistemabancario.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class ModeloHistoricoTest {
    @Test void apresentaMaisRecentesPrimeiroComSinaisDataEDestino() throws Exception {
        Banco banco = new Banco(Clock.fixed(Instant.parse("2026-10-24T12:30:15Z"), ZoneOffset.UTC));
        Conta conta = banco.abrirContaPoupanca(new Cliente("Ana", "24681357928"));
        banco.abrirContaPoupanca(conta.getTitular());
        banco.depositar(1000, new BigDecimal("100.50"));
        banco.transferir(1000, 1001, new BigDecimal("25.10"));
        ModeloHistorico modelo = new ModeloHistorico(conta, 5);
        assertEquals("24/10/2026 12:30:15", modelo.getValueAt(0, 0));
        assertEquals("Transfer sent · account 1001", modelo.getValueAt(0, 1));
        assertEquals("− R$ 25,10", modelo.getValueAt(0, 2));
        assertEquals("R$ 75,40", modelo.getValueAt(0, 3));
        assertEquals("+ R$ 100,50", modelo.getValueAt(1, 2));
        assertFalse(modelo.entrada(0));
        assertTrue(modelo.entrada(1));
        assertFalse(modelo.isCellEditable(0, 2));
        assertEquals(TipoTransacao.DEPOSITO, conta.getHistorico().get(0).tipo());
        assertEquals(1, new ModeloHistorico(conta, 1).getRowCount());
    }

    @Test void contaSemOperacoesTemExtratoVazio() {
        Banco banco = new Banco(Clock.systemUTC());
        Conta conta = banco.abrirContaPoupanca(new Cliente("Ana", "24681357928"));
        assertEquals(0, new ModeloHistorico(conta, 5).getRowCount());
    }
}
