package com.github.amandadamata.sistemabancario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClienteTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void nomeObrigatorio(String nome) {
        assertThrows(IllegalArgumentException.class,
                () -> new Cliente(nome, "000.000.000-00"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void cpfObrigatorio(String cpf) {
        assertThrows(IllegalArgumentException.class,
                () -> new Cliente("Cliente", cpf));
    }

    @Test
    void removeEspacosDasExtremidades() {
        Cliente cliente = new Cliente("  Ana Silva  ", "  000.000.000-00  ");

        assertAll(
                () -> assertEquals("Ana Silva", cliente.getNome()),
                () -> assertEquals("000.000.000-00", cliente.getCpf()));
    }
}
