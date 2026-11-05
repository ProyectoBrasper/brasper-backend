package com.brasper.platform.projects.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.projects.domain.model.aggregates.Client;
import com.brasper.platform.projects.domain.model.aggregates.Project;
import com.brasper.platform.projects.domain.model.commands.CreateClientCommand;
import com.brasper.platform.projects.domain.model.commands.CreateProjectCommand;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectCode;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectStatus;
import com.brasper.platform.projects.domain.repositories.ClientRepository;
import com.brasper.platform.projects.domain.repositories.ProjectRepository;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ProjectJpaRepositoryTest {

    @Autowired
    private ProjectRepository projects;
    @Autowired
    private ClientRepository clients;
    @Autowired
    private EntityManager em;

    private Project saveAndFlush(Project project) {
        Project saved = projects.save(project);
        em.flush();
        return saved;
    }

    private Long clientId() {
        return clients.save(new Client(new CreateClientCommand("Linings", new Ruc("20123456789"), null, null, null)))
                .getId();
    }

    private Project project(String code, Long clientId) {
        return new Project(new CreateProjectCommand(new ProjectCode(code), "Planta", null, clientId,
                new Money(new BigDecimal("1234.50"), Currency.PEN), LocalDate.of(2026, 8, 3),
                new Percentage(new BigDecimal("0.5")), new Percentage(new BigDecimal("0.03"))));
    }

    @Test
    void persistsEveryFieldIncludingValueObjectsAndAuditDates() {
        Long clientId = clientId();
        Long id = saveAndFlush(project("862BR-AGO-2026", clientId)).getId();

        Project found = projects.findById(id).orElseThrow();

        assertThat(found.getCode().value()).isEqualTo("862BR-AGO-2026");
        assertThat(found.getClientId()).isEqualTo(clientId);
        assertThat(found.getBudget()).isEqualTo(new Money(new BigDecimal("1234.50"), Currency.PEN));
        assertThat(found.getParticipation().value()).isEqualByComparingTo("0.5");
        assertThat(found.getSalesCommissionRate().value()).isEqualByComparingTo("0.03");
        assertThat(found.getStatus()).isEqualTo(ProjectStatus.QUOTED);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(projects.existsByCode(new ProjectCode("862br-ago-2026"))).isTrue();
    }

    @Test
    void progressRecordsArePersistedWithTheProjectAndOnePerMonth() {
        Project project = project("862BR-AGO-2026", clientId());
        project.start();
        project.registerProgress(new Period(2026, 8), new Percentage(new BigDecimal("0.4")));
        project.registerProgress(new Period(2026, 9), new Percentage(new BigDecimal("0.35")));
        Long id = saveAndFlush(project).getId();
        em.clear();

        Project found = projects.findById(id).orElseThrow();

        assertThat(found.getProgressRecords()).hasSize(2);
        assertThat(found.accumulatedProgress().value()).isEqualByComparingTo("0.75");
        assertThat(found.hasProgressFor(new Period(2026, 9))).isTrue();
    }

    @Test
    void costEstimatesArePersistedWithTheProjectAlongsideProgress() {
        Project project = project("862BR-AGO-2026", clientId());
        project.start();
        project.registerProgress(new Period(2026, 8), new Percentage(new BigDecimal("0.4")));
        project.setCostEstimate(CostCategory.MATERIALS, new Money(new BigDecimal("300"), Currency.PEN));
        project.setCostEstimate(CostCategory.LABOR, new Money(new BigDecimal("200.25"), Currency.PEN));
        Long id = saveAndFlush(project).getId();
        em.clear();

        Project found = projects.findById(id).orElseThrow();

        assertThat(found.getCostEstimates()).hasSize(2);
        assertThat(found.getProgressRecords()).hasSize(1);
        assertThat(found.estimatedDirectCost()).isEqualTo(new Money(new BigDecimal("500.25"), Currency.PEN));
    }

    @Test
    void codeIsUnique() {
        Long clientId = clientId();
        saveAndFlush(project("862BR-AGO-2026", clientId));

        assertThatThrownBy(() -> saveAndFlush(project("862BR-AGO-2026", clientId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsAnUnknownClient() {
        assertThatThrownBy(() -> saveAndFlush(project("001BR-ENE-2027", 999_999L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
