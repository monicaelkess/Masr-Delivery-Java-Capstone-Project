package com.masrdelivery.console;

import com.masrdelivery.exception.PlatformException;
import com.masrdelivery.menu.MenuItem;
import com.masrdelivery.menu.QuantityUnit;
import com.masrdelivery.model.*;
import com.masrdelivery.money.Money;
import com.masrdelivery.order.Order;
import com.masrdelivery.order.OrderRequest;
import com.masrdelivery.order.OrderStatus;
import com.masrdelivery.pricing.PriceBreakdown;
import com.masrdelivery.report.ReportService;
import com.masrdelivery.service.OrderService;
import com.masrdelivery.service.Platform;
import com.masrdelivery.service.RestaurantFilters;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class CustomerArea {
    private final Platform p;
    private final InputReader in;
    private final PrintStream out;

    CustomerArea(Platform p, InputReader in) { this.p = p; this.in = in; this.out = in.out(); }

    void enter() {
        Optional<Customer> who = chooseCustomer();
        if (who.isEmpty()) return;
        Customer c = who.get();
        while (true) {
            out.println();
            out.printf("--- Customer: %s | %s tier (%d completed) | wallet %s ---%n",
                    c.name(), c.tier(), c.completedOrders(), c.walletBalance());
            out.println(" 1. Browse restaurants");
            out.println(" 2. Search");
            out.println(" 3. View menu");
            out.println(" 4. Place order");
            out.println(" 5. Pay from wallet");
            out.println(" 6. Track order");
            out.println(" 7. Cancel order");
            out.println(" 8. Order history");
            out.println(" 9. Notifications");
            out.println("10. Addresses & wallet top-up");
            out.println(" 0. Back");
            int n = in.choice("Choose: ", 0, 10);
            if (n == 0) return;
            switch (n) {
                case 1 -> browse();
                case 2 -> search(c);
                case 3 -> viewMenu();
                case 4 -> ConsoleApp.attempt(out, () -> placeOrder(c));
                case 5 -> ConsoleApp.attempt(out, () -> pay(c));
                case 6 -> ConsoleApp.attempt(out, () -> track(c));
                case 7 -> ConsoleApp.attempt(out, () -> cancel(c));
                case 8 -> history(c);
                case 9 -> p.notifier().inbox(c).forEach(m -> out.println("  " + m));
                case 10 -> addressesAndWallet(c);
                default -> throw new AssertionError();
            }
        }
    }

    private Optional<Customer> chooseCustomer() {
        List<Customer> all = p.customers().all().stream().sorted(Comparator.comparing(Customer::id)).toList();
        out.println();
        out.println("Who is ordering?");
        for (int i = 0; i < all.size(); i++)
            out.printf("  %2d. %-16s %s  %s%n", i + 1, all.get(i).name(), all.get(i).mobile(), all.get(i).tier());
        out.printf("  %2d. Register a new customer%n", all.size() + 1);
        out.println("   0. Back");
        int n = in.choice("Choose: ", 0, all.size() + 1);
        if (n == 0) return Optional.empty();
        if (n <= all.size()) return Optional.of(all.get(n - 1));
        return register();
    }

    private Optional<Customer> register() {
        while (true) {
            try {
                String id = in.text("Customer id (e.g. C9): ");
                String name = in.text("Name: ");
                String mobile = in.text("Mobile (11 digits, 010/011/012/015): ");
                District d = chooseDistrict().orElse(null);
                if (d == null) return Optional.empty();
                String detail = in.text("Address detail (street, building, floor): ");
                Customer c = new Customer(id, name, mobile, new Address(d, detail), Money.ZERO);
                p.customers().add(c);
                out.println("  OK: Welcome, " + name + "!");
                return Optional.of(c);
            } catch (PlatformException | IllegalArgumentException e) {
                out.println("  ERROR: " + e.getMessage());
                if (!in.yesNo("Try again?")) return Optional.empty();
            }
        }
    }

    private Optional<District> chooseDistrict() {
        return in.pick("District:", List.of(District.values()), District::displayName);
    }

    private void browse() {
        Predicate<Restaurant> filter = RestaurantFilters.any();
        out.println("Filters (press Enter to skip any):");
        Optional<String> d = in.optionalText("  District [" + Arrays.stream(District.values())
                .map(District::displayName).collect(Collectors.joining(", ")) + "]: ");
        if (d.isPresent()) {
            try { filter = filter.and(RestaurantFilters.inDistrict(District.parse(d.get()))); }
            catch (IllegalArgumentException e) { out.println("  (ignored: " + e.getMessage() + ")"); }
        }
        Optional<String> cu = in.optionalText("  Cuisine " + p.catalog().distinctCuisines() + ": ");
        if (cu.isPresent()) {
            try { filter = filter.and(RestaurantFilters.servesCuisine(Cuisine.parse(cu.get()))); }
            catch (IllegalArgumentException e) { out.println("  (ignored: " + e.getMessage() + ")"); }
        }
        Optional<String> r = in.optionalText("  Minimum rating (0-5): ");
        if (r.isPresent()) {
            try {
                double min = Double.parseDouble(r.get());
                if (min < 0 || min > 5) throw new NumberFormatException();
                filter = filter.and(RestaurantFilters.minRating(min));
            } catch (NumberFormatException e) { out.println("  (ignored: rating must be 0-5)"); }
        }
        Optional<Money> ceiling = in.optionalMoney("  Has a dish at or under (EGP): ");
        if (ceiling.isPresent()) filter = filter.and(RestaurantFilters.hasItemAtOrBelow(ceiling.get()));

        List<Restaurant> list = p.catalog().browse(filter);
        if (list.isEmpty()) { out.println("  No open restaurants match those filters."); return; }
        for (int i = 0; i < list.size(); i++) out.printf("  %2d. %s%n", i + 1, Fmt.restaurant(list.get(i)));
    }

    private void search(Customer c) {
        if (!c.recentSearches().isEmpty()) out.println("  Recent searches: " + c.recentSearches());
        String text = in.text("Search restaurants by name or cuisine: ");
        List<Restaurant> found = p.catalog().searchText(c, text);
        if (found.isEmpty()) { out.println("  Nothing found for \"" + text + "\"."); return; }
        found.forEach(r -> out.println("  - " + Fmt.restaurant(r)));
    }

    private void viewMenu() {
        in.pick("Restaurant:", p.catalog().restaurantsByRating(), Fmt::restaurant).ifPresent(this::printMenu);
    }

    void printMenu(Restaurant r) {
        out.println("=== " + r.name() + " (" + r.district() + ")" + (r.isOpen() ? "" : " - CLOSED") + " ===");
        // groupingBy into a LinkedHashMap keeps categories and items in insertion order
        Map<String, List<MenuItem>> byCategory = r.menu().stream()
                .collect(Collectors.groupingBy(MenuItem::category, LinkedHashMap::new, Collectors.toList()));
        byCategory.forEach((cat, items) -> {
            out.println("  " + cat);
            items.forEach(i -> out.println("    " + i.id() + "  " + Fmt.item(i)));
        });
    }

    private void placeOrder(Customer c) throws PlatformException {
        Optional<Restaurant> pick = in.pick("Order from (open restaurants):", p.catalog().browse(RestaurantFilters.any()), Fmt::restaurant);
        if (pick.isEmpty()) return;
        Restaurant r = pick.get();
        OrderRequest.Builder b = OrderRequest.builder().customer(c).restaurant(r);

        while (true) {
            List<MenuItem> menu = r.menu();
            out.println("Add items (" + b.lineCount() + " in basket):");
            for (int i = 0; i < menu.size(); i++) out.printf("  %2d. %s%n", i + 1, Fmt.item(menu.get(i)));
            out.println("   0. Done adding");
            int n = in.choice("Item: ", 0, menu.size());
            if (n == 0) break;
            MenuItem item = menu.get(n - 1);
            BigDecimal qty = item.unit() == QuantityUnit.KILOGRAM
                    ? in.positiveDecimal("  Weight in kg (e.g. 0.75): ")
                    : BigDecimal.valueOf(in.choice("  Quantity: ", 1, 999));
            try { b.add(item, qty); out.println("  + added"); }
            catch (IllegalArgumentException e) { out.println("  ERROR: " + e.getMessage()); }
        }

        Optional<Address> address = in.pick("Deliver to:", c.addresses(), Address::toString);
        if (address.isEmpty()) return;
        b.deliverTo(address.get());
        in.optionalText("Promotion code (Enter for none): ").ifPresent(b::promotionCode);
        in.optionalText("Delivery notes (Enter for none): ").ifPresent(b::notes);

        OrderRequest request = b.build();                         // empty basket rejected here
        PriceBreakdown quote = p.orderService().quote(request);    // every rule checked, nothing committed
        out.println("Order summary:");
        request.lines().forEach(l -> out.println("  " + l));
        out.print(quote.format());
        if (!in.yesNo("Place this order?")) { out.println("  Order discarded."); return; }
        Order o = p.orderService().placeOrder(request);
        out.println("  OK: Order " + o.id() + " placed. Total " + o.price().total() + ". Pay from wallet to send it to the kitchen.");
        if (!c.walletBalance().isLessThan(o.price().total()) && in.yesNo("Pay now from wallet?")) {
            out.println("  OK: Paid. New balance " + p.orderService().pay(c, o.id()));
        }
    }

    private String askOrder(Customer c, String prompt, Predicate<Order> which) {
        List<Order> candidates = p.reports().history(c).ordersNewestFirst().stream().filter(which).toList();
        if (!candidates.isEmpty()) candidates.forEach(o -> out.println("  " + Fmt.order(o)));
        return in.text(prompt).toUpperCase();
    }

    private void pay(Customer c) throws PlatformException {
        String id = askOrder(c, "Order reference to pay: ", o -> !o.isPaid() && o.status() != OrderStatus.CANCELLED);
        Money balance = p.orderService().pay(c, id);
        out.println("  OK: Paid. New wallet balance: " + balance);
    }

    private void track(Customer c) throws PlatformException {
        String id = askOrder(c, "Order reference: ", o -> !o.status().isTerminal());
        OrderService.Tracking t = p.orderService().track(c, id);
        out.println("  " + t.order().id() + " is " + t.status() + " - placed " + Fmt.duration(t.elapsed()) + " ago.");
        t.order().timeline().forEach((s, at) -> out.println("     " + at.format(Fmt.WHEN) + "  " + s));
    }

    private void cancel(Customer c) throws PlatformException {
        String id = askOrder(c, "Order reference to cancel: ", o -> !o.status().isTerminal());
        Money refund = p.orderService().cancelByCustomer(c, id);
        out.println("  OK: Order " + id + " cancelled." + (refund.isPositive()
                ? " Refunded " + refund + " to your wallet (balance " + c.walletBalance() + ")." : ""));
    }

    private void history(Customer c) {
        ReportService.CustomerHistory h = p.reports().history(c);
        if (h.ordersNewestFirst().isEmpty()) out.println("  No orders yet.");
        h.ordersNewestFirst().forEach(o -> out.println("  " + Fmt.order(o)));
        out.println("  Lifetime total spent: " + h.totalSpent());
    }

    private void addressesAndWallet(Customer c) {
        c.addresses().forEach(a -> out.println("  - " + a));
        out.println(" 1. Add address   2. Top up wallet   0. Back");
        switch (in.choice("Choose: ", 0, 2)) {
            case 1 -> chooseDistrict().ifPresent(d -> {
                try {
                    boolean added = c.addAddress(new Address(d, in.text("Detail: ")));
                    out.println(added ? "  OK: Address saved." : "  That address is already saved.");
                } catch (IllegalArgumentException e) { out.println("  ERROR: " + e.getMessage()); }
            });
            case 2 -> { c.credit(in.money("Amount (EGP): ")); out.println("  OK: Balance: " + c.walletBalance()); }
            default -> { }
        }
    }
}
