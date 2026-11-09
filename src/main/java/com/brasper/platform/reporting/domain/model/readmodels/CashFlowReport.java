package com.brasper.platform.reporting.domain.model.readmodels;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CashFlowReport(LocalDate from, LocalDate to, List<CashFlowLine> lines) {

    public CashFlowReport {
        lines = List.copyOf(lines);
    }

    public BigDecimal finalBalance() {
        return lines.isEmpty() ? BigDecimal.ZERO : lines.getLast().closingBalance();
    }

    public boolean hasNegativeBalance() {
        return lines.stream().anyMatch(line -> line.closingBalance().signum() < 0);
    }
}
