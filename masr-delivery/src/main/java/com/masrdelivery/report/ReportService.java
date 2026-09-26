package com.masrdelivery.report;

import com.masrdelivery.dispatch.Rider;
import com.masrdelivery.exception.InvalidInputException;
import com.masrdelivery.menu.LineItem;
import com.masrdelivery.menu.MenuItem;
import com.masrdelivery.model.Customer;
import com.masrdelivery.model.District;
import com.masrdelivery.model.Restaurant;
import com.masrdelivery.money.Money;
import com.masrdelivery.money.MoneyCollectors;
import com.masrdelivery.order.Order;
import com.masrdelivery.order.OrderStatus;
import com.masrdelivery.repo.InMemoryRepository;
import com.masrdelivery.service.CatalogService;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class ReportService {

    public record RestaurantRevenue(Restaurant restaurant, Money revenue, long deliveredOrders) {}
    public record RiderStats(Rider rider, long deliveries, Optional<Duration> averageDeliveryDuration) {}
    public record ItemPopularity(MenuItem item, String restaurantName, long timesOrdered) {}
    public record CustomerHistory(Customer customer, List<Order> ordersNewestFirst, Money totalSpent) {}
    public record PeakHour(int hour, long orders) {}
    public record RestaurantDay(List<Order> orders, Money deliveredRevenue, Money openOrderValue) {}
    public record PlatformSnapshot(int restaurants, int customers, int riders, long ridersOnDuty, long ordersTotal,
                                   long ordersActive, Money deliveredRevenue, Optional<Money> averageOrderValue,
                                   long readyAwaitingRider) {}

    private final InMemoryRepository<Order> orders;
    private final InMemoryRepository<Restaurant> restaurants;
    private final InMemoryRepository<Customer> customers;
    private final InMemoryRepository<Rider> riders;
    private final Clock clock;

    public ReportService(InMemoryRepository<Order> orders, InMemoryRepository<Restaurant> restaurants,
                         InMemoryRepository<Customer> customers, InMemoryRepository<Rider> riders, Clock clock) {
        this.orders = orders;
        this.restaurants = restaurants;
        this.customers = customers;
        this.riders = riders;
        this.clock = clock;
    }

    private Stream<Order> delivered() { return orders.all().stream().filter(o -> o.status() == OrderStatus.DELIVERED); }

    /** 1. Revenue for [from, to], both days inclusive. */
    public Money revenue(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new InvalidInputException("Start date " + from + " is after end date " + to + ".");
        return delivered()
                .filter(o -> !o.placedAt().toLocalDate().isBefore(from) && !o.placedAt().toLocalDate().isAfter(to))
                .collect(MoneyCollectors.summing(o -> o.price().total()));
    }

    /** 2. Top five by revenue in a month; ties broken by name. */
    public List<RestaurantRevenue> topRestaurantsByRevenue(YearMonth month) {
        return delivered()
                .filter(o -> YearMonth.from(o.placedAt()).equals(month))
                .collect(Collectors.groupingBy(Order::restaurant, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new RestaurantRevenue(e.getKey(),
                        e.getValue().stream().collect(MoneyCollectors.summing(o -> o.price().total())),
                        e.getValue().size()))
                .sorted(Comparator.comparing(RestaurantRevenue::revenue).reversed()
                        .thenComparing(rr -> rr.restaurant().name()))
                .limit(5)
                .toList();
    }

    /** 3. Average delivered-order value per delivery district (districts with no orders omitted). */
    public Map<District, Money> averageOrderValueByDistrict() {
        return Collections.unmodifiableMap(delivered().collect(Collectors.groupingBy(
                o -> o.deliveryAddress().district(), () -> new EnumMap<>(District.class),
                Collectors.collectingAndThen(MoneyCollectors.averaging((Order o) -> o.price().total()), Optional::orElseThrow))));
    }

    /** 4. Rating above 4.5 AND at least 20 completed (delivered) orders. */
    public List<Restaurant> highlyRatedBusyRestaurants() {
        Map<Restaurant, Long> completed = delivered().collect(Collectors.groupingBy(Order::restaurant, Collectors.counting()));
        return restaurants.all().stream()
                .filter(r -> r.rating() > 4.5)
                .filter(r -> completed.getOrDefault(r, 0L) >= 20)
                .sorted(CatalogService.BY_RATING)
                .toList();
    }

    /** 5. Orders per current status; every status is present, zero if none. */
    public Map<OrderStatus, Long> countByStatus() {
        Map<OrderStatus, Long> counts = orders.all().stream()
                .collect(Collectors.groupingBy(Order::status, () -> new EnumMap<>(OrderStatus.class), Collectors.counting()));
        return Collections.unmodifiableMap(Arrays.stream(OrderStatus.values()).collect(Collectors.toMap(
                Function.identity(), s -> counts.getOrDefault(s, 0L), Long::sum, () -> new EnumMap<>(OrderStatus.class))));
    }

    /** 6. Deliveries and average pick-up-to-hand-over time per rider, most deliveries first. */
    public List<RiderStats> riderStatistics() {
        Map<String, List<Duration>> durations = delivered()
                .filter(o -> o.riderId().isPresent())
                .collect(Collectors.groupingBy(o -> o.riderId().orElseThrow(),
                        Collectors.mapping(o -> o.deliveryDuration().orElse(Duration.ZERO), Collectors.toList())));
        return riders.all().stream()
                .map(r -> {
                    List<Duration> ds = durations.getOrDefault(r.id(), List.of());
                    Optional<Duration> avg = ds.isEmpty() ? Optional.empty()
                            : Optional.of(Duration.ofSeconds(ds.stream().mapToLong(Duration::toSeconds).sum() / ds.size()));
                    return new RiderStats(r, ds.size(), avg);
                })
                .sorted(Comparator.comparingLong(RiderStats::deliveries).reversed()
                        .thenComparing(s -> s.rider().name()))
                .toList();
    }

    /** 7. Most frequently ordered item (number of order lines containing it). Empty when nothing was ever ordered. */
    public Optional<ItemPopularity> mostFrequentlyOrderedItem() {
        return orders.all().stream()
                .filter(o -> o.status() != OrderStatus.CANCELLED)
                .flatMap(o -> o.lines().stream().map(l -> Map.entry(l.item(), o.restaurant().name())))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.teeing(
                        Collectors.counting(),
                        Collectors.mapping(Map.Entry::getValue, Collectors.reducing("", (a, b) -> a.isEmpty() ? b : a)),
                        (count, restaurant) -> new ItemPopularity(null, restaurant, count))))
                .entrySet().stream()
                .map(e -> new ItemPopularity(e.getKey(), e.getValue().restaurantName(), e.getValue().timesOrdered()))
                .max(Comparator.comparingLong(ItemPopularity::timesOrdered)
                        .thenComparing(p -> p.item().name(), Comparator.reverseOrder()));   // tie -> alphabetically first
    }

    /** 8. Full history newest first, plus lifetime spend. */
    public CustomerHistory history(Customer c) {
        List<Order> mine = orders.all().stream()
                .filter(o -> o.customer().equals(c))
                .sorted(Comparator.comparing(Order::placedAt).thenComparing(Order::id).reversed())
                .toList();
        Money spent = mine.stream().filter(o -> o.isPaid() && !o.isRefunded())
                .collect(MoneyCollectors.summing(o -> o.price().total()));
        return new CustomerHistory(c, mine, spent);
    }

    /** 9. Busiest hour of day across all orders; ties go to the earlier hour. Empty with no orders. */
    public Optional<PeakHour> peakOrderingHour() {
        return orders.all().stream()
                .collect(Collectors.groupingBy(o -> o.placedAt().getHour(), TreeMap::new, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<Integer, Long>comparingByValue().thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(e -> new PeakHour(e.getKey(), e.getValue()));
    }

    /** 10. Customers with no order placed in the last {@code days} days (including those who never ordered). */
    public List<Customer> inactiveCustomers(int days) {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(days);
        Set<Customer> active = orders.all().stream()
                .filter(o -> !o.placedAt().isBefore(cutoff))
                .map(Order::customer)
                .collect(Collectors.toSet());
        return customers.all().stream().filter(c -> !active.contains(c))
                .sorted(Comparator.comparing(Customer::name)).toList();
    }

    /** Last order date per customer, for display next to report 10. */
    public Optional<LocalDateTime> lastOrderAt(Customer c) {
        return orders.all().stream().filter(o -> o.customer().equals(c)).map(Order::placedAt).max(Comparator.naturalOrder());
    }

    /** Restaurant area: today's orders and revenue. */
    public RestaurantDay today(Restaurant r) {
        LocalDate today = LocalDate.now(clock);
        List<Order> todays = orders.all().stream()
                .filter(o -> o.restaurant().equals(r) && o.placedAt().toLocalDate().equals(today))
                .sorted(Comparator.comparing(Order::placedAt).thenComparing(Order::id))
                .toList();
        Money deliveredRevenue = todays.stream().filter(o -> o.status() == OrderStatus.DELIVERED)
                .collect(MoneyCollectors.summing(o -> o.price().total()));
        Money open = todays.stream().filter(o -> !o.status().isTerminal())
                .collect(MoneyCollectors.summing(o -> o.price().total()));
        return new RestaurantDay(todays, deliveredRevenue, open);
    }

    public List<Order> ordersFor(Restaurant r, OrderStatus status) {
        return orders.all().stream().filter(o -> o.restaurant().equals(r) && o.status() == status)
                .sorted(Comparator.comparing(Order::placedAt).thenComparing(Order::id)).toList();
    }

    public List<Order> activeOrdersFor(Customer c) {
        return orders.all().stream().filter(o -> o.customer().equals(c) && !o.status().isTerminal())
                .sorted(Comparator.comparing(Order::placedAt).thenComparing(Order::id)).toList();
    }

    public PlatformSnapshot snapshot(long readyAwaitingRider) {
        List<Order> all = orders.all();
        return new PlatformSnapshot(restaurants.size(), customers.size(), riders.size(),
                riders.all().stream().filter(Rider::isOnDuty).count(),
                all.size(), all.stream().filter(o -> !o.status().isTerminal()).count(),
                delivered().collect(MoneyCollectors.summing(o -> o.price().total())),
                delivered().collect(MoneyCollectors.averaging(o -> o.price().total())),
                readyAwaitingRider);
    }
}
