package com.brasper.platform.shared.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    @Test
    void normalizesToTwoDecimalsSoEqualAmountsAreEqual() {
        assertThat(pen("10")).isEqualTo(pen("10.00"));
        assertThat(pen("10.005").amount()).isEqualByComparingTo("10.01");
    }

    @Test
    void addsAndSubtractsSameCurrency() {
        assertThat(pen("100.50").add(pen("0.50"))).isEqualTo(pen("101"));
        assertThat(pen("100").subtract(pen("30.25"))).isEqualTo(pen("69.75"));
    }

    @Test
    void rejectsMixedCurrencies() {
        Money usd = new Money(BigDecimal.ONE, Currency.USD);

        assertThatThrownBy(() -> pen("1").add(usd)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pen("1").subtract(usd)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multipliesAndRoundsToCents() {
        assertThat(pen("100").multiply(new BigDecimal("0.18"))).isEqualTo(pen("18"));
        assertThat(pen("10.01").multiply(new BigDecimal("0.18"))).isEqualTo(pen("1.80"));
    }

    @Test
    void roundsToWholeUnitsForDetractions() {
        assertThat(pen("118.50").multiply(new BigDecimal("0.12")).roundToUnits()).isEqualTo(pen("14"));
        assertThat(pen("14.50").roundToUnits()).isEqualTo(pen("15"));
    }

    @Test
    void zeroIsNotPositive() {
        assertThat(Money.zero(Currency.USD).isPositive()).isFalse();
        assertThat(pen("0.01").isPositive()).isTrue();
        assertThat(pen("-1").isPositive()).isFalse();
    }

    @Test
    void rejectsNulls() {
        assertThatThrownBy(() -> new Money(null, Currency.PEN)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
    }
}
