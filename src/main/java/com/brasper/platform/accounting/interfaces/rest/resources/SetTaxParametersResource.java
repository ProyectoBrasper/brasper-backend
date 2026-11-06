package com.brasper.platform.accounting.interfaces.rest.resources;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Rates are fractions: 0.18 means 18%, matching Percentage. */
public record SetTaxParametersResource(@NotNull LocalDate effectiveFrom,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal igv,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal detraction,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal retention,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal incomeTaxPrepayment) { }
