package com.masrdelivery.promotion;

import com.masrdelivery.model.Customer;
import com.masrdelivery.model.District;
import com.masrdelivery.model.Restaurant;
import com.masrdelivery.money.Money;

import java.time.LocalDate;

/** Everything a promotion may inspect. deliveryFee is AFTER the loyalty benefit. */
public record PricingContext(Customer customer, Restaurant restaurant, District deliveryDistrict,
                             Money subtotal, Money deliveryFee, LocalDate orderDate, boolean firstTimeCustomer) {}
