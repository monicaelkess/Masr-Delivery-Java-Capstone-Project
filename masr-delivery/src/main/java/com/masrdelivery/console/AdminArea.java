package com.masrdelivery.console;

import com.masrdelivery.exception.PlatformException;
import com.masrdelivery.model.*;
import com.masrdelivery.money.Money;
import com.masrdelivery.order.OrderStatus;
import com.masrdelivery.promotion.*;
import com.masrdelivery.report.ReportService;
import com.masrdelivery.service.Platform;
import com.masrdelivery.service.RestaurantFilters;
import com.masrdelivery.service.RushHourSimulation;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Predicate;

final class AdminArea {
    private final Platform p;
    private final InputReader in;
    private final PrintStream out;

    AdminArea(Platform p, InputReader in) { this.p = p; this.in = in; this.out = in.out(); }

    void enter() {
        while (true) {
            out.println();
            out.println("--- Admin & Reports ---");
            out.println(" 1. Add restaurant");
            out.println(" 2. Remove restaurant");
            out.println(" 3. Create promotion");
            out.println(" 4. List promotions");
            out.println(" 5. Reports");
            out.println(" 6. Platform-wide statistics");
            out.println(" 7. Dispatch queue");
            out.println(" 8. Audit log (last 15 entries)");
            out.println(" 9. Rush-hour concurrency check");
            out.println(" 0. Back");
            int n = in.choice("Choose: ", 0, 9);
            if (n == 0) return;
            switch (n) {
                case 1 -> ConsoleApp.attempt(out, this::addRestaurant);
                case 2 -> ConsoleApp.attempt(out, () -> {
                    Optional<Restaurant> r = in.pick("Remove which restaurant?", p.catalog().restaurantsByRating(), Fmt::restaurant);
                    if (r.isPresent() && in.yesNo("Really remove " + r.get().name() + "?")) {
                        p.catalog().removeRestaurant(r.get().id());
                        out.println("  OK: Removed.");
                    }
                });
                case 3 -> ConsoleApp.attempt(out, this::createPromotion);
                case 4 -> p.catalog().promotions().forEach(pr -> out.println("  " + pr.describe()));
                case 5 -> reports();
                case 6 -> stats();
                case 7 -> {
                    var queue = p.dispatch().queue().inDispatchOrder();
                    if (queue.isEmpty()) out.println("  Dispatch queue is empty.");
                    queue.forEach(o -> out.println("  " + o.id() + "  " + o.customer().tier() + "  ready at "
                            + o.timeOf(OrderStatus.READY).map(t -> t.format(Fmt.WHEN)).orElse("?")
                            + "  " + o.restaurant().name() + " -> " + o.deliveryAddress().district()));
                }
                case 8 -> {
                    List<String> log = p.auditLog().entries();
                    log.subList(Math.max(0, log.size() - 15), log.size()).forEach(l -> out.println("  " + l));
                }
                case 9 -> rushHour();
                default -> throw new AssertionError();
            }
        }
    }

    private void addRestaurant() throws PlatformException {
        String id = in.text("Restaurant id: ");
        String name = in.text("Name: ");
        Optional<District> d = in.pick("District:", List.of(District.values()), District::displayName);
        if (d.isEmpty()) return;
        Set<Cuisine> cuisines = EnumSet.noneOf(Cuisine.class);
        while (cuisines.isEmpty()) {
            for (String s : in.text("Cuisines, comma separated " + Arrays.toString(Cuisine.values()) + ": ").split(",")) {
                try { cuisines.add(Cuisine.parse(s)); }
                catch (IllegalArgumentException e) { out.println("  (skipped: " + e.getMessage() + ")"); }
            }
        }
        double rating = in.rating("Rating (0.0-5.0): ");
        Restaurant r = new Restaurant(id, name, d.get(), cuisines, rating, in.yesNo("Open now?"));
        p.catalog().addRestaurant(r);
        out.println("  OK: Added " + r.name() + ". Add menu items from the Restaurant area.");
    }

