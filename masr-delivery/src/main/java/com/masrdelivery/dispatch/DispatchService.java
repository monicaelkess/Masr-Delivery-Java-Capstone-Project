package com.masrdelivery.dispatch;

import com.masrdelivery.config.DistrictDistances;
import com.masrdelivery.exception.DispatchException;
import com.masrdelivery.exception.IllegalOrderTransitionException;
import com.masrdelivery.order.Order;
import com.masrdelivery.repo.InMemoryRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DispatchService {

    public record Assignment(Order order, Rider rider) {}

    private final DispatchQueue queue;
    private final InMemoryRepository<Rider> riders;
    private final DistrictDistances distances;
    private final Clock clock;

    public DispatchService(DispatchQueue queue, InMemoryRepository<Rider> riders, DistrictDistances distances, Clock clock) {
        this.queue = queue;
        this.riders = riders;
        this.distances = distances;
        this.clock = clock;
    }

    public synchronized List<Assignment> dispatchPending() {
        List<Assignment> made = new ArrayList<>();
        for (Order order : queue.inDispatchOrder()) {
            BigDecimal tripKm = distances.between(order.restaurant().district(), order.deliveryAddress().district());
            int parcels = order.parcelCount();
            List<Rider> candidates = riders.all().stream()
                    .filter(Rider::isAvailable)
                    .filter(r -> r.vehicle().canCarry(tripKm, parcels))
                    .sorted(Comparator.comparing((Rider r) -> distances.between(r.currentDistrict(), order.restaurant().district()))
                            .thenComparingInt(Rider::completedDeliveries)
                            .thenComparing(Rider::id))
                    .toList();
            for (Rider rider : candidates) {
                try {
                    rider.assign(order.id());
                } catch (DispatchException lostRace) {
                    continue;
                }
                try {
                    order.assignRider(rider.id(), LocalDateTime.now(clock));
                    made.add(new Assignment(order, rider));
                } catch (IllegalOrderTransitionException cancelledMeanwhile) {
                    rider.release(order.id());
                }
                queue.remove(order);
                break;
            }
        }
        return made;
    }

    public DispatchQueue queue() { return queue; }
}
