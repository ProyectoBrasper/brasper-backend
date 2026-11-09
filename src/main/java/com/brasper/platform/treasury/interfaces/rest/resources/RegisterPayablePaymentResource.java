package com.brasper.platform.treasury.interfaces.rest.resources;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterPayablePaymentResource(
        @NotNull LocalDate paidOn,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull Long bankAccountId) {
}
