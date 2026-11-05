package com.brasper.platform.shared.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TaxBreakdownTest {

    private static final Percentage IGV = TaxRates.standard().igv();

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    @Test
    void fromTaxableBaseAddsTheIgv() {
        TaxBreakdown breakdown = TaxBreakdown.fromTaxableBase(pen("1000"), IGV);

        assertThat(breakdown.igv()).isEqualTo(pen("180"));
        assertThat(breakdown.total()).isEqualTo(pen("1180"));
    }

    @Test
    void fromTotalSplitsTheTotalAndAlwaysAddsUp() {
        TaxBreakdown exact = TaxBreakdown.fromTotal(pen("1180"), IGV);
        assertThat(exact.taxableBase()).isEqualTo(pen("1000"));
        assertThat(exact.igv()).isEqualTo(pen("180"));

        TaxBreakdown odd = TaxBreakdown.fromTotal(pen("100"), IGV);
        assertThat(odd.taxableBase()).isEqualTo(pen("84.75"));
        assertThat(odd.igv()).isEqualTo(pen("15.25"));
        assertThat(odd.taxableBase().add(odd.igv())).isEqualTo(odd.total());
    }

    @Test
    void withoutIgvKeepsTheAmountAsTheTotal() {
        TaxBreakdown breakdown = TaxBreakdown.withoutIgv(pen("50"));

        assertThat(breakdown.igv()).isEqualTo(pen("0"));
        assertThat(breakdown.total()).isEqualTo(pen("50"));
    }

    @Test
    void rejectsAnInconsistentTotal() {
        assertThatThrownBy(() -> new TaxBreakdown(pen("100"), pen("18"), pen("120")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void standardRatesAreThePeruvianOnes() {
        TaxRates rates = TaxRates.standard();

        assertThat(rates.igv().value()).isEqualByComparingTo("0.18");
        assertThat(rates.detraction().value()).isEqualByComparingTo("0.12");
        assertThat(rates.retention().value()).isEqualByComparingTo("0.03");
        assertThat(rates.incomeTaxPrepayment().value()).isEqualByComparingTo("0.015");
    }
}
