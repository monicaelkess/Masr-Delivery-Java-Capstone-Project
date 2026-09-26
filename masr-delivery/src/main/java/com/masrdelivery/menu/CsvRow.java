package com.masrdelivery.menu;

import com.masrdelivery.exception.InvalidInputException;

import java.util.Map;

/** One data-file row, addressed by header name. */
public record CsvRow(int lineNumber, Map<String, String> fields) {
    public CsvRow { fields = Map.copyOf(fields); }

    public String get(String column) {
        String v = fields.get(column);
        if (v == null || v.isBlank()) throw new InvalidInputException("Line " + lineNumber + ": missing '" + column + "'");
        return v.trim();
    }

    public String getOrDefault(String column, String fallback) {
        String v = fields.get(column);
        return v == null || v.isBlank() ? fallback : v.trim();
    }
}
