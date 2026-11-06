package com.brasper.platform.treasury.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.valueobjects.PayableType;

public record CreatePayableCommand(String concept, PayableType type, Money totalAmount, Long supplierId,
                                   Long projectId, Long expenseId, CashFlowWeek scheduledWeek) {
}
