package com.brasper.platform.reporting.domain.model.readmodels;

import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;

/** Results over a range of months, in PEN. Costs and expenses are positive amounts. */
public record IncomeStatement(Period from, Period to, BigDecimal revenue, BigDecimal directCosts,
        BigDecimal operatingExpenses, BigDecimal financialExpenses, BigDecimal incomeTax) {

    public BigDecimal grossProfit() {
        return revenue.subtract(directCosts);
    }

    public BigDecimal operatingProfit() {
        return grossProfit().subtract(operatingExpenses);
    }

    public BigDecimal profitBeforeTax() {
        return operatingProfit().subtract(financialExpenses);
    }

    public BigDecimal netProfit() {
        return profitBeforeTax().subtract(incomeTax);
    }
}
