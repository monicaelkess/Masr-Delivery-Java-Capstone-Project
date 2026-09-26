package com.masrdelivery.menu;

import com.masrdelivery.exception.InvalidInputException;
import com.masrdelivery.money.Money;
import com.masrdelivery.money.MoneyCollectors;
import com.masrdelivery.model.Validation;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


public final class ComboItem extends MenuItem {
    private final List<MenuItem> components;
    private final BigDecimal discountPercent;

    public ComboItem(String restaurantId, String id, String name, String category, int prepMinutes,
                     boolean available, List<MenuItem> components, BigDecimal discountPercent) {
        super(restaurantId, id, name, category, prepMinutes, available);
        if (components == null || components.size() < 2)
            throw new InvalidInputException("A combo must bundle at least two items.");
        for (MenuItem c : components) {
            if (!c.restaurantId().equals(restaurantId))
                throw new InvalidInputException("Combo component " + c.id() + " belongs to another restaurant.");
            if (c.unit() != QuantityUnit.COUNT)
                throw new InvalidInputException("Combo component " + c.id() + " must be sold by count.");
        }
        if (discountPercent == null || discountPercent.signum() <= 0 || discountPercent.compareTo(BigDecimal.valueOf(100)) >= 0)
            throw new InvalidInputException("Combo discount must be greater than 0% and less than 100%.");
        this.components = List.copyOf(components);
        this.discountPercent = discountPercent;
    }

    public Money sumOfParts() { return components.stream().collect(MoneyCollectors.summing(MenuItem::unitPrice)); }

    @Override public Money unitPrice() { Money sum = sumOfParts(); return sum.minus(sum.percent(discountPercent)); }
    @Override public QuantityUnit unit() { return QuantityUnit.COUNT; }
    @Override public Money lineTotal(BigDecimal count) { return unitPrice().times(unit().validate(count)); }
    @Override public String typeCode() { return "COMBO"; }

    @Override public boolean isAvailable() { return super.isAvailable() && components.stream().allMatch(MenuItem::isAvailable); }

    public List<MenuItem> components() { return components; }
    public BigDecimal discountPercent() { return discountPercent; }

    @Override public String priceLabel() {
        return unitPrice() + " (" + discountPercent.stripTrailingZeros().toPlainString() + "% off " + sumOfParts() + ": "
                + components.stream().map(MenuItem::name).collect(Collectors.joining(" + ")) + ")";
    }
}
