package com.masrdelivery.console;

import com.masrdelivery.dispatch.Rider;
import com.masrdelivery.exception.PlatformException;
import com.masrdelivery.menu.MenuItem;
import com.masrdelivery.menu.QuantityUnit;
import com.masrdelivery.model.*;
import com.masrdelivery.money.Money;
import com.masrdelivery.order.Order;
import com.masrdelivery.order.OrderRequest;
import com.masrdelivery.promotion.*;
import com.masrdelivery.service.Platform;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;


public final class DataSeeder {
    private DataSeeder() {}

    public static void seed(Platform p) throws PlatformException {
        restaurants(p);
        menu(p);
        customers(p);
        riders(p);
        promotions(p);
        history(p);
    }

    private static void restaurants(Platform p) throws PlatformException {
        add(p, "R1", "Bait El Koshary", District.DOKKI, 4.7, Cuisine.KOSHARY, Cuisine.EGYPTIAN);
        add(p, "R2", "Nile Grill House", District.MAADI, 4.8, Cuisine.GRILL, Cuisine.EGYPTIAN);
        add(p, "R3", "Sham Shawarma", District.NASR_CITY, 4.5, Cuisine.SYRIAN);
        add(p, "R4", "Pizza Roma", District.HELIOPOLIS, 4.2, Cuisine.PIZZA);
        add(p, "R5", "Sakura Sushi", District.ZAMALEK, 4.6, Cuisine.ASIAN, Cuisine.SEAFOOD);
        add(p, "R6", "Halawany Sweets", District.MOHANDESSIN, 4.7, Cuisine.DESSERTS, Cuisine.CAFE);
    }

    private static void add(Platform p, String id, String name, District d, double rating, Cuisine first, Cuisine... more)
            throws PlatformException {
        p.catalog().addRestaurant(new Restaurant(id, name, d, EnumSet.of(first, more), rating, true));
    }

