package com.brasper.platform.projects.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.projects.domain.model.commands.CreateProjectCommand;
import com.brasper.platform.projects.domain.model.commands.UpdateProjectCommand;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectCode;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectStatus;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProjectTest {

    private static CreateProjectCommand create(BigDecimal budget, String participation, String commission) {
        return new CreateProjectCommand(new ProjectCode("862BR-AGO-2026"), " Linings planta ", null, 1L,
                new Money(budget, Currency.USD), LocalDate.of(2026, 8, 3),
                new Percentage(new BigDecimal(participation)), new Percentage(new BigDecimal(commission)));
    }

    private static Project project() {
        return new Project(create(new BigDecimal("1000"), "1", "0.03"));
    }

    private static UpdateProjectCommand update(String name, String budget) {
        return new UpdateProjectCommand(1L, name, "desc", new BigDecimal(budget), new Percentage(new BigDecimal("0.5")),
                new Percentage(new BigDecimal("0.02")));
    }

    @Test
    void startsQuotedWithTheGivenData() {
        Project project = project();

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.QUOTED);
        assertThat(project.getName()).isEqualTo("Linings planta");
        assertThat(project.getDescription()).isNull();
        assertThat(project.getBudget()).isEqualTo(new Money(new BigDecimal("1000.00"), Currency.USD));
    }

    @Test
    void rejectsInvalidData() {
        assertThatThrownBy(() -> new Project(create(BigDecimal.ZERO, "1", "0.03")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Project(create(BigDecimal.TEN, "0", "0.03")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Project(create(BigDecimal.TEN, "1.5", "0.03")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Project(create(BigDecimal.TEN, "1", "1.5")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateKeepsTheCurrencyAndTheCode() {
        Project project = project();

        project.update(update("Nuevo nombre", "2500.505"));

        assertThat(project.getName()).isEqualTo("Nuevo nombre");
        assertThat(project.getBudget()).isEqualTo(new Money(new BigDecimal("2500.51"), Currency.USD));
        assertThat(project.getParticipation().value()).isEqualByComparingTo("0.5");
        assertThat(project.getCode().value()).isEqualTo("862BR-AGO-2026");
    }

    @Test
    void followsTheStatusRules() {
        Project project = project();

        project.start();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThatThrownBy(project::start).isInstanceOf(IllegalStateException.class);

        project.cancel();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CANCELLED);
        assertThatThrownBy(project::cancel).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(project::start).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> project.update(update("X", "10"))).isInstanceOf(IllegalStateException.class);
    }
}
