package com.masrdelivery.menu;

import com.masrdelivery.exception.DuplicateEntityException;
import com.masrdelivery.exception.EntityNotFoundException;
import com.masrdelivery.exception.InvalidInputException;
import com.masrdelivery.model.Restaurant;
import com.masrdelivery.money.Money;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;


public final class MenuItemFactory {

    private final Map<String, MenuItemParser> parsers = new LinkedHashMap<>();

    public static MenuItemFactory withDefaults() {
        MenuItemFactory f = new MenuItemFactory();
        f.register("STANDARD", (row, r) -> new StandardItem(r.id(), row.get("item_id"), row.get("name"),
                row.get("category"), prepMinutes(row), available(row), Money.of(row.get("price"))));
        f.register("WEIGHTED", (row, r) -> new WeightedItem(r.id(), row.get("item_id"), row.get("name"),
                row.get("category"), prepMinutes(row), available(row), Money.of(row.get("price"))));
        f.register("COMBO", MenuItemFactory::parseCombo);
        return f;
    }

    public synchronized void register(String type, MenuItemParser parser) {
        String key = type.trim().toUpperCase(Locale.ROOT);
        if (parsers.putIfAbsent(key, Objects.requireNonNull(parser)) != null)
            throw new IllegalStateException("Parser already registered for type " + key);
    }

    public synchronized Set<String> supportedTypes() { return Set.copyOf(parsers.keySet()); }

    public MenuItem create(CsvRow row, Restaurant restaurant) {
        String type = row.get("type").toUpperCase(Locale.ROOT);
        MenuItemParser parser;
        synchronized (this) { parser = parsers.get(type); }
        if (parser == null)
            throw new InvalidInputException("Line " + row.lineNumber() + ": unknown menu item type '" + type
                    + "'. Supported: " + supportedTypes());
        return parser.parse(row, restaurant);
    }

    /** Reads a whole menu file, adding each item to its restaurant in file order. */
    public int load(BufferedReader reader, Function<String, Optional<Restaurant>> restaurantLookup)
            throws DuplicateEntityException, EntityNotFoundException {
        try {
            String headerLine = reader.readLine();
            if (headerLine == null) return 0;
            String[] header = headerLine.split(",", -1);
            int count = 0, lineNo = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] cells = line.split(",", -1);
                Map<String, String> fields = new HashMap<>();
                for (int i = 0; i < header.length && i < cells.length; i++) fields.put(header[i].trim(), cells[i]);
                CsvRow row = new CsvRow(lineNo, fields);
                String rid = row.get("restaurant_id");
                Restaurant r = restaurantLookup.apply(rid)
                        .orElseThrow(() -> new EntityNotFoundException("Menu file references unknown restaurant " + rid));
                r.addMenuItem(create(row, r), new BigDecimal(row.getOrDefault("stock", "100")));
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static MenuItem parseCombo(CsvRow row, Restaurant r) {
        String[] spec = row.get("extra").split("@");
        if (spec.length != 2) throw new InvalidInputException("Line " + row.lineNumber() + ": combo extra must be ITEMS@DISCOUNT");
        List<MenuItem> components = Arrays.stream(spec[0].split("\\+"))
                .map(String::trim)
                .map(id -> r.findItem(id).orElseThrow(() -> new InvalidInputException(
                        "Line " + row.lineNumber() + ": combo component " + id + " not found (list components before the combo)")))
                .toList();
        return new ComboItem(r.id(), row.get("item_id"), row.get("name"), row.get("category"),
                prepMinutes(row), available(row), components, new BigDecimal(spec[1].trim()));
    }

    private static int prepMinutes(CsvRow row) {
        try { return Integer.parseInt(row.get("prep_minutes")); }
        catch (NumberFormatException e) { throw new InvalidInputException("Line " + row.lineNumber() + ": bad prep_minutes"); }
    }

    private static boolean available(CsvRow row) { return Boolean.parseBoolean(row.getOrDefault("available", "true")); }
}
