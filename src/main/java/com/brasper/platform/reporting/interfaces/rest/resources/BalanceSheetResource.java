package com.brasper.platform.reporting.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record BalanceSheetResource(LocalDate asOf, Map<String, BigDecimal> assets,
        Map<String, BigDecimal> liabilities, Map<String, BigDecimal> equity, BigDecimal totalAssets,
        BigDecimal totalLiabilities, BigDecimal totalEquity, BigDecimal difference, boolean balanced) { }
