package com.masrdelivery.model;

import com.masrdelivery.exception.InvalidInputException;

import java.util.Arrays;

public enum Cuisine {
    EGYPTIAN("Egyptian"), GRILL("Grill"), KOSHARY("Koshary"), SEAFOOD("Seafood"), PIZZA("Pizza"),
    BURGERS("Burgers"), SYRIAN("Syrian"), ASIAN("Asian"), DESSERTS("Desserts"), CAFE("Cafe");

    private final String displayName;
    Cuisine(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }

    public static Cuisine parse(String text) {
        String key = text == null ? "" : text.trim().replace(' ', '_').toUpperCase();
        return Arrays.stream(values()).filter(c -> c.name().equals(key)).findFirst()
                .orElseThrow(() -> new InvalidInputException("Unknown cuisine: " + text));
    }

    @Override public String toString() { return displayName; }
}
