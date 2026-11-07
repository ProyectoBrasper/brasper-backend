package com.brasper.platform.accounting.domain.events;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

/** Accounting already posted the entry; this tells Treasury to take the money out of the account. */
public record TaxReturnPaidEvent(Long taxReturnId, LocalDate paidOn, Money amount, Long bankAccountId) { }
