package com.brasper.platform.reporting.interfaces.rest.transform;

import com.brasper.platform.reporting.domain.model.readmodels.BalanceSheet;
import com.brasper.platform.reporting.domain.model.readmodels.IncomeStatement;
import com.brasper.platform.reporting.interfaces.rest.resources.BalanceSheetResource;
import com.brasper.platform.reporting.interfaces.rest.resources.IncomeStatementResource;

public final class FinancialStatementsResourceAssembler {

    private FinancialStatementsResourceAssembler() { }

    public static IncomeStatementResource toResource(IncomeStatement s) {
        return new IncomeStatementResource(s.from().year(), s.from().month(), s.to().year(), s.to().month(),
                s.revenue(), s.directCosts(), s.grossProfit(), s.operatingExpenses(), s.operatingProfit(),
                s.financialExpenses(), s.profitBeforeTax(), s.incomeTax(), s.netProfit());
    }

    public static BalanceSheetResource toResource(BalanceSheet b) {
        return new BalanceSheetResource(b.asOf(), b.assets(), b.liabilities(), b.equity(), b.totalAssets(),
                b.totalLiabilities(), b.totalEquity(), b.difference(), b.isBalanced());
    }
}
