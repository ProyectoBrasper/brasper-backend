package com.brasper.platform.reporting.application.queryservices;

import com.brasper.platform.reporting.application.outboundservices.DashboardSource;
import com.brasper.platform.reporting.application.outboundservices.ProfitabilitySource;
import com.brasper.platform.reporting.domain.model.readmodels.CashFlowReport;
import com.brasper.platform.reporting.domain.model.readmodels.Dashboard;
import com.brasper.platform.reporting.domain.model.readmodels.ProjectProfitability;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Composes the other reports. Alerts: {@code NEGATIVE_PROJECTED_BALANCE} when any week of the month closes below zero,
 * and {@code COST_OVERRUN:<project code>} for each project whose real cost passes its estimate.
 * shortcut: no overdue-invoice alert yet, because Billing exposes no due dates.
 */
@Service
public class DashboardQueryService {

    private static final String IN_PROGRESS = "IN_PROGRESS";

    private final DashboardSource source;
    private final ProfitabilitySource projects;
    private final CashFlowQueryService cashFlow;
    private final FinancialStatementsQueryService statements;
    private final ProjectProfitabilityQueryService profitability;

    public DashboardQueryService(DashboardSource source, ProfitabilitySource projects, CashFlowQueryService cashFlow,
            FinancialStatementsQueryService statements, ProjectProfitabilityQueryService profitability) {
        this.source = source;
        this.projects = projects;
        this.cashFlow = cashFlow;
        this.statements = statements;
        this.profitability = profitability;
    }

    /** Balances are taken at the end of the month, or today when the month has not ended. */
    public Dashboard dashboard(Period period, LocalDate today) {
        LocalDate asOf = period.endDate().isBefore(today) ? period.endDate() : today;
        CashFlowReport flow = cashFlow.weekly(period, today);
        List<String> alerts = new ArrayList<>();
        if (flow.hasNegativeBalance()) {
            alerts.add("NEGATIVE_PROJECTED_BALANCE");
        }
        profitability.findPortfolio(period).stream().filter(p -> p.costDeviation().signum() > 0)
                .map(ProjectProfitability::code).forEach(code -> alerts.add("COST_OVERRUN:" + code));
        return new Dashboard(source.cashBalanceAt(asOf), source.receivablesOutstandingAt(asOf),
                source.payablesOutstanding(), statements.incomeStatement(new Period(period.year(), 1), period).netProfit(),
                flow.finalBalance(),
                (int) projects.allProjects().stream().filter(project -> IN_PROGRESS.equals(project.status())).count(),
                alerts);
    }
}