    private void createPromotion() throws PlatformException {
        String code = in.text("Code: ");
        out.println(" 1. Percentage off (with cap)   2. Fixed amount off   3. Free delivery");
        int type = in.choice("Type: ", 1, 3);
        List<PromotionCondition> conditions = new ArrayList<>();
        while (true) {
            out.println("Add a condition: 1. Minimum subtotal  2. Expiry date  3. One district  4. First-time customers  0. Done");
            int c = in.choice("Choose: ", 0, 4);
            if (c == 0) break;
            switch (c) {
                case 1 -> conditions.add(new MinimumSubtotalCondition(in.money("Minimum subtotal (EGP): ")));
                case 2 -> {
                    LocalDate day = in.date("Last valid day");
                    if (day.isBefore(LocalDate.now(p.clock()))) out.println("  (note: that date is already past)");
                    conditions.add(new ExpiryCondition(day));
                }
                case 3 -> in.pick("District:", List.of(District.values()), District::displayName)
                        .ifPresent(d -> conditions.add(new DistrictCondition(d)));
                case 4 -> conditions.add(new FirstTimeCustomerCondition());
                default -> { }
            }
        }
        Promotion promo = switch (type) {   // the only place that chooses a promotion class: its creation screen
            case 1 -> new PercentageOffPromotion(code, in.positiveDecimal("Percent off: "), in.money("Maximum discount (EGP): "), conditions);
            case 2 -> new FixedAmountPromotion(code, in.money("Amount off (EGP): "), conditions);
            default -> new FreeDeliveryPromotion(code, conditions);
        };
        p.catalog().addPromotion(promo);
        out.println("  OK: Created " + promo.describe());
    }

    private void reports() {
        ReportService rs = p.reports();
        while (true) {
            out.println();
            out.println("--- Reports ---");
            out.println(" 1. Total revenue for a date range");
            out.println(" 2. Top five restaurants by revenue for a month");
            out.println(" 3. Average order value per district");
            out.println(" 4. Restaurants rated above 4.5 with 20+ completed orders");
            out.println(" 5. Orders by current status");
            out.println(" 6. Rider deliveries and average delivery time");
            out.println(" 7. Most frequently ordered item");
            out.println(" 8. A customer's order history and total spent");
            out.println(" 9. Peak ordering hour");
            out.println("10. Customers inactive for 30 days");
            out.println("11. Custom restaurant search (combine criteria)");
            out.println(" 0. Back");
            int n = in.choice("Choose: ", 0, 11);
            if (n == 0) return;
            switch (n) {
                case 1 -> ConsoleApp.attempt(out, () -> {
                    LocalDate from = in.date("From"), to = in.date("To");
                    out.println("  Revenue " + from + " .. " + to + ": " + rs.revenue(from, to));
                });
                case 2 -> {
                    YearMonth m = in.yearMonth("Month");
                    var top = rs.topRestaurantsByRevenue(m);
                    if (top.isEmpty()) out.println("  No delivered orders in " + m + ".");
                    for (int i = 0; i < top.size(); i++)
                        out.printf("  %d. %-20s %14s  (%d orders)%n", i + 1, top.get(i).restaurant().name(),
                                top.get(i).revenue(), top.get(i).deliveredOrders());
                }
                case 3 -> {
                    var avg = rs.averageOrderValueByDistrict();
                    if (avg.isEmpty()) out.println("  No delivered orders yet.");
                    avg.forEach((d, v) -> out.printf("  %-12s %14s%n", d, v));
                }
                case 4 -> {
                    var list = rs.highlyRatedBusyRestaurants();
                    if (list.isEmpty()) out.println("  None qualify yet.");
                    list.forEach(r -> out.println("  " + Fmt.restaurant(r)));
                }
                case 5 -> rs.countByStatus().forEach((s, c) -> out.printf("  %-17s %d%n", s, c));
                case 6 -> rs.riderStatistics().forEach(s -> out.printf("  %-10s %-10s %3d deliveries  avg %s%n",
                        s.rider().name(), s.rider().vehicle().displayName(), s.deliveries(),
                        s.averageDeliveryDuration().map(Fmt::duration).orElse("n/a")));
                case 7 -> out.println(rs.mostFrequentlyOrderedItem()
                        .map(x -> "  " + x.item().name() + " from " + x.restaurantName() + ", ordered " + x.timesOrdered() + " times")
                        .orElse("  No orders exist yet, so there is no most-ordered item."));
                case 8 -> in.pick("Customer:", p.customers().all().stream().sorted(Comparator.comparing(Customer::name)).toList(),
                        Customer::toString).ifPresent(c -> {
                    var h = rs.history(c);
                    h.ordersNewestFirst().forEach(o -> out.println("  " + Fmt.order(o)));
                    out.println("  Total spent: " + h.totalSpent());
                });
                case 9 -> out.println(rs.peakOrderingHour()
                        .map(h -> String.format("  Peak hour: %02d:00-%02d:59 with %d orders", h.hour(), h.hour(), h.orders()))
                        .orElse("  No orders yet."));
                case 10 -> {
                    var list = rs.inactiveCustomers(30);
                    if (list.isEmpty()) out.println("  Everyone has ordered in the last 30 days.");
                    list.forEach(c -> out.println("  " + c + "  last order: "
                            + rs.lastOrderAt(c).map(t -> t.format(Fmt.WHEN)).orElse("never")));
                }
                case 11 -> customSearch();
                default -> throw new AssertionError();
            }
        }
    }

