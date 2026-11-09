package com.brasper.platform.projects.interfaces.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.projects.domain.model.aggregates.Client;
import com.brasper.platform.projects.domain.model.aggregates.Project;
import com.brasper.platform.projects.domain.model.commands.CreateClientCommand;
import com.brasper.platform.projects.domain.model.commands.CreateProjectCommand;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectCode;
import com.brasper.platform.projects.domain.repositories.ClientRepository;
import com.brasper.platform.projects.domain.repositories.ProjectRepository;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ProjectsContextFacadeTest {

    @Autowired
    private ProjectsContextFacade facade;
    @Autowired
    private ClientRepository clients;
    @Autowired
    private ProjectRepository projects;

    private Long clientId;

    private Long saveProject(String code) {
        if (clientId == null) {
            clientId = clients.save(new Client(
                    new CreateClientCommand("Linings", new Ruc("20123456789"), null, null, null))).getId();
        }
        Project project = new Project(new CreateProjectCommand(new ProjectCode(code), "Planta " + code, null,
                clientId, new Money(new BigDecimal("1000"), Currency.USD), LocalDate.of(2026, 8, 3),
                new Percentage(new BigDecimal("0.5")), new Percentage(new BigDecimal("0.03"))));
        project.setCostEstimate(CostCategory.MATERIALS, new Money(new BigDecimal("300"), Currency.USD));
        project.setCostEstimate(CostCategory.LABOR, new Money(new BigDecimal("100.50"), Currency.USD));
        project.start();
        project.registerProgress(new Period(2026, 8), new Percentage(new BigDecimal("0.4")));
        return projects.save(project).getId();
    }

    @Test
    void looksProjectsUpByIdAndCode() {
        Long id = saveProject("862BR-AGO-2026");

        assertThat(facade.existsProjectById(id)).isTrue();
        assertThat(facade.existsProjectById(999_999L)).isFalse();
        assertThat(facade.fetchProjectIdByCode("862BR-AGO-2026")).contains(id);
        assertThat(facade.fetchProjectIdByCode("862br-ago-2026")).contains(id);
        assertThat(facade.fetchProjectIdByCode("001BR-ENE-2027")).isEmpty();
        assertThat(facade.fetchProjectIdByCode("not a code")).isEmpty();
        assertThat(facade.fetchProjectStatus(id)).contains("IN_PROGRESS");
        assertThat(facade.fetchProjectStatus(999_999L)).isEmpty();
    }

    @Test
    void snapshotCarriesTheProjectNumbersAndTheClientName() {
        Long id = saveProject("862BR-AGO-2026");

        ProjectSnapshot snapshot = facade.fetchProjectSnapshot(id).orElseThrow();

        assertThat(snapshot.code()).isEqualTo("862BR-AGO-2026");
        assertThat(snapshot.clientId()).isEqualTo(clientId);
        assertThat(snapshot.clientName()).isEqualTo("Linings");
        assertThat(snapshot.status()).isEqualTo("IN_PROGRESS");
        assertThat(snapshot.currency()).isEqualTo("USD");
        assertThat(snapshot.budget()).isEqualByComparingTo("1000");
        assertThat(snapshot.participation()).isEqualByComparingTo("0.5");
        assertThat(snapshot.accumulatedProgress()).isEqualByComparingTo("0.4");
        assertThat(snapshot.estimatedCost()).isEqualByComparingTo("400.50");
        assertThat(facade.fetchProjectSnapshot(999_999L)).isEmpty();
    }

    @Test
    void pagesSnapshotsByIdAndRejectsBadPages() {
        Long first = saveProject("862BR-AGO-2026");
        Long second = saveProject("863BR-AGO-2026");
        Long third = saveProject("864BR-AGO-2026");

        // other tests may have committed projects, so walk every page instead of assuming an empty table
        List<Long> all = new ArrayList<>();
        for (int page = 0; ; page++) {
            List<ProjectSnapshot> snapshots = facade.fetchProjectSnapshots(page, 2);
            assertThat(snapshots.size()).isLessThanOrEqualTo(2);
            if (snapshots.isEmpty()) {
                break;
            }
            snapshots.forEach(snapshot -> all.add(snapshot.id()));
        }
        assertThat(all).endsWith(first, second, third).isSorted();
        assertThat(facade.fetchProjectSnapshots(all.size() + 5, 2)).isEmpty();
        assertThatThrownBy(() -> facade.fetchProjectSnapshots(-1, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> facade.fetchProjectSnapshots(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void progressByPeriodListsOnlyProjectsWithProgressThatMonth() {
        Long id = saveProject("862BR-AGO-2026");

        assertThat(facade.fetchProgressByPeriod(2026, 8)).containsOnlyKeys(id);
        assertThat(facade.fetchProgressByPeriod(2026, 8).get(id)).isEqualByComparingTo("0.4");
        assertThat(facade.fetchProgressByPeriod(2026, 9)).isEmpty();
    }
}
