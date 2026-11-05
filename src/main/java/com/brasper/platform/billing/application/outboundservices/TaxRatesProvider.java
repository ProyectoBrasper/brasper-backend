package com.brasper.platform.billing.application.outboundservices;

import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.time.LocalDate;

/** The tax rates in force on a date. Accounting will own them; until it exists an adapter returns fixed ones. */
public interface TaxRatesProvider {

    TaxRates ratesAt(LocalDate date);
}
