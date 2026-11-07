package com.brasper.platform.accounting.domain.model.valueobjects;

import com.brasper.platform.shared.domain.model.valueobjects.Money;

/** Debit and credit movement of one account, in one currency, over a period. */
public record TrialBalanceLine(AccountCode accountCode, String accountName, Money debit, Money credit) { }
