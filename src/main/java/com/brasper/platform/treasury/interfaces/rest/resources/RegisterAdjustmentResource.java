package com.brasper.platform.treasury.interfaces.rest.resources;

import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The amount is in the account's currency. */
public record RegisterAdjustmentResource(
        @NotNull LocalDate occurredOn,
        @NotNull MovementDirection direction,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @Size(max = 500) String concept) {
}
