package com.masrdelivery.model;

import com.masrdelivery.exception.InsufficientBalanceException;
import com.masrdelivery.exception.InvalidInputException;
import com.masrdelivery.money.Money;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


public final class Customer implements Identifiable {

    public static final int RECENT_SEARCH_LIMIT = 5;

    private final String id;
    private final String name;
    private final String mobile;
    private final List<Address> addresses = new ArrayList<>();
    private Money wallet;
    private int completedOrders;

    private final Deque<String> recentSearches = new ArrayDeque<>(RECENT_SEARCH_LIMIT + 1);

    public Customer(String id, String name, String mobile, Address firstAddress, Money openingBalance) {
        this.id = Validation.requireText(id, "Customer id");
        this.name = Validation.requireText(name, "Customer name");
        this.mobile = Validation.requireMobile(mobile);
        this.addresses.add(Validation.requireNonNull(firstAddress, "Address"));
        if (openingBalance == null || openingBalance.isNegative())
            throw new InvalidInputException("Opening wallet balance must not be negative.");
        this.wallet = openingBalance;
    }

    @Override public String id() { return id; }
    public String name()   { return name; }
    public String mobile() { return mobile; }

    public synchronized boolean addAddress(Address address) {
        Validation.requireNonNull(address, "Address");
        if (addresses.contains(address)) return false;
        return addresses.add(address);
    }
    public synchronized List<Address> addresses() { return List.copyOf(addresses); }
    public synchronized boolean ownsAddress(Address a) { return addresses.contains(a); }

    public synchronized Money walletBalance() { return wallet; }

        public synchronized void debit(Money amount) throws InsufficientBalanceException {
        if (amount.isNegative()) throw new InvalidInputException("Cannot debit a negative amount.");
        if (wallet.isLessThan(amount)) throw new InsufficientBalanceException(wallet, amount);
        wallet = wallet.minus(amount);
    }

    public synchronized void credit(Money amount) {
        if (amount.isNegative()) throw new InvalidInputException("Cannot credit a negative amount.");
        wallet = wallet.plus(amount);
    }

    public synchronized int completedOrders() { return completedOrders; }
    public synchronized void recordCompletedOrder() { completedOrders++; }


    public LoyaltyTier tier() { return LoyaltyTier.forCompletedOrders(completedOrders()); }

    public synchronized void recordSearch(String text) {
        if (text == null || text.isBlank()) return;
        recentSearches.addFirst(text.trim());
        while (recentSearches.size() > RECENT_SEARCH_LIMIT) recentSearches.removeLast();
    }

    public synchronized List<String> recentSearches() { return List.copyOf(recentSearches); }

    @Override public boolean equals(Object o) { return o instanceof Customer c && id.equals(c.id); }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return name + " [" + id + "]"; }
}
