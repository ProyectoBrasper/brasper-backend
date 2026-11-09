package com.brasper.platform.treasury.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;

public record ReschedulePayableCommand(Long payableId, CashFlowWeek scheduledWeek) {
}
