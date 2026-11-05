package com.brasper.platform.billing.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;

public record RescheduleCollectionCommand(Long scheduledCollectionId, CashFlowWeek expectedWeek) {
}
