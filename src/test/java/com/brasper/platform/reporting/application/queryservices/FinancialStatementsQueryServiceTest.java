package com.brasper.platform.reporting.application.queryservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.brasper.platform.reporting.application.outboundservices.StatementsSource;
import com.brasper.platform.reporting.domain.model.readmodels.BalanceSheet;
import com.brasper.platform.reporting.domain.model.readmodels.IncomeStatement;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinancialStatementsQueryServiceTest {

    private final StatementsSource source = mock(StatementsSource.class);
    private final FinancialStatementsQueryService service = new FinancialStatementsQueryService(source);

    private static BigDecimal n(String value) {
        return new BigDecimal(value);
    }

    @Test
    void groupsTheMovementsOfTheRangeIntoTheIncomeStatement() {
        when(source.movementsBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))).thenReturn(Map.of(
                "70", n("1000"), "60", n("300"), "62", n("100"), "68", n("20"), "67", n("30"), "88", n("50"),
                "10", n("999")));

        IncomeStatement statement = service.incomeStatement(new Period(2026, 1), new Period(2026, 3));

        assertThat(statement.revenue()).isEqualByComparingTo("1000");
        assertThat(statement.directCosts()).isEqualByComparingTo("300");
        assertThat(statement.operatingExpenses()).isEqualByComparingTo("120");
        assertThat(statement.financialExpenses()).isEqualByComparingTo("30");
        assertThat(statement.grossProfit()).isEqualByComparingTo("700");
        assertThat(statement.operatingProfit()).isEqualByComparingTo("580");
        assertThat(statement.profitBeforeTax()).isEqualByComparingTo("550");
        assertThat(statement.netProfit()).isEqualByComparingTo("500");
    }

    @Test
    void rejectsARangeThatEndsBeforeItStarts() {
        assertThatThrownBy(() -> service.incomeStatement(new Period(2026, 3), new Period(2026, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theBalanceSheetBalancesAndCarriesTheProfitAsResult() {
        // sale of 100 + 18 IGV, then a 50 purchase + 9 IGV paid in cash; 39 is the depreciation contra account
        when(source.balancesAt(LocalDate.of(2026, 1, 31))).thenReturn(Map.of(
                "10", n("-59"), "12", n("118"), "40111", n("9"), "70", n("100"), "60", n("50"), "50", n("0")));

        BalanceSheet sheet = service.balanceSheet(new Period(2026, 1));

        assertThat(sheet.assets()).containsOnlyKeys("10", "12");
        assertThat(sheet.liabilities()).containsOnlyKeys("40111");
        assertThat(sheet.equity()).containsOnlyKeys("RESULT");
        assertThat(sheet.equity().get("RESULT")).isEqualByComparingTo("50");
        assertThat(sheet.totalAssets()).isEqualByComparingTo("59");
        assertThat(sheet.difference()).isEqualByComparingTo("0");
        assertThat(sheet.isBalanced()).isTrue();
    }
}
