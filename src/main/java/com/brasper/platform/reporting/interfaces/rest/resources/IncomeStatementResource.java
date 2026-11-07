package com.brasper.platform.reporting.interfaces.rest.resources;

import java.math.BigDecimal;

public record IncomeStatementResource(int fromYear, int fromMonth, int toYear, int toMonth, BigDecimal revenue,
        BigDecimal directCosts, BigDecimal grossProfit, BigDecimal operatingExpenses, BigDecimal operatingProfit,
        BigDecimal financialExpenses, BigDecimal profitBeforeTax, BigDecimal incomeTax, BigDecimal netProfit) { }
