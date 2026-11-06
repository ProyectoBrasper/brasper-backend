package com.brasper.platform.treasury.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashMovementResource(Long id, LocalDate occurredOn, String direction, BigDecimal amount,
                                   String concept, String source, Long sourceId) {
}
