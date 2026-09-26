package com.masrdelivery.service;

import com.masrdelivery.menu.MenuItem;
import com.masrdelivery.model.Cuisine;
import com.masrdelivery.model.District;
import com.masrdelivery.model.Restaurant;
import com.masrdelivery.money.Money;

import java.util.Locale;
import java.util.function.Predicate;


public final class RestaurantFilters {
    private RestaurantFilters() {}

    public static Predicate<Restaurant> any()                        { return r -> true; }
    public static Predicate<Restaurant> open()                       { return Restaurant::isOpen; }
    public static Predicate<Restaurant> inDistrict(District d)       { return r -> r.district() == d; }
    public static Predicate<Restaurant> servesCuisine(Cuisine c)     { return r -> r.cuisines().contains(c); }
    public static Predicate<Restaurant> minRating(double min)        { return r -> r.rating() >= min; }

    /** Has at least one currently available item priced at or below the ceiling. */
    public static Predicate<Restaurant> hasItemAtOrBelow(Money ceiling) {
        return r -> r.menu().stream().filter(MenuItem::isAvailable).anyMatch(i -> !ceiling.isLessThan(i.unitPrice()));
    }

    /** Case-insensitive match on name or any cuisine. */
    public static Predicate<Restaurant> matchesText(String text) {
        String needle = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        return r -> !needle.isEmpty() && (r.name().toLowerCase(Locale.ROOT).contains(needle)
                || r.cuisines().stream().anyMatch(c -> c.displayName().toLowerCase(Locale.ROOT).contains(needle)));
    }
}
