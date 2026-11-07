package com.brasper.platform.accounting.interfaces.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.brasper.platform.accounting.application.queryservices.LedgerQueryService;
import com.brasper.platform.accounting.application.queryservices.TaxQueryService;
import com.brasper.platform.accounting.domain.model.aggregates.TaxReturn;
import com.brasper.platform.accounting.domain.model.commands.PrepareTaxReturnCommand;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AccountingContextFacadeImplTest {

    private final LedgerQueryService ledger = mock(LedgerQueryService.class);
    private final TaxQueryService tax = mock(TaxQueryService.class);
    private final AccountingContextFacade facade = new AccountingContextFacadeImpl(ledger, tax);

    @Test
    void fallsBackToTheStandardRatesWhenNoParametersExist() {
        LocalDate day = LocalDate.of(2026, 3, 1);
        when(tax.findParametersAt(day)).thenReturn(Optional.empty());

        assertThat(facade.fetchTaxRatesAt(day)).isEqualTo(TaxRates.standard());
    }

    @Test
    void reportsTheTotalPayableOfThePreparedReturnOrZero() {
        Money igv = new Money(new BigDecimal("18"), Currency.PEN);
        Money zero = Money.zero(Currency.PEN);
        Money prepayment = new Money(new BigDecimal("1.50"), Currency.PEN);
        TaxReturn taxReturn = new TaxReturn(new PrepareTaxReturnCommand(new Period(2026, 1)), igv, zero, zero, prepayment);
        when(tax.findReturnByPeriod(new Period(2026, 1))).thenReturn(Optional.of(taxReturn));
        when(tax.findReturnByPeriod(new Period(2026, 2))).thenReturn(Optional.empty());

        assertThat(facade.fetchTaxPayableByPeriod(2026, 1)).isEqualByComparingTo("19.50");
        assertThat(facade.fetchTaxPayableByPeriod(2026, 2)).isEqualByComparingTo("0");
    }
}
