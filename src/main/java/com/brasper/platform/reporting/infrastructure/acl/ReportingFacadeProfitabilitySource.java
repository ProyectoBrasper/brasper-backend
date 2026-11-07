package com.brasper.platform.reporting.infrastructure.acl;

import com.brasper.platform.accounting.interfaces.acl.AccountingContextFacade;
import com.brasper.platform.billing.interfaces.acl.BillingContextFacade;
import com.brasper.platform.expenses.interfaces.acl.ExpensesContextFacade;
import com.brasper.platform.projects.interfaces.acl.ProjectSnapshot;
import com.brasper.platform.projects.interfaces.acl.ProjectsContextFacade;
import com.brasper.platform.reporting.application.outboundservices.ProfitabilitySource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ReportingFacadeProfitabilitySource implements ProfitabilitySource {

    private static final int PAGE_SIZE = 100;

    private final ProjectsContextFacade projects;
    private final BillingContextFacade billing;
    private final ExpensesContextFacade expenses;
    private final AccountingContextFacade accounting;

    ReportingFacadeProfitabilitySource(ProjectsContextFacade projects, BillingContextFacade billing,
            ExpensesContextFacade expenses, AccountingContextFacade accounting) {
        this.projects = projects;
        this.billing = billing;
        this.expenses = expenses;
        this.accounting = accounting;
    }

    @Override
    public Optional<ProjectData> project(Long projectId) {
        return projects.fetchProjectSnapshot(projectId).map(ReportingFacadeProfitabilitySource::toData);
    }

    @Override
    public List<ProjectData> allProjects() {
        List<ProjectData> all = new ArrayList<>();
        List<ProjectSnapshot> page;
        int number = 0;
        do {
            page = projects.fetchProjectSnapshots(number++, PAGE_SIZE);
            page.stream().map(ReportingFacadeProfitabilitySource::toData).forEach(all::add);
        } while (page.size() == PAGE_SIZE);
        return all;
    }

    @Override
    public Map<Long, BigDecimal> progressByProject(int year, int month) {
        return projects.fetchProgressByPeriod(year, month);
    }

    @Override
    public BigDecimal invoiced(Long projectId) {
        return billing.sumInvoicedByProject(projectId);
    }

    @Override
    public BigDecimal collected(Long projectId) {
        return billing.sumCollectedByProject(projectId);
    }

    @Override
    public BigDecimal directCost(Long projectId) {
        return expenses.sumDirectCostByProject(projectId);
    }

    @Override
    public Map<String, BigDecimal> directCostByCategory(Long projectId) {
        return expenses.fetchDirectCostByCategory(projectId);
    }

    @Override
    public BigDecimal igvRate() {
        return accounting.fetchTaxRatesAt(LocalDate.now()).igv().value();
    }

    private static ProjectData toData(ProjectSnapshot s) {
        return new ProjectData(s.id(), s.code(), s.name(), s.clientName(), s.status(), s.currency(), s.budget(),
                s.estimatedCost(), s.participation(), s.salesCommissionRate(), s.accumulatedProgress());
    }
}
