package com.brasper.platform.projects.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brasper.platform.projects.domain.model.aggregates.Client;
import com.brasper.platform.projects.domain.events.ProjectCompletedEvent;
import com.brasper.platform.projects.domain.model.aggregates.Project;
import com.brasper.platform.projects.domain.model.commands.CancelProjectCommand;
import com.brasper.platform.projects.domain.model.commands.CreateClientCommand;
import com.brasper.platform.projects.domain.model.commands.CreateProjectCommand;
import com.brasper.platform.projects.domain.model.commands.RegisterProjectProgressCommand;
import com.brasper.platform.projects.domain.model.commands.SetProjectCostEstimateCommand;
import com.brasper.platform.projects.domain.model.commands.StartProjectCommand;
import com.brasper.platform.projects.domain.model.commands.UpdateProjectCommand;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ProjectCommandServiceTest {

    private static final ProjectCode CODE = new ProjectCode("862BR-AGO-2026");

    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final ClientRepository clients = mock(ClientRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ProjectCommandService service = new ProjectCommandService(projects, clients, events);

    private static CreateProjectCommand create() {
        return new CreateProjectCommand(CODE, "Planta", null, 1L, new Money(BigDecimal.TEN, Currency.PEN),
                LocalDate.of(2026, 8, 3), new Percentage(BigDecimal.ONE), new Percentage(new BigDecimal("0.03")));
    }

    private Project storedProject() {
        Project project = new Project(create());
        when(projects.findById(5L)).thenReturn(Optional.of(project));
        when(projects.save(project)).thenReturn(project);
        return project;
    }

    @Test
    void createStoresAQuotedProjectForAnExistingClient() {
        when(clients.findById(1L)).thenReturn(Optional.of(new Client(
                new CreateClientCommand("Linings", new Ruc("20123456789"), null, null, null))));
        when(projects.existsByCode(CODE)).thenReturn(false);
        when(projects.save(any(Project.class))).thenAnswer(call -> call.getArgument(0));

        Project project = service.handle(create()).success().orElseThrow();

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.QUOTED);
        assertThat(project.getClientId()).isEqualTo(1L);
    }

    @Test
    void createFailsForAnUnknownClientOrADuplicateCode() {
        when(clients.findById(1L)).thenReturn(Optional.empty());
        assertThat(service.handle(create()).failure()).contains(ProjectCommandFailure.CLIENT_NOT_FOUND);

        when(clients.findById(1L)).thenReturn(Optional.of(mock(Client.class)));
        when(projects.existsByCode(CODE)).thenReturn(true);
        assertThat(service.handle(create()).failure()).contains(ProjectCommandFailure.DUPLICATE_CODE);
        verify(projects, never()).save(any());
    }

    @Test
    void startAndCancelFollowTheStatusRules() {
        Project project = storedProject();

        assertThat(service.handle(new StartProjectCommand(5L)).isSuccess()).isTrue();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(service.handle(new StartProjectCommand(5L)).failure())
                .contains(ProjectCommandFailure.INVALID_STATUS_TRANSITION);

        assertThat(service.handle(new CancelProjectCommand(5L)).isSuccess()).isTrue();
        assertThat(service.handle(new CancelProjectCommand(5L)).failure())
                .contains(ProjectCommandFailure.INVALID_STATUS_TRANSITION);
    }

    @Test
    void updateChangesAnOpenProjectAndFailsOnACancelledOne() {
        Project project = storedProject();
        var update = new UpdateProjectCommand(5L, "Nuevo", null, new BigDecimal("99"),
                new Percentage(new BigDecimal("0.5")), new Percentage(BigDecimal.ZERO));

        assertThat(service.handle(update).isSuccess()).isTrue();
        assertThat(project.getName()).isEqualTo("Nuevo");

        project.cancel();
        assertThat(service.handle(update).failure()).contains(ProjectCommandFailure.INVALID_STATUS_TRANSITION);
    }

    private static RegisterProjectProgressCommand progress(int month, String percentage) {
        return new RegisterProjectProgressCommand(5L, new Period(2026, month),
                new Percentage(new BigDecimal(percentage)));
    }

    @Test
    void registersProgressAndCompletesTheProjectAtOneHundredPercent() {
        Project project = storedProject();
        project.start();

        var first = service.handle(progress(8, "0.6")).success().orElseThrow();
        assertThat(first.getPercentage().value()).isEqualByComparingTo("0.6");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);

        verify(events, never()).publishEvent(any(ProjectCompletedEvent.class));

        service.handle(progress(9, "0.4"));
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        verify(events).publishEvent(any(ProjectCompletedEvent.class));
    }

    @Test
    void progressFailuresSayWhichRuleBrokeAndStoreNothing() {
        Project project = storedProject();
        assertThat(service.handle(progress(8, "0.1")).failure())
                .contains(ProjectCommandFailure.INVALID_STATUS_TRANSITION);

        project.start();
        service.handle(progress(8, "0.7"));
        assertThat(service.handle(progress(8, "0.1")).failure())
                .contains(ProjectCommandFailure.PERIOD_ALREADY_REGISTERED);
        assertThat(service.handle(progress(9, "0.31")).failure())
                .contains(ProjectCommandFailure.PROGRESS_EXCEEDS_LIMIT);
        assertThat(project.getProgressRecords()).hasSize(1);

        when(projects.findById(9L)).thenReturn(Optional.empty());
        assertThat(service.handle(new RegisterProjectProgressCommand(9L, new Period(2026, 8),
                new Percentage(BigDecimal.ONE))).failure()).contains(ProjectCommandFailure.PROJECT_NOT_FOUND);
    }

    private static SetProjectCostEstimateCommand estimate(Currency currency) {
        return new SetProjectCostEstimateCommand(5L, CostCategory.MATERIALS, new Money(BigDecimal.TEN, currency));
    }

    @Test
    void setsACostEstimateAndReturnsThePersistedOne() {
        Project project = storedProject();

        var estimate = service.handle(estimate(Currency.PEN)).success().orElseThrow();

        assertThat(estimate.getCategory()).isEqualTo(CostCategory.MATERIALS);
        assertThat(project.estimatedDirectCost().amount()).isEqualByComparingTo("10");
    }

    @Test
    void costEstimateFailuresSayWhichRuleBrokeAndStoreNothing() {
        Project project = storedProject();
        assertThat(service.handle(estimate(Currency.USD)).failure())
                .contains(ProjectCommandFailure.COST_CURRENCY_MISMATCH);

        project.cancel();
        assertThat(service.handle(estimate(Currency.PEN)).failure())
                .contains(ProjectCommandFailure.INVALID_STATUS_TRANSITION);
        assertThat(project.getCostEstimates()).isEmpty();

        when(projects.findById(9L)).thenReturn(Optional.empty());
        assertThat(service.handle(new SetProjectCostEstimateCommand(9L, CostCategory.OTHER,
                new Money(BigDecimal.ONE, Currency.PEN))).failure()).contains(ProjectCommandFailure.PROJECT_NOT_FOUND);
    }

    @Test
    void changesToAnUnknownProjectFail() {
        when(projects.findById(9L)).thenReturn(Optional.empty());

        assertThat(service.handle(new StartProjectCommand(9L)).failure())
                .contains(ProjectCommandFailure.PROJECT_NOT_FOUND);
        assertThat(service.handle(new CancelProjectCommand(9L)).failure())
                .contains(ProjectCommandFailure.PROJECT_NOT_FOUND);
    }
}
