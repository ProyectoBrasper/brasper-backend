package com.brasper.platform.staff.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.staff.domain.model.aggregates.Employee;
import com.brasper.platform.staff.domain.model.commands.HireEmployeeCommand;
import com.brasper.platform.staff.domain.model.commands.UpdateEmployeeCommand;
import com.brasper.platform.staff.domain.model.valueobjects.Dni;
import com.brasper.platform.staff.domain.repositories.EmployeeRepository;
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
class EmployeeJpaRepositoryTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 1);
    private static final Money SALARY = new Money(new BigDecimal("1234.56"), Currency.USD);
    @Autowired private EmployeeRepository employees;
    @Autowired private EntityManager em;

    private Employee hire(String dni) {
        return employees.save(new Employee(new HireEmployeeCommand("Ana", new Dni(dni), "Engineer", SALARY, DATE)));
    }
    @Test
    void roundTripsAndFiltersEmployees() {
        Employee active = hire("01234567");
        Employee terminated = hire("87654321");
        terminated.terminate(DATE.plusDays(1));
        em.flush(); em.clear();
        Employee found = employees.findByDni(new Dni("01234567")).orElseThrow();
        assertThat(found).extracting(Employee::getId, Employee::getFullName, Employee::getPosition,
                Employee::getMonthlySalary, Employee::getHiredOn, Employee::isActive)
                .containsExactly(active.getId(), "Ana", "Engineer", SALARY, DATE, true);
        assertThat(found.getCreatedAt()).isNotNull().isEqualTo(found.getUpdatedAt());
        var createdAt = found.getCreatedAt();
        found.update(new UpdateEmployeeCommand(found.getId(), "Manager", SALARY));
        em.flush(); em.clear();
        Employee updated = employees.findById(found.getId()).orElseThrow();
        assertThat(updated.getPosition()).isEqualTo("Manager");
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        assertThat(employees.existsByDni(new Dni("01234567"))).isTrue();
        assertThat(employees.existsByDni(new Dni("11111111"))).isFalse();
        assertThat(employees.findByDni(new Dni("11111111"))).isEmpty();
        assertThat(employees.findAll()).hasSize(2);
        assertThat(employees.findAllByActive(true)).extracting(Employee::getId).containsExactly(active.getId());
        assertThat(employees.findAllByActive(false)).extracting(Employee::getTerminatedOn).containsExactly(DATE.plusDays(1));
    }
    @Test
    void rejectsDuplicateDni() {
        hire("01234567");
        em.flush(); em.clear();
        assertThatThrownBy(() -> { hire("01234567"); em.flush(); })
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
