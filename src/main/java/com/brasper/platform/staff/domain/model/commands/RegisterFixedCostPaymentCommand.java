package com.brasper.platform.staff.domain.model.commands;

import java.time.LocalDate;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;

public record RegisterFixedCostPaymentCommand(Long fixedCostId, LocalDate paidOn, Period period,
        Money amount, Long bankAccountId, String notes) {}
