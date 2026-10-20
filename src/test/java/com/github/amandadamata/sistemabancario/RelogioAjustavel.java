package com.github.amandadamata.sistemabancario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relógio de teste que só avança quando o teste pede, tornando regras de data previsíveis. */
final class RelogioAjustavel extends Clock {

    private Instant instante;

    RelogioAjustavel(Instant instanteInicial) {
        this.instante = instanteInicial;
    }

    static RelogioAjustavel em(String instanteIso) {
        return new RelogioAjustavel(Instant.parse(instanteIso));
    }

    void avancar(Duration duracao) {
        instante = instante.plus(duracao);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("Os testes usam sempre UTC.");
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
