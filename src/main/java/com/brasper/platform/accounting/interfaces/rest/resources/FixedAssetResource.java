package com.brasper.platform.accounting.interfaces.rest.resources;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FixedAssetResource(Long id, String description, LocalDate acquiredOn, BigDecimal taxableBase,
        BigDecimal igv, BigDecimal total, String currency, int usefulLifeMonths, Long expenseId, LocalDate disposedOn,
        BigDecimal monthlyDepreciation, BigDecimal accumulatedDepreciation, BigDecimal bookValue) { }
