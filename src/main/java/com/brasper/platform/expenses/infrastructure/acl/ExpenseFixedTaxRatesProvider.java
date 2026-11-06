package com.brasper.platform.expenses.infrastructure.acl;

import com.brasper.platform.expenses.application.outboundservices.ExpenseTaxRatesProvider;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/** shortcut: the current Peruvian rates for every date, replace with the Accounting facade when it exists. */
@Component
class ExpenseFixedTaxRatesProvider implements ExpenseTaxRatesProvider {

    @Override
    public TaxRates ratesAt(LocalDate date) {
        return TaxRates.standard();
    }
}
