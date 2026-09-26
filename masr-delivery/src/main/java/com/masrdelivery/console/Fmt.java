package com.masrdelivery.console;

import com.masrdelivery.menu.MenuItem;
import com.masrdelivery.model.Cuisine;
import com.masrdelivery.model.Restaurant;
import com.masrdelivery.order.Order;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;


final class Fmt {
    private Fmt() {}

    static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    static String restaurant(Restaurant r) {
        return String.format("%-20s %-12s %-22s %.1f*%s", r.name(), r.district(),
                r.cuisines().stream().map(Cuisine::displayName).collect(Collectors.joining(", ")),
                r.rating(), r.isOpen() ? "" : "  (closed)");
    }

    static String item(MenuItem i) {
        return i.name() + " - " + i.priceLabel() + (i.isAvailable() ? "" : "  [UNAVAILABLE]");
    }

    static String order(Order o) {
        return String.format("%s  %s  %-18s %-16s %12s%s", o.id(), o.placedAt().format(WHEN), o.restaurant().name(),
                o.status(), o.price().total(), o.isRefunded() ? " (refunded)" : o.isPaid() ? " (paid)" : " (unpaid)");
    }

    static String duration(Duration d) {
        long h = d.toHours(), m = d.toMinutesPart();
        return h > 0 ? h + " h " + m + " min" : m + " min " + d.toSecondsPart() + " s";
    }
}
