package com.brasper.platform.treasury.application.queryservices;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;

/** An account with its balance already computed, because the movements are lazy and the transaction ends here. */
public record BankAccountSummary(BankAccount account, Money currentBalance) {
}
