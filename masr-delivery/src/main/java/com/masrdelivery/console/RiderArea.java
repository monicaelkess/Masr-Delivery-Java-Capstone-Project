package com.masrdelivery.console;

import com.masrdelivery.dispatch.Rider;
import com.masrdelivery.order.Order;
import com.masrdelivery.report.ReportService;
import com.masrdelivery.service.Platform;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.Optional;

final class RiderArea {
    private final Platform p;
    private final InputReader in;
    private final PrintStream out;

    RiderArea(Platform p, InputReader in) { this.p = p; this.in = in; this.out = in.out(); }

    void enter() {
        Optional<Rider> pick = in.pick("Which rider?", p.riders().all().stream().sorted(Comparator.comparing(Rider::id)).toList(),
                r -> String.format("%-10s %-10s %-11s %s", r.name(), r.vehicle().displayName(), r.currentDistrict(),
                        r.isOnDuty() ? (r.isAvailable() ? "on duty, free" : "on duty, busy") : "off duty"));
        if (pick.isEmpty()) return;
        Rider r = pick.get();
        while (true) {
            out.println();
            out.println("--- Rider: " + r + " | " + (r.isOnDuty() ? "ON duty" : "OFF duty") + " ---");
            out.println(" 1. Go on duty");
            out.println(" 2. Go off duty");
            out.println(" 3. View current order");
            out.println(" 4. Mark picked up");
            out.println(" 5. Mark delivered");
            out.println(" 6. My statistics");
            out.println(" 0. Back");
            int n = in.choice("Choose: ", 0, 6);
            if (n == 0) return;
            switch (n) {
                case 1 -> {
                    var assigned = p.orderService().goOnDuty(r);
                    out.println("  OK: On duty.");
                    assigned.stream().filter(a -> a.rider().equals(r))
                            .forEach(a -> out.println("  -> You have been assigned " + a.order().id()));
                }
                case 2 -> ConsoleApp.attempt(out, () -> { p.orderService().goOffDuty(r); out.println("  OK: Off duty."); });
                case 3 -> out.println("  " + p.riderDashboard().cardFor(r.id()).orElse("No assigned order."));
                case 4 -> ConsoleApp.attempt(out, () -> {
                    Order o = p.orderService().pickUp(r);
                    out.println("  OK: " + o.id() + " picked up. Deliver to " + o.deliveryAddress()
                            + " (~" + r.vehicle().estimatedTravelTime(p.config().distances()
                            .between(o.restaurant().district(), o.deliveryAddress().district())).toMinutes() + " min).");
                });
                case 5 -> ConsoleApp.attempt(out, () -> {
                    Order o = p.orderService().deliver(r);
                    out.println("  OK: " + o.id() + " delivered. Total deliveries: " + r.completedDeliveries());
                });
                case 6 -> {
                    ReportService.RiderStats s = p.reports().riderStatistics().stream()
                            .filter(x -> x.rider().equals(r)).findFirst().orElseThrow();
                    out.println("  Deliveries: " + s.deliveries() + ", average delivery time: "
                            + s.averageDeliveryDuration().map(Fmt::duration).orElse("n/a (no deliveries yet)"));
                }
                default -> throw new AssertionError();
            }
        }
    }
}
