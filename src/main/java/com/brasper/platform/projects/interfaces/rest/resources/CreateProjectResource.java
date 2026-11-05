package com.brasper.platform.projects.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code budget} is the taxable base (without IGV). Rates are fractions: 0.5 is 50%. */
public record CreateProjectResource(
        @NotBlank String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotNull Long clientId,
        @NotNull @Positive BigDecimal budget,
        @NotNull Currency currency,
        @NotNull LocalDate quotedOn,
        @NotNull @DecimalMin("0.0001") @DecimalMax("1") BigDecimal participation,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal salesCommissionRate) {
}
