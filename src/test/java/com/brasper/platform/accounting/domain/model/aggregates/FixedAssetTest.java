package com.brasper.platform.accounting.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.accounting.domain.model.commands.RegisterFixedAssetCommand;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FixedAssetTest {
    private static final LocalDate DATE = LocalDate.of(2026, 1, 31);
    private static Money money(String value) { return new Money(new BigDecimal(value), Currency.PEN); }
    private static FixedAsset asset(String description, LocalDate date, Money base, int life) {
        return new FixedAsset(new RegisterFixedAssetCommand(description, date, base, life, null), TaxRates.standard());
    }
    private static FixedAsset asset(String base, int life) { return asset(" Machine ", DATE, money(base), life); }

    @Test
    void calculatesTaxesAndMonthlyDepreciationFromBase() {
        FixedAsset asset = asset("1000", 3);
        assertThat(asset.getDescription()).isEqualTo("Machine");
        assertThat(asset.getAmounts().taxableBase()).isEqualTo(money("1000"));
        assertThat(asset.getAmounts().igv()).isEqualTo(money("180"));
        assertThat(asset.getAmounts().total()).isEqualTo(money("1180"));
        assertThat(asset.monthlyDepreciation()).isEqualTo(money("333.33"));
        assertThat(asset("1", 8).monthlyDepreciation()).isEqualTo(money("0.13"));
    }
    @Test
    void countsCalendarMonthsExcludingAcquisitionMonth() {
        FixedAsset asset = asset("1200", 12);
        assertThat(asset.accumulatedDepreciationAt(DATE.minusYears(1))).isEqualTo(money("0"));
        assertThat(asset.accumulatedDepreciationAt(DATE)).isEqualTo(money("0"));
        assertThat(asset.accumulatedDepreciationAt(LocalDate.of(2026, 2, 1))).isEqualTo(money("100"));
        assertThat(asset.accumulatedDepreciationAt(LocalDate.of(2026, 2, 28))).isEqualTo(money("100"));
        assertThat(asset.accumulatedDepreciationAt(LocalDate.of(2027, 1, 1))).isEqualTo(money("1200"));
        assertThat(asset.bookValueAt(LocalDate.of(2026, 3, 1))).isEqualTo(money("1000"));
    }
    @Test
    void absorbsRoundingRemainderAndCapsAtTaxableBase() {
        for (String base : new String[]{"1000", "1001", "0.01"}) {
            FixedAsset asset = asset(base, 3);
            assertThat(asset.accumulatedDepreciationAt(DATE.plusMonths(3))).isEqualTo(money(base));
            assertThat(asset.accumulatedDepreciationAt(DATE.plusYears(100))).isEqualTo(money(base));
            assertThat(asset.bookValueAt(DATE.plusMonths(3))).isEqualTo(money("0"));
        }
        FixedAsset tiny = asset("0.04", 6);
        assertThat(tiny.accumulatedDepreciationAt(DATE.plusMonths(5))).isEqualTo(money("0.04"));
        assertThat(tiny.bookValueAt(DATE.plusMonths(5))).isEqualTo(money("0"));
        assertThat(asset("600", 600).accumulatedDepreciationAt(DATE.plusMonths(600))).isEqualTo(money("600"));
    }
    @Test
    void disposalFreezesAccumulationWithoutChangingEarlierValues() {
        FixedAsset asset = asset("1200", 12);
        LocalDate disposal = LocalDate.of(2026, 3, 1);
        asset.dispose(disposal);
        assertThat(asset.getDisposedOn()).isEqualTo(disposal);
        assertThat(asset.accumulatedDepreciationAt(LocalDate.of(2026, 2, 1))).isEqualTo(money("100"));
        assertThat(asset.accumulatedDepreciationAt(disposal)).isEqualTo(money("200"));
        assertThat(asset.accumulatedDepreciationAt(DATE.plusYears(10))).isEqualTo(money("200"));
        assertThat(asset.bookValueAt(DATE.plusYears(10))).isEqualTo(money("1000"));
        assertThatThrownBy(() -> asset.dispose(disposal)).isInstanceOf(IllegalStateException.class);
    }
    @Test
    void rejectsInvalidDisposalAndAllowsDisposalOnAcquisition() {
        FixedAsset asset = asset("1200", 12);
        assertThatThrownBy(() -> asset.dispose(DATE.minusDays(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> asset.dispose(null)).isInstanceOf(NullPointerException.class);
        assertThat(asset.getDisposedOn()).isNull();
        asset.dispose(DATE);
        assertThat(asset.accumulatedDepreciationAt(DATE.plusYears(1))).isEqualTo(money("0"));
    }
    @Test
    void rejectsInvalidRegistrationAndNullDates() {
        for (String description : new String[]{null, " ", "x".repeat(301)})
            assertThatThrownBy(() -> asset(description, DATE, money("1"), 1)).isInstanceOf(IllegalArgumentException.class);
        for (int life : new int[]{-1, 0, 601})
            assertThatThrownBy(() -> asset("1", life)).isInstanceOf(IllegalArgumentException.class);
        for (String base : new String[]{"0", "-1", "1000000000000", "999999999999"})
            assertThatThrownBy(() -> asset(base, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> asset("Machine", null, money("1"), 1)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> asset("Machine", DATE, null, 1)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new FixedAsset(new RegisterFixedAssetCommand("Machine", DATE, money("1"), 1, null), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> asset("1", 1).accumulatedDepreciationAt(null)).isInstanceOf(NullPointerException.class);
        assertThat(asset("x".repeat(300), DATE, money("1"), 1).getDescription()).hasSize(300);
    }
}
