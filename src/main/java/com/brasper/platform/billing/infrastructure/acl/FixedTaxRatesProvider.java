package com.brasper.platform.billing.infrastructure.acl;

import com.brasper.platform.billing.application.outboundservices.TaxRatesProvider;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/** shortcut: the current Peruvian rates for every date, replace with the Accounting facade when it exists. */
@Component
class FixedTaxRatesProvider implements TaxRatesProvider {

    @Override
    public TaxRates ratesAt(LocalDate date) {
        return TaxRates.standard();
    }
}
