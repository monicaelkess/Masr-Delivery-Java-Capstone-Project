package com.masrdelivery.service;

import com.masrdelivery.exception.DuplicateEntityException;
import com.masrdelivery.exception.EntityNotFoundException;
import com.masrdelivery.exception.OperationNotAllowedException;
import com.masrdelivery.model.Cuisine;
import com.masrdelivery.model.Customer;
import com.masrdelivery.model.Restaurant;
import com.masrdelivery.order.Order;
import com.masrdelivery.promotion.Promotion;
import com.masrdelivery.repo.InMemoryRepository;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Restaurants, search and promotions. */
public final class CatalogService {

    /** Rating high to low, ties alphabetical by name, then id so the order is total (C.4). */
    public static final Comparator<Restaurant> BY_RATING = Comparator
            .comparingDouble(Restaurant::rating).reversed()
            .thenComparing(Restaurant::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Restaurant::id);

    private final InMemoryRepository<Restaurant> restaurants;
    private final InMemoryRepository<Promotion> promotions;
    private final InMemoryRepository<Order> orders;

    public CatalogService(InMemoryRepository<Restaurant> restaurants, InMemoryRepository<Promotion> promotions,
                          InMemoryRepository<Order> orders) {
        this.restaurants = restaurants;
        this.promotions = promotions;
        this.orders = orders;
    }

    public Restaurant addRestaurant(Restaurant r) throws DuplicateEntityException { return restaurants.add(r); }

    public Restaurant removeRestaurant(String id) throws EntityNotFoundException, OperationNotAllowedException {
        Restaurant r = restaurants.get(id);
        long live = orders.all().stream().filter(o -> o.restaurant().equals(r) && !o.status().isTerminal()).count();
        if (live > 0)
            throw new OperationNotAllowedException(r.name() + " still has " + live + " active order(s); finish or cancel them first.");
        return restaurants.remove(id);
    }

    public Restaurant restaurant(String id) throws EntityNotFoundException { return restaurants.get(id); }

    public List<Restaurant> restaurantsByRating() { return search(RestaurantFilters.any()); }

    /** The generic search: the caller supplies the condition (Part D search facility). */
    public List<Restaurant> search(Predicate<Restaurant> condition) {
        return restaurants.all().stream().filter(condition).sorted(BY_RATING).toList();
    }

    /** Customer "browse": open restaurants only, optional extra filter. */
    public List<Restaurant> browse(Predicate<Restaurant> filter) { return search(RestaurantFilters.open().and(filter)); }

    /** Customer free-text search; remembered in the customer's last-five list. */
    public List<Restaurant> searchText(Customer customer, String text) {
        customer.recordSearch(text);
        return search(RestaurantFilters.matchesText(text));
    }

    /** Distinct cuisines currently offered, alphabetical, no duplicates (C.3). */
    public SortedSet<Cuisine> distinctCuisines() {
        return Collections.unmodifiableSortedSet(restaurants.all().stream()
                .flatMap(r -> r.cuisines().stream())
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Cuisine::displayName)))));
    }

    public Promotion addPromotion(Promotion p) throws DuplicateEntityException { return promotions.add(p); }

    /** Case-insensitive lookup. */
    public Optional<Promotion> findPromotion(String code) {
        return code == null || code.isBlank() ? Optional.empty() : promotions.find(Promotion.normalize(code));
    }

    public List<Promotion> promotions() {
        return promotions.all().stream().sorted(Comparator.comparing(Promotion::code)).toList();
    }
}
