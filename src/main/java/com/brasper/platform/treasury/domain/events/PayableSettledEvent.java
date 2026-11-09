package com.brasper.platform.treasury.domain.events;

import java.time.LocalDate;

/** The last payment of a payable that came from an expense: the expense is paid now. */
public record PayableSettledEvent(Long payableId, Long expenseId, LocalDate settledOn) { }
