package com.brasper.platform.staff.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.staff.domain.model.commands.HireEmployeeCommand;
import com.brasper.platform.staff.domain.model.commands.UpdateEmployeeCommand;
import com.brasper.platform.staff.domain.model.valueobjects.Dni;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EmployeeTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 1);
    private static Money salary(String value) { return new Money(new BigDecimal(value), Currency.PEN); }
    private static Employee hire(String name, Money salary) {
        return new Employee(new HireEmployeeCommand(name, new Dni("01234567"), "Engineer", salary, DATE));
    }
    @Test
    void hiresAndUpdatesEmployee() {
        Employee employee = hire(" Ana Perez ", salary("1000"));
        assertThat(employee).extracting(Employee::getFullName, Employee::getDni, Employee::getPosition,
                Employee::getMonthlySalary, Employee::getHiredOn, Employee::isActive, Employee::getTerminatedOn)
                .containsExactly("Ana Perez", new Dni("01234567"), "Engineer", salary("1000"), DATE, true, null);
        employee.update(new UpdateEmployeeCommand(null, "Manager", salary("2000")));
        assertThat(employee).extracting(Employee::getPosition, Employee::getMonthlySalary)
                .containsExactly("Manager", salary("2000"));
    }
    @Test
    void rejectsInvalidHiringAndUpdatesWithoutPartialChanges() {
        for (String name : new String[]{null, " ", "x".repeat(201)})
            assertThatThrownBy(() -> hire(name, salary("1000"))).isInstanceOf(IllegalArgumentException.class);
        Employee employee = hire("Ana", salary("1000"));
        for (String amount : new String[]{"0", "-1", "1000000000000.00"}) {
            assertThatThrownBy(() -> hire("Ana", salary(amount))).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> employee.update(new UpdateEmployeeCommand(null, "Manager", salary(amount))))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> employee.update(new UpdateEmployeeCommand(null, "x".repeat(101), salary("2000"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(employee).extracting(Employee::getPosition, Employee::getMonthlySalary)
                .containsExactly("Engineer", salary("1000"));
    }
    @Test
    void terminatesOnHiringDateAndRejectsFurtherChanges() {
        Employee employee = hire("Ana", salary("1000"));
        assertThatThrownBy(() -> employee.terminate(DATE.minusDays(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> employee.terminate(null)).isInstanceOf(NullPointerException.class);
        assertThat(employee.isActive()).isTrue();
        employee.terminate(DATE);
        assertThat(employee.isActive()).isFalse();
        assertThat(employee.getTerminatedOn()).isEqualTo(DATE);
        assertThatThrownBy(() -> employee.terminate(DATE)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> employee.update(new UpdateEmployeeCommand(null, "Manager", salary("2000"))))
                .isInstanceOf(IllegalStateException.class);
    }
}
