package com.brasper.platform.staff.interfaces.rest.resources;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterFixedCostPaymentResource(@NotNull LocalDate paidOn,
        @Min(1) int year, @Min(1) @Max(12) int month,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        Long bankAccountId, @Size(max = 500) String notes) {}
