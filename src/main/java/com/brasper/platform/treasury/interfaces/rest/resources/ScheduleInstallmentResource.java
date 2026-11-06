package com.brasper.platform.treasury.interfaces.rest.resources;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ScheduleInstallmentResource(
        @NotNull LocalDate dueOn,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal principal,
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal interest) {
}
