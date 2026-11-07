package com.brasper.platform.treasury.domain.events;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import java.time.LocalDate;

/** Cash is already moved. For a RECEIVED loan the company paid; for a GRANTED one it was paid. */
public record LoanInstallmentPaidEvent(Long installmentId, Long loanId, LoanType type, LocalDate paidOn,
        Money principal, Money interest, Long bankAccountId) { }
