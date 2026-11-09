package com.brasper.platform.treasury.domain.model.commands;

import java.time.LocalDate;

public record PayLoanInstallmentCommand(Long loanId, Long installmentId, LocalDate paidOn, Long bankAccountId) {
}
