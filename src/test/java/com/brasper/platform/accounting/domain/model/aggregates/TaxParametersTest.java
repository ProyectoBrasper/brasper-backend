package com.brasper.platform.accounting.domain.model.aggregates;

import static org.assertj.core.api.Assertions.*;
import com.brasper.platform.accounting.domain.model.commands.SetTaxParametersCommand;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TaxParametersTest {
    @Test void retainsRatesAndEffectiveDate() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        TaxParameters parameters = new TaxParameters(new SetTaxParametersCommand(TaxRates.standard(), date));
        assertThat(parameters.getRates()).isEqualTo(TaxRates.standard());
        assertThat(parameters.getEffectiveFrom()).isEqualTo(date);
    }
    @Test void requiresRatesAndEffectiveDate() {
        assertThatNullPointerException().isThrownBy(() ->
                new TaxParameters(new SetTaxParametersCommand(null, LocalDate.of(2026, 11, 1))));
        assertThatNullPointerException().isThrownBy(() ->
                new TaxParameters(new SetTaxParametersCommand(TaxRates.standard(), null)));
    }
}
