package com.brasper.platform.treasury.domain.events;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import java.time.LocalDate;

/** RECEIVED: the company got the money. GRANTED: the company lent it. Cash is already moved. */
public record LoanDisbursedEvent(Long loanId, LoanType type, LocalDate disbursedOn, Money principal,
        Long bankAccountId) { }