    private static void menu(Platform p) throws PlatformException {
        try (BufferedReader reader = openMenu(p)) {
            p.menuItemFactory().load(reader, p.restaurants()::find);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BufferedReader openMenu(Platform p) throws IOException {
        if (Files.isRegularFile(p.config().menuFile())) return Files.newBufferedReader(p.config().menuFile());
        InputStream in = DataSeeder.class.getResourceAsStream("menu.csv");
        if (in == null) throw new FileNotFoundException("Menu file not found: "
                + p.config().menuFile().toAbsolutePath()
                + " (and no /menu.csv on the classpath)");
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private static void customers(Platform p) throws PlatformException {
        p.customers().add(new Customer("C1", "Mariam Adel", "01012345678", new Address(District.FAISAL, "14 Mariouteya St, floor 3"), Money.of(900)));
        p.customers().add(new Customer("C2", "Youssef Hany", "01198765432", new Address(District.MAADI, "Road 9, bldg 21"), Money.of(600)));
        p.customers().add(new Customer("C3", "Nour Samir", "01234567890", new Address(District.NASR_CITY, "Abbas El Akkad, bldg 5"), Money.of(400)));
        p.customers().add(new Customer("C4", "Karim Fawzy", "01555501234", new Address(District.HELIOPOLIS, "Baghdad St 7"), Money.of(250)));
        p.customers().add(new Customer("C5", "Salma Tarek", "01011122233", new Address(District.FAISAL, "El Tawabek St 11, apt 8"), Money.of(300)));
        p.customers().get("C1").addAddress(new Address(District.DOKKI, "Office: Tahrir St 40"));
    }

    private static void riders(Platform p) throws PlatformException {
        rider(p, "D1", "Ahmed", "MOTORCYCLE", District.DOKKI);
        rider(p, "D2", "Mostafa", "MOTORCYCLE", District.MAADI);
        rider(p, "D3", "Hassan", "BICYCLE", District.ZAMALEK);
        rider(p, "D4", "Omar", "CAR", District.NASR_CITY);
        rider(p, "D5", "Mahmoud", "BICYCLE", District.DOKKI);
    }

    private static void rider(Platform p, String id, String name, String vehicle, District d) throws PlatformException {
        p.riders().add(new Rider(id, name, p.vehicles().find(vehicle).orElseThrow(), d));
    }

    private static void promotions(Platform p) throws PlatformException {
        LocalDate today = LocalDate.now(p.clock());
        p.catalog().addPromotion(new PercentageOffPromotion("NILE20", new BigDecimal("20"), Money.of(50),
                List.of(new ExpiryCondition(today.plusYears(1)))));
        p.catalog().addPromotion(new FixedAmountPromotion("WELCOME50", Money.of(50),
                List.of(new FirstTimeCustomerCondition(), new MinimumSubtotalCondition(Money.of(150)))));
        p.catalog().addPromotion(new FreeDeliveryPromotion("FREEDEL",
                List.of(new MinimumSubtotalCondition(Money.of(200)), new ExpiryCondition(today.plusMonths(3)))));
        p.catalog().addPromotion(new PercentageOffPromotion("MAADI15", new BigDecimal("15"), Money.of(40),
                List.of(new DistrictCondition(District.MAADI))));
        p.catalog().addPromotion(new FixedAmountPromotion("SUMMER30", Money.of(30),
                List.of(new ExpiryCondition(today.minusDays(20)))));
    }

       private static void history(Platform p) throws PlatformException {
        Random rnd = new Random(2026);
        Map<Restaurant, Map<String, BigDecimal>> dailyStock = new HashMap<>();
        for (Restaurant r : p.restaurants().all()) {
            Map<String, BigDecimal> s = new HashMap<>();
            r.menu().forEach(i -> s.put(i.id(), r.stockOf(i.id())));
            dailyStock.put(r, s);
        }
        p.riders().all().forEach(Rider::goOnDuty);
        LocalDateTime now = LocalDateTime.now(p.clock());
        Restaurant r1 = p.restaurants().get("R1"), r2 = p.restaurants().get("R2"), r3 = p.restaurants().get("R3"),
                r4 = p.restaurants().get("R4"), r5 = p.restaurants().get("R5"), r6 = p.restaurants().get("R6");
        Customer mariam = p.customers().get("C1"), youssef = p.customers().get("C2"),
                nour = p.customers().get("C3"), karim = p.customers().get("C4");
        int[] hours = {13, 14, 19, 20, 20, 21, 21, 22, 15, 20};

        // Mariam: 32 completed -> Gold. Youssef: 12 -> Silver. Nour: 3 -> Bronze. Karim: 2, both > 30 days ago.
        List<Object[]> plan = new ArrayList<>();
        for (int i = 0; i < 32; i++) plan.add(new Object[]{mariam, i % 3 == 2 ? r6 : r1, 58 - i});
        for (int i = 0; i < 12; i++) plan.add(new Object[]{youssef, i % 2 == 0 ? r2 : r1, 50 - 3 * i});
        plan.add(new Object[]{nour, r3, 20}); plan.add(new Object[]{nour, r4, 12}); plan.add(new Object[]{nour, r5, 6});
        plan.add(new Object[]{karim, r4, 45}); plan.add(new Object[]{karim, r4, 40});
        plan.sort(Comparator.comparingInt(o -> -(int) o[2]));   // oldest first, so tiers grow realistically

        int n = 0;
        for (Object[] step : plan) {
            Customer c = (Customer) step[0];
            Restaurant r = (Restaurant) step[1];
            LocalDateTime at = now.toLocalDate().minusDays((int) step[2])
                    .atTime(LocalTime.of(hours[n++ % hours.length], rnd.nextInt(60)));
            completeOrder(p, c, r, at, rnd);
        }
        // one cancelled historical order (Karim, 42 days ago)
        p.clock().pin(now.minusDays(42));
        Order cancelled = placePaid(p, karim, r4, rnd);
        p.orderService().cancelByCustomer(karim, cancelled.id());

        // restore today's stock after the historical replay
        for (var e : dailyStock.entrySet()) for (var s : e.getValue().entrySet()) e.getKey().setStock(s.getKey(), s.getValue());

        // live orders "now": riders go off duty first so a READY order waits in the queue
        p.clock().unpin();
        for (Rider rd : p.riders().all()) rd.goOffDuty();
        placePaidOrUnpaid(p, nour, r3, rnd, false);                          // PLACED, unpaid
        Order y = placePaid(p, youssef, r1, rnd);                            // ACCEPTED -> PREPARING
        p.orderService().accept(r1, y.id());
        p.orderService().startPreparing(r1, y.id());
        Order m = placePaid(p, mariam, r1, rnd);                             // READY, Gold, waiting for a rider
        p.orderService().accept(r1, m.id());
        p.orderService().startPreparing(r1, m.id());
        p.orderService().markReady(r1, m.id());
        r5.setOpen(false);                                                   // Sakura is closed today
    }

    private static void completeOrder(Platform p, Customer c, Restaurant r, LocalDateTime at, Random rnd) throws PlatformException {
        p.clock().pin(at);
        Order o = placePaid(p, c, r, rnd);
        p.clock().advance(Duration.ofMinutes(3));
        p.orderService().accept(r, o.id());
        p.orderService().startPreparing(r, o.id());
        p.clock().advance(Duration.ofMinutes(15 + rnd.nextInt(15)));
        p.orderService().markReady(r, o.id());
        Rider rider = p.riders().get(o.riderId().orElseThrow(() -> new IllegalStateException("seed: no rider for " + o.id())));
        p.clock().advance(Duration.ofMinutes(5 + rnd.nextInt(6)));
        p.orderService().pickUp(rider);
        p.clock().advance(Duration.ofMinutes(12 + rnd.nextInt(25)));
        p.orderService().deliver(rider);
    }

    private static Order placePaid(Platform p, Customer c, Restaurant r, Random rnd) throws PlatformException {
        return placePaidOrUnpaid(p, c, r, rnd, true);
    }

    private static Order placePaidOrUnpaid(Platform p, Customer c, Restaurant r, Random rnd, boolean pay) throws PlatformException {
        List<MenuItem> items = r.menu().stream().filter(MenuItem::isAvailable).toList();
        OrderRequest.Builder b = OrderRequest.builder().customer(c).restaurant(r).deliverTo(c.addresses().get(0));
        int lines = 1 + rnd.nextInt(2);
        for (int i = 0; i < lines; i++) {
            MenuItem item = items.get(rnd.nextInt(items.size()));
            b.add(item, item.unit() == QuantityUnit.KILOGRAM ? new BigDecimal("0.5") : BigDecimal.valueOf(1 + rnd.nextInt(2)));
        }
        Order o = p.orderService().placeOrder(b.build());
        if (pay) {
            c.credit(o.price().total());          // simulated top-up so the demo wallet ends where it started
            p.orderService().pay(c, o.id());
        }
        return o;
    }
}
