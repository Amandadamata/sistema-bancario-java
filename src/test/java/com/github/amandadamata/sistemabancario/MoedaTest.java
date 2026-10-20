package com.github.amandadamata.sistemabancario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoedaTest {

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "100,50; 100.50",
            "100.50; 100.50",
            "1000; 1000.00",
            "' 7,5 '; 7.50",
            "0; 0.00"})
    void converteVirgulaOuPontoComoSeparadorDecimal(String texto, String esperado) {
        assertEquals(new BigDecimal(esperado), Moeda.converter(texto));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "-10", "1.234,56", "10,005", "1e3", "NaN", ",50", "10,"})
    void recusaFormatosInvalidosOuAmbiguos(String texto) {
        assertThrows(IllegalArgumentException.class, () -> Moeda.converter(texto));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "0.30; R$ 0,30",
            "1234.5; R$ 1.234,50",
            "-150.00; R$ -150,00",
            "1000000; R$ 1.000.000,00"})
    void formataNoPadraoBrasileiro(String valor, String esperado) {
        assertEquals(esperado, Moeda.formatar(new BigDecimal(valor)));
    }

    @Test
    void formatarExigeValor() {
        assertThrows(NullPointerException.class, () -> Moeda.formatar(null));
    }
}
