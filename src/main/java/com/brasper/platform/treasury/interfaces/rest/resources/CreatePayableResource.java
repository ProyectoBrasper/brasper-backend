package com.brasper.platform.treasury.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.treasury.domain.model.valueobjects.PayableType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreatePayableResource(
        @NotBlank @Size(max = 500) String concept,
        @NotNull PayableType type,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal totalAmount,
        @NotNull Currency currency,
        Long supplierId, Long projectId, Long expenseId,
        @NotNull @Min(1) Integer year,
        @NotNull @Min(1) @Max(12) Integer month,
        @NotNull @Min(1) @Max(6) Integer week) {
}
