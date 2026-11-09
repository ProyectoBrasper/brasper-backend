package com.brasper.platform.accounting.interfaces.acl;

import com.brasper.platform.accounting.application.queryservices.LedgerQueryService;
import com.brasper.platform.accounting.application.queryservices.TaxQueryService;
import com.brasper.platform.accounting.domain.model.aggregates.TaxParameters;
import com.brasper.platform.accounting.domain.model.aggregates.TaxReturn;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Lives in {@code interfaces} because the layer rules forbid application from depending on the facade contract. */
@Service
public class AccountingContextFacadeImpl implements AccountingContextFacade {

    private final LedgerQueryService ledger;
    private final TaxQueryService tax;

    public AccountingContextFacadeImpl(LedgerQueryService ledger, TaxQueryService tax) {
        this.ledger = ledger;
        this.tax = tax;
    }

    @Override
    public TaxRates fetchTaxRatesAt(LocalDate date) {
        return tax.findParametersAt(date).map(TaxParameters::getRates).orElseGet(TaxRates::standard);
    }

    @Override
    public Map<String, BigDecimal> fetchAccountBalancesAt(LocalDate date) {
        return ledger.accountBalancesAt(date);
    }

    @Override
    public Map<String, BigDecimal> fetchAccountMovementsBetween(LocalDate from, LocalDate to) {
        return ledger.accountMovementsBetween(from, to);
    }

    @Override
    public Map<String, BigDecimal> fetchBalancesByAccountTypeAt(LocalDate date) {
        return ledger.balancesByTypeAt(date);
    }

    @Override
    public Map<String, BigDecimal> fetchResultByPeriod(int year, int month) {
        return ledger.resultByPeriod(new Period(year, month));
    }

    @Override
    public BigDecimal fetchTaxPayableByPeriod(int year, int month) {
        return tax.findReturnByPeriod(new Period(year, month)).map(TaxReturn::totalPayable)
                .map(total -> total.amount()).orElse(BigDecimal.ZERO);
    }
}
