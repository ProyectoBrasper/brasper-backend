package com.brasper.platform.reporting.application.queryservices;

import com.brasper.platform.reporting.application.outboundservices.ProfitabilitySource;
import com.brasper.platform.reporting.application.outboundservices.ProfitabilitySource.ProjectData;
import com.brasper.platform.reporting.domain.model.readmodels.ProjectProfitability;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ProjectProfitabilityQueryService {

    private final ProfitabilitySource source;

    public ProjectProfitabilityQueryService(ProfitabilitySource source) {
        this.source = source;
    }

    public Optional<ProjectProfitability> findByProject(Long projectId, Period period) {
        return source.project(projectId).map(project ->
                build(project, source.igvRate(), source.progressByProject(period.year(), period.month())));
    }

    public List<ProjectProfitability> findPortfolio(Period period) {
        BigDecimal igv = source.igvRate();
        Map<Long, BigDecimal> progress = source.progressByProject(period.year(), period.month());
        return source.allProjects().stream().map(project -> build(project, igv, progress)).toList();
    }

    private ProjectProfitability build(ProjectData project, BigDecimal igv, Map<Long, BigDecimal> progress) {
        BigDecimal commission = project.budget().multiply(BigDecimal.ONE.add(igv))
                .multiply(project.salesCommissionRate()).setScale(2, RoundingMode.HALF_UP);
        return new ProjectProfitability(project.id(), project.code(), project.name(), project.clientName(),
                project.status(), project.currency(), project.budget(), project.estimatedCost(),
                source.directCost(project.id()), source.directCostByCategory(project.id()), commission,
                project.participation(), project.accumulatedProgress(),
                progress.getOrDefault(project.id(), BigDecimal.ZERO), source.invoiced(project.id()),
                source.collected(project.id()));
    }
}
