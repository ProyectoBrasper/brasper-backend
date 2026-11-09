package com.brasper.platform.treasury.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

public record RegisterPayablePaymentCommand(Long payableId, LocalDate paidOn, Money amount, Long bankAccountId) {
}
