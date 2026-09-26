package com.masrdelivery.dispatch;

import java.math.BigDecimal;

public final class Bicycle implements Vehicle {
    @Override public String code() { return "BICYCLE"; }
    @Override public String displayName() { return "Bicycle"; }
    @Override public BigDecimal maxRangeKm() { return new BigDecimal("5"); }
    @Override public int averageSpeedKmh() { return 15; }
    @Override public int maxParcels() { return 6; }
}
