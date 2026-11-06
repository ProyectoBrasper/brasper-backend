package com.brasper.platform.expenses.application.outboundservices;

import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.time.LocalDate;

/** The tax rates in force on a date. Accounting will own them; until it exists an adapter returns fixed ones. */
public interface ExpenseTaxRatesProvider {

    TaxRates ratesAt(LocalDate date);
}
