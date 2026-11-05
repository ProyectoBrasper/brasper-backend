package com.brasper.platform.shared.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PercentageTest {

    @Test
    void ofPercentStoresAFraction() {
        assertThat(Percentage.ofPercent(new BigDecimal("18")).value()).isEqualByComparingTo("0.18");
        assertThat(Percentage.ofPercent(new BigDecimal("3.5")).value()).isEqualByComparingTo("0.035");
    }

    @Test
    void equalRatiosAreEqual() {
        assertThat(new Percentage(new BigDecimal("0.18"))).isEqualTo(Percentage.ofPercent(new BigDecimal("18")));
    }

    @Test
    void appliesToMoney() {
        Money base = new Money(new BigDecimal("1000"), Currency.PEN);

        assertThat(Percentage.ofPercent(new BigDecimal("18")).of(base))
                .isEqualTo(new Money(new BigDecimal("180"), Currency.PEN));
    }

    @Test
    void rejectsNegative() {
        assertThatThrownBy(() -> new Percentage(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
