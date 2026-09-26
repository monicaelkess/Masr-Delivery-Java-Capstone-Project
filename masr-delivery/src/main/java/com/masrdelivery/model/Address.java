package com.masrdelivery.model;


public record Address(District district, String detail) {
    public Address {
        Validation.requireNonNull(district, "District");
        detail = Validation.requireText(detail, "Address detail").replaceAll("\\s+", " ");
    }
    @Override public String toString() { return detail + ", " + district; }
}
