package com.brasper.platform.reporting.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record CashFlowReportResource(LocalDate from, LocalDate to, BigDecimal finalBalance,
        boolean hasNegativeBalance, List<Line> lines) {

    public record Line(String label, LocalDate startDate, LocalDate endDate, boolean projected,
            BigDecimal openingBalance, Map<String, BigDecimal> inflows, Map<String, BigDecimal> outflows,
            BigDecimal totalInflows, BigDecimal totalOutflows, BigDecimal netFlow, BigDecimal closingBalance) { }
}
