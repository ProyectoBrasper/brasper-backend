package com.brasper.platform.accounting.interfaces.rest.resources;
import jakarta.validation.constraints.*;

public record PrepareTaxReturnResource(@Min(2000) @Max(2100) int year, @Min(1) @Max(12) int month) { }
