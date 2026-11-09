package com.brasper.platform.billing.application.outboundservices;

import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.time.LocalDate;

/** The tax rates in force on a date. Accounting owns them; the adapter asks its facade. */
public interface TaxRatesProvider {

    TaxRates ratesAt(LocalDate date);
}
