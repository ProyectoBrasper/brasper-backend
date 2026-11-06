package com.brasper.platform.treasury.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import java.time.LocalDate;

public record RegisterLoanCommand(LoanType type, String counterparty, Money principal, Percentage interestRate,
                                  LocalDate disbursedOn, Long bankAccountId) {
}
