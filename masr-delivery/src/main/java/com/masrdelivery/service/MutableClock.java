package com.masrdelivery.service;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;


public final class MutableClock extends Clock {
    public static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

    private final ZoneId zone;
    private final AtomicReference<Instant> pinned = new AtomicReference<>();

    public MutableClock(ZoneId zone) { this.zone = zone; }
    public static MutableClock system() { return new MutableClock(CAIRO); }

    public void pin(LocalDateTime local) { pinned.set(local.atZone(zone).toInstant()); }
    public void advance(Duration d)      { pinned.updateAndGet(i -> (i == null ? Instant.now() : i).plus(d)); }
    public void unpin()                  { pinned.set(null); }

    @Override public ZoneId getZone() { return zone; }
    @Override public Clock withZone(ZoneId z) { throw new UnsupportedOperationException(); }
    @Override public Instant instant() { Instant p = pinned.get(); return p != null ? p : Instant.now(); }
}
