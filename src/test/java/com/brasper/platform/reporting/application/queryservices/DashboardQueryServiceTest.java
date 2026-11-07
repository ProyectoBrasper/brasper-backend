package com.brasper.platform.reporting.application.queryservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.brasper.platform.reporting.application.outboundservices.DashboardSource;
import com.brasper.platform.reporting.application.outboundservices.ProfitabilitySource;
import com.brasper.platform.reporting.application.outboundservices.ProfitabilitySource.ProjectData;
import com.brasper.platform.reporting.domain.model.readmodels.CashFlowLine;
import com.brasper.platform.reporting.domain.model.readmodels.CashFlowReport;
import com.brasper.platform.reporting.domain.model.readmodels.Dashboard;
import com.brasper.platform.reporting.domain.model.readmodels.IncomeStatement;
import com.brasper.platform.reporting.domain.model.readmodels.ProjectProfitability;
import com.brasper.platform.reporting.domain.model.valueobjects.CashFlowConcept;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DashboardQueryServiceTest {

    private static final Period MARCH = new Period(2038, 3);
    private static final LocalDate TODAY = LocalDate.of(2038, 3, 10);

    private final DashboardSource source = mock(DashboardSource.class);
    private final ProfitabilitySource projects = mock(ProfitabilitySource.class);
    private final CashFlowQueryService cashFlow = mock(CashFlowQueryService.class);
    private final FinancialStatementsQueryService statements = mock(FinancialStatementsQueryService.class);
    private final ProjectProfitabilityQueryService profitability = mock(ProjectProfitabilityQueryService.class);
    private final DashboardQueryService service =
            new DashboardQueryService(source, projects, cashFlow, statements, profitability);

    private static BigDecimal n(String value) {
        return new BigDecimal(value);
    }

    private static ProjectData project(long id, String status) {
        return new ProjectData(id, "P" + id, "Plant", "Client", status, "PEN", n("1000"), n("500"), n("1"), n("0.03"), n("0.4"));
    }

    private static ProjectProfitability profitability(long id, String actualCost) {
        return new ProjectProfitability(id, "P" + id, "Plant", "Client", "IN_PROGRESS", "PEN", n("1000"), n("500"),
                n(actualCost), Map.of(), n("35.40"), n("1"), n("0.4"), n("0"), n("0"), n("0"));
    }

    private void figures(String closing) {
        when(source.cashBalanceAt(TODAY)).thenReturn(n("500"));
        when(source.receivablesOutstandingAt(TODAY)).thenReturn(n("200"));
        when(source.payablesOutstanding()).thenReturn(n("80"));
        when(statements.incomeStatement(new Period(2038, 1), MARCH)).thenReturn(
                new IncomeStatement(new Period(2038, 1), MARCH, n("1000"), n("300"), n("100"), n("0"), n("0")));
        when(projects.allProjects()).thenReturn(List.of(project(1, "IN_PROGRESS"), project(2, "COMPLETED")));
        when(profitability.findPortfolio(MARCH)).thenReturn(List.of(profitability(1, "600"), profitability(2, "400")));
        CashFlowLine line = new CashFlowLine("w", TODAY, TODAY, true, n(closing), Map.of(), Map.of(CashFlowConcept.PAYROLL, n("0")));
        when(cashFlow.weekly(MARCH, TODAY)).thenReturn(new CashFlowReport(TODAY, TODAY, List.of(line)));
    }

    @Test
    void composesTheFiguresAndRaisesAnAlertPerOverrunProject() {
        figures("350");

        Dashboard dashboard = service.dashboard(MARCH, TODAY);

        assertThat(dashboard.cashBalance()).isEqualByComparingTo("500");
        assertThat(dashboard.receivablesOutstanding()).isEqualByComparingTo("200");
        assertThat(dashboard.payablesOutstanding()).isEqualByComparingTo("80");
        assertThat(dashboard.netProfitYtd()).isEqualByComparingTo("600");
        assertThat(dashboard.projectedClosingBalance()).isEqualByComparingTo("350");
        assertThat(dashboard.projectsInProgress()).isEqualTo(1);
        assertThat(dashboard.alerts()).containsExactly("COST_OVERRUN:P1");
    }

    @Test
    void aNegativeProjectedBalanceIsAnAlert() {
        figures("-20");

        assertThat(service.dashboard(MARCH, TODAY).alerts()).contains("NEGATIVE_PROJECTED_BALANCE");
    }
}
