package com.masrdelivery.menu;

import com.masrdelivery.model.Restaurant;

@FunctionalInterface
public interface MenuItemParser {
    MenuItem parse(CsvRow row, Restaurant restaurant);
}
