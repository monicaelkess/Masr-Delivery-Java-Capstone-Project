package com.masrdelivery.money;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;


public final class MoneyCollectors {
    private MoneyCollectors() {}

    public static <T> Collector<T, ?, Money> summing(Function<? super T, Money> mapper) {
        return Collectors.reducing(Money.ZERO, mapper, Money::plus);
    }

    public static <T> Collector<T, ?, Optional<Money>> averaging(Function<? super T, Money> mapper) {
        return Collectors.teeing(summing(mapper), Collectors.counting(),
                (sum, count) -> count == 0 ? Optional.<Money>empty() : Optional.of(sum.dividedBy(count)));
    }
}
