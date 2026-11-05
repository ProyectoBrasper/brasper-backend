package com.brasper.platform.projects.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** {@code percentage} is the progress earned in that month, as a fraction (0.25 is 25%). */
public record RegisterProgressResource(
        @Min(2000) @Max(2100) int year,
        @Min(1) @Max(12) int month,
        @NotNull @DecimalMin("0.0001") @DecimalMax("1") BigDecimal percentage) {
}
