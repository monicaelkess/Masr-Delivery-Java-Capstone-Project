package com.masrdelivery.dispatch;

import java.math.BigDecimal;

public final class Car implements Vehicle {
    @Override public String code() { return "CAR"; }
    @Override public String displayName() { return "Car"; }
    @Override public BigDecimal maxRangeKm() { return new BigDecimal("40"); }
    @Override public int averageSpeedKmh() { return 30; }
    @Override public int maxParcels() { return 60; }
}
