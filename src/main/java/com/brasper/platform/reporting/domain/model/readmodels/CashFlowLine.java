package com.brasper.platform.reporting.domain.model.readmodels;

import com.brasper.platform.reporting.domain.model.valueobjects.CashFlowConcept;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** One week or month of the cash flow, in PEN. {@code projected} lines come from what is scheduled, not from what happened. */
public record CashFlowLine(String label, LocalDate startDate, LocalDate endDate, boolean projected,
        BigDecimal openingBalance, Map<CashFlowConcept, BigDecimal> inflows, Map<CashFlowConcept, BigDecimal> outflows) {

    public CashFlowLine {
        inflows = Map.copyOf(inflows);
        outflows = Map.copyOf(outflows);
    }

    public BigDecimal totalInflows() {
        return total(inflows);
    }

    public BigDecimal totalOutflows() {
        return total(outflows);
    }

    public BigDecimal netFlow() {
        return totalInflows().subtract(totalOutflows());
    }

    public BigDecimal closingBalance() {
        return openingBalance.add(netFlow());
    }

    private static BigDecimal total(Map<CashFlowConcept, BigDecimal> amounts) {
        return amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
