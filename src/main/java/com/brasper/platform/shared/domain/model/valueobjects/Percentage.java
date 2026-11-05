package com.brasper.platform.shared.domain.model.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** A ratio stored as a fraction with 4 decimals: 18% is 0.1800. Never negative. */
public record Percentage(BigDecimal value) {

    private static final int SCALE = 4;

    public Percentage {
        Objects.requireNonNull(value, "value");
        value = value.setScale(SCALE, RoundingMode.HALF_UP);
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Percentage cannot be negative: " + value);
        }
    }

    /** {@code ofPercent(18)} is 18%, stored as 0.1800. */
    public static Percentage ofPercent(BigDecimal percent) {
        return new Percentage(Objects.requireNonNull(percent, "percent").movePointLeft(2));
    }

    public Money of(Money money) {
        return money.multiply(value);
    }
}
