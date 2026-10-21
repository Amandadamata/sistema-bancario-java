package com.github.amandadamata.sistemabancario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Os CPFs usados aqui são sintéticos: foram gerados pelo próprio algoritmo a partir de sequências
 * arbitrárias (por exemplo, os dígitos de pi e de e). Não são CPFs de pessoas conhecidas.
 */
class CpfTest {

    @ParameterizedTest
    @CsvSource({
            "246.813.579-28, 24681357928",
            "24681357928,    24681357928",
            "135.792.468-28, 13579246828",
            "802.461.357-35, 80246135735",
            "314.159.265-90, 31415926590",
            "271.828.182-05, 27182818205",
            "' 246 813 579 28 ', 24681357928"})
    void aceitaCpfsValidosComOuSemPontuacaoENormaliza(String digitado, String esperado) {
        assertEquals(esperado, Cpf.de(digitado).digitos());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void cpfAusenteERecusado(String digitado) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> Cpf.de(digitado));
        assertEquals("O CPF é obrigatório.", erro.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2468135792", "246813579281", "123", "246.813.579-2"})
    void tamanhoErradoERecusado(String digitado) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> Cpf.de(digitado));
        assertEquals("O CPF deve ter 11 dígitos.", erro.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"246.813.579-2a", "246/813/579-28", "２４６８１３５７９２８"})
    void caracteresNaoPermitidosSaoRecusados(String digitado) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> Cpf.de(digitado));
        assertEquals("O CPF deve conter apenas números, pontos e hífen.", erro.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"000.000.000-00", "111.111.111-11", "222.222.222-22", "333.333.333-33",
            "444.444.444-44", "555.555.555-55", "666.666.666-66", "777.777.777-77",
            "888.888.888-88", "999.999.999-99"})
    void sequenciasRepetidasSaoRecusadasMesmoSatisfazendoOCalculo(String digitado) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> Cpf.de(digitado));
        assertEquals("CPF inválido: todos os dígitos são iguais.", erro.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"246.813.579-29", "246.813.579-38", "246.813.579-00", "246.813.578-28"})
    void digitosVerificadoresErradosSaoRecusados(String digitado) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> Cpf.de(digitado));
        assertEquals("CPF inválido: os dígitos verificadores não conferem.", erro.getMessage());
    }

    @Test
    void mensagensDeErroNaoRepetemONumeroInformado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> Cpf.de("246.813.579-29"));

        assertAll(
                () -> assertFalse(erro.getMessage().contains("246")),
                () -> assertFalse(erro.getMessage().contains("579")));
    }

    @Test
    void calculaOsDoisDigitosVerificadores() {
        // 1º dígito: 2*10 + 4*9 + 6*8 + 8*7 + 1*6 + 3*5 + 5*4 + 7*3 + 9*2 = 240; 240 % 11 = 9; 11 - 9 = 2.
        // 2º dígito: 2*11 + 4*10 + ... + 9*3 + 2*2 = 289; 289 % 11 = 3; 11 - 3 = 8.
        assertAll(
                () -> assertEquals(2, Cpf.digitoVerificador("246813579", 9)),
                () -> assertEquals(8, Cpf.digitoVerificador("2468135792", 10)));
    }

    @Test
    void formataMascaraEComparaPelosDigitos() {
        Cpf cpf = Cpf.de("24681357928");

        assertAll(
                () -> assertEquals("246.813.579-28", cpf.formatado()),
                () -> assertEquals("***.***.***-28", cpf.mascarado()),
                () -> assertEquals("***.***.***-28", cpf.toString()),
                () -> assertEquals(Cpf.de("246.813.579-28"), cpf),
                () -> assertEquals(Cpf.de("246.813.579-28").hashCode(), cpf.hashCode()),
                () -> assertNotEquals(Cpf.de("135.792.468-28"), cpf),
                () -> assertTrue(cpf.equals(cpf)));
    }
}
