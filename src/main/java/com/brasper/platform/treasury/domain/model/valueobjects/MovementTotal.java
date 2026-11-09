package com.brasper.platform.treasury.domain.model.valueobjects;

import java.math.BigDecimal;

/** Money that moved in one direction for one source, summed by the database over a range of dates. */
public record MovementTotal(MovementDirection direction, MovementSource source, BigDecimal amount) { }
