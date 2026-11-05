package com.brasper.platform.projects.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Replaces the estimate of {@code category}; {@code currency} must be the project's. */
public record SetCostEstimateResource(
        @NotNull CostCategory category,
        @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal estimatedAmount,
        @NotNull Currency currency) {
}
