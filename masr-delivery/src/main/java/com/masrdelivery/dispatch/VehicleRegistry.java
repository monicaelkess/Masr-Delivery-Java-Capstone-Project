package com.masrdelivery.dispatch;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class VehicleRegistry {
    private final Map<String, Vehicle> vehicles = new ConcurrentHashMap<>();

    public static VehicleRegistry withDefaults() {
        VehicleRegistry r = new VehicleRegistry();
        r.register(new Motorcycle());
        r.register(new Bicycle());
        r.register(new Car());
        return r;
    }

    public void register(Vehicle v) {
        if (vehicles.putIfAbsent(v.code().toUpperCase(Locale.ROOT), v) != null)
            throw new IllegalStateException("Vehicle type already registered: " + v.code());
    }

    public Optional<Vehicle> find(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(vehicles.get(code.trim().toUpperCase(Locale.ROOT)));
    }

    public List<Vehicle> all() {
        return vehicles.values().stream().sorted(Comparator.comparing(Vehicle::code)).toList();
    }
}
