package com.brasper.platform.billing.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Money;

/** A projected sale ("venta proyectada") that the cash flow expects to collect in {@code expectedWeek}. */
public record ScheduleCollectionCommand(Long projectId, Money expectedAmount, CashFlowWeek expectedWeek,
        String notes) {
}
