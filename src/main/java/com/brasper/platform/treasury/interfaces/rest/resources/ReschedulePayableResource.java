package com.brasper.platform.treasury.interfaces.rest.resources;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReschedulePayableResource(
        @NotNull @Min(1) Integer year,
        @NotNull @Min(1) @Max(12) Integer month,
        @NotNull @Min(1) @Max(6) Integer week) {
}
