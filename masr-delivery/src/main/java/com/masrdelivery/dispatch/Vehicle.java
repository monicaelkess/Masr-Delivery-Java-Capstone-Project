package com.masrdelivery.dispatch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * STRATEGY for rider dispatch behaviour. A Rider HAS-A Vehicle (composition), rather
 * than MotorcycleRider/BicycleRider subclasses, so an electric scooter is one new class
 * registered in {@link VehicleRegistry}; no Rider or dispatch code changes.
 */
public interface Vehicle {
    String code();
    String displayName();
    /** Longest restaurant-to-customer distance this vehicle accepts. */
    BigDecimal maxRangeKm();
    int averageSpeedKmh();
    /** Largest order (in parcels) the vehicle can carry. */
    int maxParcels();

    default boolean canCarry(BigDecimal distanceKm, int parcels) {
        return distanceKm.compareTo(maxRangeKm()) <= 0 && parcels <= maxParcels();
    }

    default Duration estimatedTravelTime(BigDecimal distanceKm) {
        long minutes = distanceKm.multiply(BigDecimal.valueOf(60))
                .divide(BigDecimal.valueOf(averageSpeedKmh()), 0, RoundingMode.CEILING).longValueExact();
        return Duration.ofMinutes(minutes);
    }
}
