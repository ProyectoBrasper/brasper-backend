package com.brasper.platform.reporting.domain.model.readmodels;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/** Management view of one project. Amounts are in {@code currency}; the rates are fractions. */
public record ProjectProfitability(Long projectId, String code, String name, String clientName, String status,
        String currency, BigDecimal budget, BigDecimal estimatedCost, BigDecimal actualCost,
        Map<String, BigDecimal> actualCostByCategory, BigDecimal salesCommission, BigDecimal participation,
        BigDecimal accumulatedProgress, BigDecimal progressInPeriod, BigDecimal invoiced, BigDecimal collected) {

    public ProjectProfitability {
        actualCostByCategory = Map.copyOf(actualCostByCategory);
    }

    public BigDecimal costDeviation() {
        return actualCost.subtract(estimatedCost);
    }

    public BigDecimal grossProfit() {
        return budget.subtract(actualCost).subtract(salesCommission).multiply(participation)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal margin() {
        return budget.signum() == 0 ? BigDecimal.ZERO : grossProfit().divide(budget, 4, RoundingMode.HALF_UP);
    }

    /** The share of the gross profit earned by the progress of the period. */
    public BigDecimal recognizedProfit() {
        return grossProfit().multiply(progressInPeriod).setScale(2, RoundingMode.HALF_UP);
    }
}
