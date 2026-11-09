package com.brasper.platform.reporting.domain.model.readmodels;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Position on a date, in PEN, keyed by account code. Equity also carries {@code RESULT}, the profit not yet closed. */
public record BalanceSheet(LocalDate asOf, Map<String, BigDecimal> assets, Map<String, BigDecimal> liabilities,
        Map<String, BigDecimal> equity) {

    public BalanceSheet {
        assets = Map.copyOf(assets);
        liabilities = Map.copyOf(liabilities);
        equity = Map.copyOf(equity);
    }

    public BigDecimal totalAssets() {
        return total(assets);
    }

    public BigDecimal totalLiabilities() {
        return total(liabilities);
    }

    public BigDecimal totalEquity() {
        return total(equity);
    }

    public BigDecimal difference() {
        return totalAssets().subtract(totalLiabilities()).subtract(totalEquity());
    }

    public boolean isBalanced() {
        return difference().signum() == 0;
    }

    private static BigDecimal total(Map<String, BigDecimal> lines) {
        return lines.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
