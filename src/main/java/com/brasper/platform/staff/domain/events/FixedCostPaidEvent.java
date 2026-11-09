package com.brasper.platform.staff.domain.events;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.staff.domain.model.valueobjects.FixedCostType;
import java.time.LocalDate;

/** {@code bankAccountId} is where the money came from; null when the payment names no account. */
public record FixedCostPaidEvent(Long paymentId, Long fixedCostId, FixedCostType type, LocalDate paidOn,
        Money amount, Long bankAccountId) { }
