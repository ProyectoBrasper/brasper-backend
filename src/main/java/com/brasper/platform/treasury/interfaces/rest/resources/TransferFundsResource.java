package com.brasper.platform.treasury.interfaces.rest.resources;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The amount is in the currency of the origin account. */
public record TransferFundsResource(
        @NotNull Long fromAccountId,
        @NotNull Long toAccountId,
        @NotNull LocalDate transferredOn,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @Size(max = 500) String concept) {
}
