package com.github.amandadamata.bankingsystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void nameRequired(String name) {
        assertThrows(IllegalArgumentException.class,
                () -> new Customer(name, "246.813.579-28"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void cpfRequired(String cpf) {
        assertThrows(IllegalArgumentException.class,
                () -> new Customer("Customer", cpf));
    }

    @Test
    void trimsSpacesAndNormalizesTheCpf() {
        Customer customer = new Customer("  Ana Silva  ", "  246.813.579-28  ");

        assertAll(
                () -> assertEquals("Ana Silva", customer.getName()),
                () -> assertEquals("24681357928", customer.getCpf().digits()));
    }

    @Test
    void invalidCpfStopsTheCustomerFromBeingCreated() {
        assertThrows(IllegalArgumentException.class, () -> new Customer("Ana Silva", "246.813.579-29"));
    }

    @Test
    void toStringDoesNotExposeTheFullCpf() {
        Customer customer = new Customer("Ana Silva", "246.813.579-28");

        assertEquals("Ana Silva (CPF ***.***.***-28)", customer.toString());
    }
}
