package com.brasper.platform.expenses.infrastructure.acl;

import com.brasper.platform.accounting.interfaces.acl.AccountingContextFacade;
import com.brasper.platform.expenses.application.outboundservices.ExpenseTaxRatesProvider;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
class ExpenseAccountingTaxRatesProvider implements ExpenseTaxRatesProvider {

    private final AccountingContextFacade accounting;

    ExpenseAccountingTaxRatesProvider(AccountingContextFacade accounting) {
        this.accounting = accounting;
    }

    @Override
    public TaxRates ratesAt(LocalDate date) {
        return accounting.fetchTaxRatesAt(date);
    }
}
