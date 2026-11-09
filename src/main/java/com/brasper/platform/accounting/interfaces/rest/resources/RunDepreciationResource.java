package com.brasper.platform.accounting.interfaces.rest.resources;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RunDepreciationResource(@Min(1) @Max(12) int month, @Min(2000) @Max(2100) int year) { }
