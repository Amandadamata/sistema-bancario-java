package com.github.amandadamata.bankingsystem;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Test clock that only moves when the test asks, which makes date rules predictable. */
final class AdjustableClock extends Clock {

    private Instant moment;

    AdjustableClock(Instant initialInstant) {
        this.moment = initialInstant;
    }

    static AdjustableClock at(String isoInstant) {
        return new AdjustableClock(Instant.parse(isoInstant));
    }

    void advance(Duration duration) {
        moment = moment.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("Tests always use UTC.");
    }

    @Override
    public Instant instant() {
        return moment;
    }
}
