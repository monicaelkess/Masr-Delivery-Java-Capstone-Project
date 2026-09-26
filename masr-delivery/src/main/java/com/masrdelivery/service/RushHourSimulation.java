package com.masrdelivery.service;

import com.masrdelivery.config.PlatformConfig;
import com.masrdelivery.dispatch.Motorcycle;
import com.masrdelivery.dispatch.Rider;
import com.masrdelivery.exception.PlatformException;
import com.masrdelivery.exception.StockShortageException;
import com.masrdelivery.menu.StandardItem;
import com.masrdelivery.model.*;
import com.masrdelivery.money.Money;
import com.masrdelivery.order.Order;
import com.masrdelivery.order.OrderRequest;
import com.masrdelivery.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public final class RushHourSimulation {

    public record Result(int customers, int stock, int placed, int shortages, BigDecimal stockLeft,
                         int delivered, int riderDeliveriesTotal, int assignmentsChecked, int invariantViolations,
                         long walletMismatches, Duration elapsed) {
        public boolean allChecksPassed() {
            return placed == stock && shortages == customers - stock && stockLeft.signum() == 0
                    && delivered == placed && riderDeliveriesTotal == placed && invariantViolations == 0
                    && walletMismatches == 0;
        }
    }

    public static Result run(PlatformConfig config, int customers, int stock, int riders) throws InterruptedException {
        long start = System.nanoTime();
        Platform p = new Platform(config, MutableClock.system(), null);
        try {
            Restaurant r = new Restaurant("RUSH", "Rush Kitchen", District.DOKKI, EnumSet.of(Cuisine.EGYPTIAN), 4.0, true);
            StandardItem item = new StandardItem("RUSH", "X", "Rush Meal", "Mains", 5, true, Money.of(50));
            r.addMenuItem(item, BigDecimal.valueOf(stock));
            p.restaurants().add(r);
            for (int i = 0; i < riders; i++) {
                Rider rider = new Rider("RR" + i, "Rider " + i, new Motorcycle(), District.DOKKI);
                p.riders().add(rider);
                rider.goOnDuty();
            }
            List<Customer> cs = new ArrayList<>();
            for (int i = 0; i < customers; i++) {
                Customer c = new Customer("RC" + i, "Customer " + i, String.format("0101%07d", i),
                        new Address(District.DOKKI, "Street " + i), Money.of(1000));
                p.customers().add(c);
                cs.add(c);
            }

            ExecutorService pool = Executors.newFixedThreadPool(config.workerPoolSize());
            AtomicInteger shortages = new AtomicInteger();
            ConcurrentLinkedQueue<Order> placed = new ConcurrentLinkedQueue<>();
            CountDownLatch go = new CountDownLatch(1);
            List<Future<?>> fs = new ArrayList<>();
            for (Customer c : cs) {
                fs.add(pool.submit(() -> {
                    go.await();
                    try {
                        Order o = p.orderService().placeOrder(OrderRequest.builder().customer(c).restaurant(r)
                                .deliverTo(c.addresses().get(0)).add(item, 1).build());
                        p.orderService().pay(c, o.id());
                        p.orderService().pay(c, o.id());          // double-pay must be refused
                    } catch (StockShortageException e) {
                        shortages.incrementAndGet();
                    } catch (PlatformException expectedSecondPay) {
                        // PaymentStateException from the second pay: correct
                    }
                    return null;
                }));
            }
            go.countDown();
            for (Future<?> f : fs) f.get();
            p.orders().all().forEach(placed::add);

            List<Future<?>> kitchen = new ArrayList<>();
            for (Order o : placed) {
                kitchen.add(pool.submit(() -> {
                    p.orderService().accept(r, o.id());
                    p.orderService().startPreparing(r, o.id());
                    p.orderService().markReady(r, o.id());
                    return null;
                }));
            }
            ExecutorService riderThreads = Executors.newFixedThreadPool(riders);
            List<Future<?>> riderWork = new ArrayList<>();
            for (Rider rider : p.riders().all()) {
                riderWork.add(riderThreads.submit(() -> {
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                    while (System.nanoTime() < deadline) {
                        long outstanding = p.orders().all().stream().filter(o -> o.status() != OrderStatus.DELIVERED).count();
                        if (outstanding == 0) break;
                        if (p.orderService().currentOrder(rider).isPresent()) {
                            p.orderService().pickUp(rider);
                            p.orderService().deliver(rider);
                        } else {
                            p.dispatch().dispatchPending();
                            Thread.onSpinWait();
                        }
                    }
                    return null;
                }));
            }
            for (Future<?> f : kitchen) f.get();
            for (Future<?> f : riderWork) f.get();
            pool.shutdown();
            riderThreads.shutdown();

            int checked = 0, violations = 0;
            for (Rider rider : p.riders().all()) {
                List<Order> mine = p.orders().all().stream()
                        .filter(o -> o.riderId().filter(rider.id()::equals).isPresent())
                        .sorted(Comparator.comparing((Order o) -> o.timeOf(OrderStatus.ASSIGNED).orElseThrow()))
                        .toList();
                for (int i = 1; i < mine.size(); i++) {
                    checked++;
                    if (mine.get(i).timeOf(OrderStatus.ASSIGNED).orElseThrow()
                            .isBefore(mine.get(i - 1).timeOf(OrderStatus.DELIVERED).orElseThrow())) violations++;
                }
            }

            int delivered = (int) p.orders().all().stream().filter(o -> o.status() == OrderStatus.DELIVERED).count();
            int riderTotal = p.riders().all().stream().mapToInt(Rider::completedDeliveries).sum();
            long walletMismatch = cs.stream().filter(c -> !walletMatchesOrders(p, c)).count();
            return new Result(customers, stock, placed.size(), shortages.get(), r.stockOf("X"), delivered, riderTotal,
                    checked, violations, walletMismatch, Duration.ofNanos(System.nanoTime() - start));
        } catch (ExecutionException e) {
            throw new IllegalStateException("Simulation task failed", e.getCause());
        } catch (PlatformException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Wallet = opening balance minus exactly one payment per paid order. */
    private static boolean walletMatchesOrders(Platform p, Customer c) {
        Money spent = p.orders().all().stream().filter(o -> o.customer().equals(c) && o.isPaid())
                .map(o -> o.price().total()).reduce(Money.ZERO, Money::plus);
        return c.walletBalance().equals(Money.of(1000).minus(spent));
    }
}
