package com.masrdelivery.model;

import com.masrdelivery.exception.InvalidInputException;

import java.util.Arrays;

public enum District {
    MAADI("Maadi"), DOKKI("Dokki"), FAISAL("Faisal"), NASR_CITY("Nasr City"),
    HELIOPOLIS("Heliopolis"), ZAMALEK("Zamalek"), MOHANDESSIN("Mohandessin");

    private final String displayName;
    District(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }


    public static District parse(String text) {
        String key = text == null ? "" : text.trim().replace(' ', '_').replace('-', '_').toUpperCase();
        return Arrays.stream(values()).filter(d -> d.name().equals(key)).findFirst()
                .orElseThrow(() -> new InvalidInputException("Unknown district: " + text));
    }

    @Override public String toString() { return displayName; }
}
