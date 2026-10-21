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
                () -> new Cliente(nome, "246.813.579-28"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void cpfObrigatorio(String cpf) {
        assertThrows(IllegalArgumentException.class,
                () -> new Cliente("Cliente", cpf));
    }

    @Test
    void removeEspacosDasExtremidadesENormalizaOCpf() {
        Cliente cliente = new Cliente("  Ana Silva  ", "  246.813.579-28  ");

        assertAll(
                () -> assertEquals("Ana Silva", cliente.getNome()),
                () -> assertEquals("24681357928", cliente.getCpf().digitos()));
    }

    @Test
    void cpfInvalidoImpedeACriacaoDoCliente() {
        assertThrows(IllegalArgumentException.class, () -> new Cliente("Ana Silva", "246.813.579-29"));
    }

    @Test
    void toStringNaoExpoeOCpfCompleto() {
        Cliente cliente = new Cliente("Ana Silva", "246.813.579-28");

        assertEquals("Ana Silva (CPF ***.***.***-28)", cliente.toString());
    }
}
