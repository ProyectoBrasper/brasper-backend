package com.brasper.platform.accounting.domain.model.valueobjects;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import java.math.BigDecimal;

/** Debits and credits of one account in one currency, summed by the database over some set of entries. */
public record AccountTotals(AccountCode accountCode, Currency currency, BigDecimal debit, BigDecimal credit) { }
