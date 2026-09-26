package com.masrdelivery.promotion;

import com.masrdelivery.exception.PromotionNotApplicableException;
import com.masrdelivery.model.District;

public record DistrictCondition(District district) implements PromotionCondition {
    @Override public void check(PricingContext ctx, String code) throws PromotionNotApplicableException {
        if (ctx.deliveryDistrict() != district)
            throw new PromotionNotApplicableException("Promotion " + code + " is only valid for deliveries to "
                    + district + "; this order goes to " + ctx.deliveryDistrict() + ".");
    }
    @Override public String describe() { return "deliveries to " + district + " only"; }
}
