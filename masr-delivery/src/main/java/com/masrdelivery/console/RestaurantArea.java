package com.masrdelivery.console;

import com.masrdelivery.dispatch.DispatchService;
import com.masrdelivery.exception.PlatformException;
import com.masrdelivery.menu.CsvRow;
import com.masrdelivery.menu.MenuItem;
import com.masrdelivery.model.Restaurant;
import com.masrdelivery.money.Money;
import com.masrdelivery.order.Order;
import com.masrdelivery.order.OrderStatus;
import com.masrdelivery.report.ReportService;
import com.masrdelivery.service.Platform;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class RestaurantArea {
    private final Platform p;
    private final InputReader in;
    private final PrintStream out;

    RestaurantArea(Platform p, InputReader in) { this.p = p; this.in = in; this.out = in.out(); }

    void enter() {
        Optional<Restaurant> pick = in.pick("Which restaurant?", p.catalog().restaurantsByRating(), Fmt::restaurant);
        if (pick.isEmpty()) return;
        Restaurant r = pick.get();
        while (true) {
            out.println();
            out.println("--- Restaurant: " + r.name() + (r.isOpen() ? " (open)" : " (closed)") + " ---");
            out.println(" 1. Accept or reject a pending order");
            out.println(" 2. Mark an order preparing");
            out.println(" 3. Mark an order ready");
            out.println(" 4. Toggle item availability");
            out.println(" 5. Add menu item");
            out.println(" 6. Remove menu item");
            out.println(" 7. Adjust daily stock");
            out.println(" 8. Open / close restaurant");
            out.println(" 9. Today's orders and revenue");
            out.println("10. View menu");
            out.println(" 0. Back");
            int n = in.choice("Choose: ", 0, 10);
            if (n == 0) return;
            switch (n) {
                case 1 -> ConsoleApp.attempt(out, () -> acceptOrReject(r));
                case 2 -> ConsoleApp.attempt(out, () -> pickOrder(r, OrderStatus.ACCEPTED, "Start preparing:")
                        .ifPresent(o -> run(() -> { p.orderService().startPreparing(r, o.id()); out.println("  OK: " + o.id() + " PREPARING"); })));
                case 3 -> ConsoleApp.attempt(out, () -> pickOrder(r, OrderStatus.PREPARING, "Mark ready:")
                        .ifPresent(o -> run(() -> {
                            List<DispatchService.Assignment> a = p.orderService().markReady(r, o.id());
                            out.println("  OK: " + o.id() + " READY");
                            a.forEach(x -> out.println("  -> " + x.order().id() + " assigned to " + x.rider().name()));
                            if (o.status() == OrderStatus.READY) out.println("  (waiting in dispatch queue for a free rider)");
                        })));
                case 4 -> toggle(r);
                case 5 -> ConsoleApp.attempt(out, () -> addItem(r));
                case 6 -> ConsoleApp.attempt(out, () -> {
                    Optional<MenuItem> i = in.pick("Remove which item?", r.menu(), Fmt::item);
                    if (i.isPresent()) { r.removeMenuItem(i.get().id()); out.println("  OK: Removed " + i.get().name()); }
                });
                case 7 -> ConsoleApp.attempt(out, () -> {
                    Optional<MenuItem> i = in.pick("Adjust stock for:", r.menu(),
                            x -> x.name() + "  (left: " + x.unit().format(r.stockOf(x.id())) + ")");
                    if (i.isPresent()) {
                        r.setStock(i.get().id(), in.nonNegativeDecimal("New stock for today: "));
                        out.println("  OK: Stock updated.");
                    }
                });
                case 8 -> { r.setOpen(!r.isOpen()); out.println("  OK: Now " + (r.isOpen() ? "OPEN" : "CLOSED")); }
                case 9 -> today(r);
                case 10 -> new CustomerArea(p, in).printMenu(r);
                default -> throw new AssertionError();
            }
        }
    }

    private void run(ConsoleApp.Action a) { ConsoleApp.attempt(out, a); }

    private Optional<Order> pickOrder(Restaurant r, OrderStatus status, String title) {
        List<Order> list = p.reports().ordersFor(r, status);
        if (list.isEmpty()) { out.println("  No orders in status " + status + "."); return Optional.empty(); }
        return in.pick(title, list, Fmt::order);
    }

    private void acceptOrReject(Restaurant r) throws PlatformException {
        Optional<Order> o = pickOrder(r, OrderStatus.PLACED, "Pending orders:");
        if (o.isEmpty()) return;
        o.get().lines().forEach(l -> out.println("    " + l));
        o.get().notes().ifPresent(n -> out.println("    Notes: " + n));
        out.println(" 1. Accept   2. Reject   0. Back");
        switch (in.choice("Choose: ", 0, 2)) {
            case 1 -> { p.orderService().accept(r, o.get().id()); out.println("  OK: Accepted " + o.get().id()); }
            case 2 -> {
                Money refund = p.orderService().reject(r, o.get().id());
                out.println("  OK: Rejected " + o.get().id() + (refund.isPositive() ? "; " + refund + " refunded to customer." : "."));
            }
            default -> { }
        }
    }

    private void toggle(Restaurant r) {
        in.pick("Toggle which item?", r.menu(), Fmt::item).ifPresent(i -> {
            i.setAvailable(!i.isAvailable());
            out.println("  OK: " + i.name() + " is now " + (i.isAvailable() ? "available" : "unavailable"));
        });
    }

    /** Collects generic fields; the factory alone decides which class to build. */
    private void addItem(Restaurant r) throws PlatformException {
        out.println("Types: " + p.menuItemFactory().supportedTypes());
        Map<String, String> f = new HashMap<>();
        f.put("type", in.text("Type: "));
        f.put("item_id", in.text("Item id (unique in this restaurant): "));
        f.put("name", in.text("Name: "));
        f.put("category", in.text("Category: "));
        f.put("prep_minutes", String.valueOf(in.choice("Preparation minutes: ", 1, 600)));
        f.put("available", "true");
        in.optionalText("Price in EGP (per kg for weighted; blank for combo): ").ifPresent(v -> f.put("price", v));
        in.optionalText("Combo spec ITEM+ITEM@DISCOUNT% (blank otherwise): ").ifPresent(v -> f.put("extra", v));
        BigDecimal stock = in.nonNegativeDecimal("Daily stock: ");
        MenuItem item = p.menuItemFactory().create(new CsvRow(0, f), r);
        r.addMenuItem(item, stock);
        out.println("  OK: Added " + Fmt.item(item));
    }

    private void today(Restaurant r) {
        ReportService.RestaurantDay d = p.reports().today(r);
        if (d.orders().isEmpty()) out.println("  No orders today.");
        d.orders().forEach(o -> out.println("  " + Fmt.order(o)));
        out.println("  Delivered revenue today: " + d.deliveredRevenue());
        out.println("  Value of open orders:    " + d.openOrderValue());
    }
}
