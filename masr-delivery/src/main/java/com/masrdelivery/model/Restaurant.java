package com.masrdelivery.model;

import com.masrdelivery.exception.*;
import com.masrdelivery.menu.MenuItem;

import java.math.BigDecimal;
import java.util.*;


public final class Restaurant implements Identifiable {

    private final String id;
    private final String name;
    private final District district;
    private final Set<Cuisine> cuisines;
    private volatile double rating;
    private volatile boolean open;

    private final Map<String, MenuItem> menu = new LinkedHashMap<>();
    private final Map<String, BigDecimal> stock = new HashMap<>();

    public Restaurant(String id, String name, District district, Set<Cuisine> cuisines, double rating, boolean open) {
        this.id = Validation.requireText(id, "Restaurant id");
        this.name = Validation.requireText(name, "Restaurant name");
        this.district = Validation.requireNonNull(district, "District");
        if (cuisines == null || cuisines.isEmpty()) throw new InvalidInputException("A restaurant needs at least one cuisine.");
        this.cuisines = Collections.unmodifiableSet(EnumSet.copyOf(cuisines));
        this.rating = Validation.requireRating(rating);
        this.open = open;
    }

    @Override public String id() { return id; }
    public String name()          { return name; }
    public District district()    { return district; }
    public Set<Cuisine> cuisines(){ return cuisines; }
    public double rating()        { return rating; }
    public boolean isOpen()       { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public void setRating(double rating) { this.rating = Validation.requireRating(rating); }

    public synchronized void addMenuItem(MenuItem item, BigDecimal dailyStock) throws DuplicateEntityException {
        Validation.requireNonNull(item, "Menu item");
        if (!item.restaurantId().equals(id))
            throw new InvalidInputException("Item " + item.id() + " belongs to restaurant " + item.restaurantId());
        Validation.requireNonNegative(dailyStock, "Stock");
        if (menu.putIfAbsent(item.id(), item) != null)
            throw new DuplicateEntityException("Menu item id " + item.id() + " already exists at " + name + ".");
        stock.put(item.id(), dailyStock);
    }

    public synchronized MenuItem removeMenuItem(String itemId) throws EntityNotFoundException {
        MenuItem removed = menu.remove(itemId);
        if (removed == null) throw new EntityNotFoundException("No item " + itemId + " at " + name + ".");
        stock.remove(itemId);
        return removed;
    }


    public synchronized List<MenuItem> menu() { return List.copyOf(menu.values()); }

    public synchronized Optional<MenuItem> findItem(String itemId) { return Optional.ofNullable(menu.get(itemId)); }

    public synchronized BigDecimal stockOf(String itemId) { return stock.getOrDefault(itemId, BigDecimal.ZERO); }

    public synchronized void setStock(String itemId, BigDecimal quantity) throws EntityNotFoundException {
        if (!menu.containsKey(itemId)) throw new EntityNotFoundException("No item " + itemId + " at " + name + ".");
        stock.put(itemId, Validation.requireNonNegative(quantity, "Stock"));
    }


    public synchronized void reserve(Map<MenuItem, BigDecimal> totals)
            throws RestaurantClosedException, ItemUnavailableException, StockShortageException {
        if (!open) throw new RestaurantClosedException(name + " is closed right now.");
        for (Map.Entry<MenuItem, BigDecimal> e : totals.entrySet()) {
            MenuItem item = e.getKey();
            MenuItem current = menu.get(item.id());
            if (current == null || !current.equals(item))
                throw new ItemUnavailableException(item.name() + " is not on " + name + "'s menu.");
            if (!current.isAvailable())
                throw new ItemUnavailableException(item.name() + " is currently unavailable.");
            BigDecimal left = stock.getOrDefault(item.id(), BigDecimal.ZERO);
            if (left.compareTo(e.getValue()) < 0)
                throw new StockShortageException("Only " + item.unit().format(left) + " of " + item.name()
                        + " left today; you asked for " + item.unit().format(e.getValue()) + ".");
        }
        totals.forEach((item, qty) -> stock.merge(item.id(), qty, BigDecimal::subtract));
    }

    public synchronized void release(Map<MenuItem, BigDecimal> totals) {
        totals.forEach((item, qty) -> { if (menu.containsKey(item.id())) stock.merge(item.id(), qty, BigDecimal::add); });
    }

    @Override public boolean equals(Object o) { return o instanceof Restaurant r && id.equals(r.id); }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return name + " [" + id + "]"; }
}
