package com.masrdelivery.dispatch;

import com.masrdelivery.exception.RiderBusyException;
import com.masrdelivery.exception.RiderUnavailableException;
import com.masrdelivery.model.District;
import com.masrdelivery.model.Identifiable;
import com.masrdelivery.model.Validation;

import java.util.Optional;


public final class Rider implements Identifiable {

    private final String id;
    private final String name;
    private final Vehicle vehicle;


    private District currentDistrict;
    private boolean onDuty;
    private String activeOrderId;
    private int completedDeliveries;

    public Rider(String id, String name, Vehicle vehicle, District currentDistrict) {
        this.id = Validation.requireText(id, "Rider id");
        this.name = Validation.requireText(name, "Rider name");
        this.vehicle = Validation.requireNonNull(vehicle, "Vehicle");
        this.currentDistrict = Validation.requireNonNull(currentDistrict, "District");
    }

    public synchronized void goOnDuty() { onDuty = true; }

    public synchronized void goOffDuty() throws RiderBusyException {
        if (activeOrderId != null) throw new RiderBusyException(id, activeOrderId);
        onDuty = false;
    }

    public synchronized void assign(String orderId) throws RiderUnavailableException, RiderBusyException {
        if (!onDuty) throw new RiderUnavailableException("Rider " + id + " is off duty.");
        if (activeOrderId != null) throw new RiderBusyException(id, activeOrderId);
        activeOrderId = orderId;
    }

    public synchronized void completeDelivery(String orderId, District dropOff) {
        if (!orderId.equals(activeOrderId))
            throw new IllegalStateException("Rider " + id + " is not carrying " + orderId);
        activeOrderId = null;
        completedDeliveries++;
        currentDistrict = dropOff;
    }

       public synchronized void release(String orderId) {
        if (orderId.equals(activeOrderId)) activeOrderId = null;
    }

    @Override public String id()      { return id; }
    public String name()               { return name; }
    public Vehicle vehicle()           { return vehicle; }
    public synchronized District currentDistrict()     { return currentDistrict; }
    public synchronized boolean isOnDuty()             { return onDuty; }
    public synchronized boolean isAvailable()          { return onDuty && activeOrderId == null; }
    public synchronized Optional<String> activeOrderId(){ return Optional.ofNullable(activeOrderId); }
    public synchronized int completedDeliveries()      { return completedDeliveries; }

    @Override public boolean equals(Object o) { return o instanceof Rider r && id.equals(r.id); }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return name + " [" + id + ", " + vehicle.displayName() + "]"; }
}
