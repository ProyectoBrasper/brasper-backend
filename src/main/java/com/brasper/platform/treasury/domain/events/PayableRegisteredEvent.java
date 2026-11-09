package com.brasper.platform.treasury.domain.events;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.valueobjects.PayableType;
import java.time.LocalDate;

/** A payable was created. {@code expenseId} is set when it came from an expense, which Accounting already booked. */
public record PayableRegisteredEvent(Long payableId, Long expenseId, PayableType type, Long projectId, Money total,
        LocalDate registeredOn) { }