    private void customSearch() {
        Predicate<Restaurant> filter = RestaurantFilters.any();
        List<String> used = new ArrayList<>();
        while (true) {
            out.println("Criteria so far: " + (used.isEmpty() ? "(none)" : String.join(" AND ", used)));
            out.println(" 1. District  2. Cuisine  3. Minimum rating  4. Dish at/under price  5. Open only  0. Run search");
            int c = in.choice("Add: ", 0, 5);
            if (c == 0) break;
            switch (c) {
                case 1 -> { var d = in.pick("District:", List.of(District.values()), District::displayName);
                    if (d.isPresent()) { filter = filter.and(RestaurantFilters.inDistrict(d.get())); used.add("district=" + d.get()); } }
                case 2 -> { var cu = in.pick("Cuisine:", List.copyOf(p.catalog().distinctCuisines()), Cuisine::displayName);
                    if (cu.isPresent()) { filter = filter.and(RestaurantFilters.servesCuisine(cu.get())); used.add("cuisine=" + cu.get()); } }
                case 3 -> { double r = in.rating("Minimum rating: "); filter = filter.and(RestaurantFilters.minRating(r)); used.add("rating>=" + r); }
                case 4 -> { Money m = in.money("Price ceiling (EGP): "); filter = filter.and(RestaurantFilters.hasItemAtOrBelow(m)); used.add("dish<=" + m); }
                case 5 -> { filter = filter.and(RestaurantFilters.open()); used.add("open"); }
                default -> { }
            }
        }
        var result = p.catalog().search(filter);
        if (result.isEmpty()) out.println("  No restaurants match.");
        result.forEach(r -> out.println("  " + Fmt.restaurant(r)));
    }

    private void stats() {
        var s = p.reports().snapshot(p.dispatch().queue().size());
        out.println("  Restaurants: " + s.restaurants() + "   Customers: " + s.customers()
                + "   Riders: " + s.riders() + " (" + s.ridersOnDuty() + " on duty)");
        out.println("  Orders: " + s.ordersTotal() + " total, " + s.ordersActive() + " active, "
                + s.readyAwaitingRider() + " waiting for a rider");
        out.println("  Delivered revenue: " + s.deliveredRevenue() + "   Average order: "
                + s.averageOrderValue().map(Money::toString).orElse("n/a"));
        out.println("  Distinct cuisines: " + p.catalog().distinctCuisines());
        out.println("  Live counters (event listener): placed " + p.statistics().ordersPlaced() + ", delivered "
                + p.statistics().ordersDelivered() + ", cancelled " + p.statistics().ordersCancelled());
        if (!p.events().listenerFailures().isEmpty()) out.println("  Listener failures: " + p.events().listenerFailures());
    }

    private void rushHour() {
        out.println("  Running on an isolated copy with a pool of " + p.config().workerPoolSize() + " threads...");
        try {
            var r = RushHourSimulation.run(p.config(), 60, 25, 5);
            out.printf("  %d customers raced for %d meals: %d placed, %d refused for stock, stock left %s%n",
                    r.customers(), r.stock(), r.placed(), r.shortages(), r.stockLeft().toPlainString());
            out.printf("  %d delivered; riders' counters total %d; wallets consistent: %s%n",
                    r.delivered(), r.riderDeliveriesTotal(), r.walletMismatches() == 0);
            out.printf("  One-active-order invariant: %d consecutive assignments checked, %d overlaps%n", r.assignmentsChecked(), r.invariantViolations());
            out.println("  " + (r.allChecksPassed() ? "ALL CHECKS PASSED" : "CHECK FAILED") + " in " + r.elapsed().toMillis() + " ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            out.println("  ERROR: Simulation error: " + e.getMessage());
        }
    }
}
