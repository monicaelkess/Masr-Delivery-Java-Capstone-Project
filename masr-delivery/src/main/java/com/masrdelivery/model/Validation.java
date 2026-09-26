package com.masrdelivery.model;

import com.masrdelivery.exception.InvalidInputException;
import com.masrdelivery.money.Money;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class Validation {
    private Validation() {}


    private static final Pattern EGYPT_MOBILE = Pattern.compile("01[0125]\\d{8}");

    public static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new InvalidInputException(field + " must not be blank.");
        return value.trim();
    }

    public static String requireMobile(String mobile) {
        if (mobile == null || !EGYPT_MOBILE.matcher(mobile.trim()).matches())
            throw new InvalidInputException("Mobile number must be 11 digits starting with 010, 011, 012 or 015: " + mobile);
        return mobile.trim();
    }

    public static Money requirePositive(Money value, String field) {
        if (value == null || !value.isPositive()) throw new InvalidInputException(field + " must be greater than zero.");
        return value;
    }

    public static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) throw new InvalidInputException(field + " must be greater than zero.");
        return value;
    }

    public static int requirePositive(int value, String field) {
        if (value <= 0) throw new InvalidInputException(field + " must be greater than zero.");
        return value;
    }

    public static BigDecimal requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) throw new InvalidInputException(field + " must not be negative.");
        return value;
    }

    public static double requireRating(double rating) {
        if (Double.isNaN(rating) || rating < 0.0 || rating > 5.0)
            throw new InvalidInputException("Rating must be between 0.0 and 5.0 inclusive: " + rating);
        return rating;
    }

    public static <T> T requireNonNull(T value, String field) {
        if (value == null) throw new InvalidInputException(field + " is required.");
        return value;
    }
}
