package com.brasper.platform.billing.infrastructure.acl;

import com.brasper.platform.accounting.interfaces.acl.AccountingContextFacade;
import com.brasper.platform.billing.application.outboundservices.TaxRatesProvider;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
class BillingAccountingTaxRatesProvider implements TaxRatesProvider {

    private final AccountingContextFacade accounting;

    BillingAccountingTaxRatesProvider(AccountingContextFacade accounting) {
        this.accounting = accounting;
    }

    @Override
    public TaxRates ratesAt(LocalDate date) {
        return accounting.fetchTaxRatesAt(date);
    }
}
