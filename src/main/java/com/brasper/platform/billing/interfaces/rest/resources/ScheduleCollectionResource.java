package com.brasper.platform.billing.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code expectedOn} is any day of the expected week; the server derives the cash-flow week from it. */
public record ScheduleCollectionResource(
        @NotNull Long projectId,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal expectedAmount,
        @NotNull Currency currency,
        @NotNull LocalDate expectedOn,
        @Size(max = 500) String notes) {
}
