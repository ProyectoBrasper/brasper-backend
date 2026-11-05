package com.brasper.platform.projects.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateProjectResource(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotNull @Positive BigDecimal budget,
        @NotNull @DecimalMin("0.0001") @DecimalMax("1") BigDecimal participation,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal salesCommissionRate) {
}
