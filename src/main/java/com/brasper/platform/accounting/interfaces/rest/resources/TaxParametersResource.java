package com.brasper.platform.accounting.interfaces.rest.resources;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TaxParametersResource(Long id, LocalDate effectiveFrom, BigDecimal igv, BigDecimal detraction,
        BigDecimal retention, BigDecimal incomeTaxPrepayment) { }
