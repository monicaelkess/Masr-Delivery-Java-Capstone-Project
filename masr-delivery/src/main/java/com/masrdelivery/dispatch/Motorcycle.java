package com.masrdelivery.dispatch;

import java.math.BigDecimal;

public final class Motorcycle implements Vehicle {
    @Override public String code() { return "MOTORCYCLE"; }
    @Override public String displayName() { return "Motorcycle"; }
    @Override public BigDecimal maxRangeKm() { return new BigDecimal("20"); }
    @Override public int averageSpeedKmh() { return 35; }
    @Override public int maxParcels() { return 15; }
}
