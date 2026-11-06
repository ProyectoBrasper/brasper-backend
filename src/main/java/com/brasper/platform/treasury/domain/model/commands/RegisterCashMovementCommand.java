package com.brasper.platform.treasury.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
import java.time.LocalDate;

public record RegisterCashMovementCommand(Long bankAccountId, LocalDate occurredOn, MovementDirection direction,
                                          Money amount, String concept, MovementSource source, Long sourceId) {
}
