package com.brasper.platform.reporting.application.queryservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.brasper.platform.reporting.application.outboundservices.ProfitabilitySource;
import com.brasper.platform.reporting.application.outboundservices.ProfitabilitySource.ProjectData;
import com.brasper.platform.reporting.domain.model.readmodels.ProjectProfitability;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ProjectProfitabilityQueryServiceTest {

    private static final Period PERIOD = new Period(2026, 8);

    private final ProfitabilitySource source = mock(ProfitabilitySource.class);
    private final ProjectProfitabilityQueryService service = new ProjectProfitabilityQueryService(source);

    private static ProjectData project(long id, String participation) {
        return new ProjectData(id, "P" + id, "Plant " + id, "Linings", "IN_PROGRESS", "PEN", new BigDecimal("1000"),
                new BigDecimal("500"), new BigDecimal(participation), new BigDecimal("0.03"), new BigDecimal("0.40"));
    }

    private void costs(long id) {
        when(source.directCost(id)).thenReturn(new BigDecimal("600"));
        when(source.directCostByCategory(id)).thenReturn(Map.of("MATERIALS", new BigDecimal("600")));
        when(source.invoiced(id)).thenReturn(new BigDecimal("400"));
        when(source.collected(id)).thenReturn(new BigDecimal("300"));
    }

    @Test
    void computesCommissionProfitMarginDeviationAndRecognizedProfit() {
        when(source.project(1L)).thenReturn(Optional.of(project(1, "0.5")));
        when(source.igvRate()).thenReturn(new BigDecimal("0.18"));
        when(source.progressByProject(2026, 8)).thenReturn(Map.of(1L, new BigDecimal("0.25")));
        costs(1);

        ProjectProfitability p = service.findByProject(1L, PERIOD).orElseThrow();

        assertThat(p.salesCommission()).isEqualByComparingTo("35.40");
        assertThat(p.costDeviation()).isEqualByComparingTo("100");
        assertThat(p.grossProfit()).isEqualByComparingTo("182.30");
        assertThat(p.margin()).isEqualByComparingTo("0.1823");
        assertThat(p.recognizedProfit()).isEqualByComparingTo("45.58");
        assertThat(p.invoiced()).isEqualByComparingTo("400");
        assertThat(p.collected()).isEqualByComparingTo("300");
    }

    @Test
    void anUnknownProjectHasNoReportAndAProjectWithoutProgressRecognizesNothing() {
        when(source.project(9L)).thenReturn(Optional.empty());
        when(source.project(1L)).thenReturn(Optional.of(project(1, "1")));
        when(source.igvRate()).thenReturn(new BigDecimal("0.18"));
        when(source.progressByProject(2026, 8)).thenReturn(Map.of());
        costs(1);

        assertThat(service.findByProject(9L, PERIOD)).isEmpty();
        assertThat(service.findByProject(1L, PERIOD).orElseThrow().recognizedProfit()).isEqualByComparingTo("0");
    }

    @Test
    void thePortfolioListsEveryProject() {
        List<ProjectData> all = IntStream.rangeClosed(1, 3).mapToObj(i -> project(i, "1")).toList();
        when(source.allProjects()).thenReturn(all);
        when(source.igvRate()).thenReturn(new BigDecimal("0.18"));
        when(source.progressByProject(2026, 8)).thenReturn(Map.of());
        all.forEach(p -> costs(p.id()));

        assertThat(service.findPortfolio(PERIOD)).extracting(ProjectProfitability::projectId).containsExactly(1L, 2L, 3L);
    }
}
