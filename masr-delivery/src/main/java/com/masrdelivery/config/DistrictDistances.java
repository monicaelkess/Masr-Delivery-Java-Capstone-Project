package com.masrdelivery.config;

import com.masrdelivery.model.District;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Symmetric road-distance table between districts (km). The assignment gives only
 * Maadi-Faisal = 12 km, so the rest are fixed, documented reference values that every
 * submission can share. Two points in the same district are treated as 2 km apart.
 */
public final class DistrictDistances {

    private final Map<District, Map<District, BigDecimal>> table = new EnumMap<>(District.class);
    private final BigDecimal sameDistrictKm;

    private DistrictDistances(BigDecimal sameDistrictKm) {
        this.sameDistrictKm = sameDistrictKm;
        for (District d : District.values()) table.put(d, new EnumMap<>(District.class));
    }

    public static DistrictDistances defaults() {
        DistrictDistances t = new DistrictDistances(new BigDecimal("2"));
        t.set(District.MAADI, District.DOKKI, "11");
        t.set(District.MAADI, District.FAISAL, "12");
        t.set(District.MAADI, District.NASR_CITY, "14");
        t.set(District.MAADI, District.HELIOPOLIS, "17");
        t.set(District.MAADI, District.ZAMALEK, "10");
        t.set(District.MAADI, District.MOHANDESSIN, "12.5");
        t.set(District.DOKKI, District.FAISAL, "5");
        t.set(District.DOKKI, District.NASR_CITY, "16");
        t.set(District.DOKKI, District.HELIOPOLIS, "17");
        t.set(District.DOKKI, District.ZAMALEK, "3");
        t.set(District.DOKKI, District.MOHANDESSIN, "2.5");
        t.set(District.FAISAL, District.NASR_CITY, "20");
        t.set(District.FAISAL, District.HELIOPOLIS, "21");
        t.set(District.FAISAL, District.ZAMALEK, "7");
        t.set(District.FAISAL, District.MOHANDESSIN, "6");
        t.set(District.NASR_CITY, District.HELIOPOLIS, "6");
        t.set(District.NASR_CITY, District.ZAMALEK, "13");
        t.set(District.NASR_CITY, District.MOHANDESSIN, "16");
        t.set(District.HELIOPOLIS, District.ZAMALEK, "13");
        t.set(District.HELIOPOLIS, District.MOHANDESSIN, "16.5");
        t.set(District.ZAMALEK, District.MOHANDESSIN, "3");
        return t;
    }

    private void set(District a, District b, String km) {
        table.get(a).put(b, new BigDecimal(km));
        table.get(b).put(a, new BigDecimal(km));
    }

    public BigDecimal between(District a, District b) {
        if (a == b) return sameDistrictKm;
        BigDecimal km = table.get(a).get(b);
        if (km == null) throw new IllegalStateException("No distance configured between " + a + " and " + b);
        return km;
    }
}
