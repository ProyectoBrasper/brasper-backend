package com.brasper.platform.accounting.interfaces.acl;

import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** The only way other contexts ask Accounting for tax rates and ledger figures. Amounts are in PEN. */
public interface AccountingContextFacade {

    /** The rates in force on the date; the standard Peruvian ones while no parameters have been set. */
    TaxRates fetchTaxRatesAt(LocalDate date);

    /** Balance per account code up to and including the date. */
    Map<String, BigDecimal> fetchAccountBalancesAt(LocalDate date);

    /** Movement per account code between the two dates, both included, signed like the balances. */
    Map<String, BigDecimal> fetchAccountMovementsBetween(LocalDate from, LocalDate to);

    /** Balance per {@code LedgerAccountType} name (ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE) up to the date. */
    Map<String, BigDecimal> fetchBalancesByAccountTypeAt(LocalDate date);

    /** REVENUE, EXPENSE and RESULT of the month. */
    Map<String, BigDecimal> fetchResultByPeriod(int year, int month);

    /** Total payable of the month's tax return; zero when none has been prepared. */
    BigDecimal fetchTaxPayableByPeriod(int year, int month);
}
