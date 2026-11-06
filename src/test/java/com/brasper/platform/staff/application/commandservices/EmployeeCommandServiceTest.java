package com.brasper.platform.staff.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.staff.domain.model.aggregates.Employee;
import com.brasper.platform.staff.domain.model.commands.HireEmployeeCommand;
import com.brasper.platform.staff.domain.model.commands.TerminateEmployeeCommand;
import com.brasper.platform.staff.domain.model.commands.UpdateEmployeeCommand;
import com.brasper.platform.staff.domain.model.valueobjects.Dni;
import com.brasper.platform.staff.domain.repositories.EmployeeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EmployeeCommandServiceTest {

    private static final LocalDate HIRED = LocalDate.of(2026, 3, 1);

    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final EmployeeCommandService service = new EmployeeCommandService(employees);

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private static HireEmployeeCommand hire() {
        return new HireEmployeeCommand("Ana Perez", new Dni("12345678"), "Engineer", pen("3000"), HIRED);
    }

    private Employee stored() {
        Employee employee = new Employee(hire());
        when(employees.findById(1L)).thenReturn(Optional.of(employee));
        when(employees.save(any(Employee.class))).thenAnswer(call -> call.getArgument(0));
        return employee;
    }

    @Test
    void hiresAndRejectsADuplicateDni() {
        when(employees.save(any(Employee.class))).thenAnswer(call -> call.getArgument(0));
        when(employees.existsByDni(new Dni("12345678"))).thenReturn(false, true);

        assertThat(service.handle(hire()).isSuccess()).isTrue();
        assertThat(service.handle(hire()).failure()).contains(EmployeeCommandFailure.DUPLICATE_DNI);
    }

    @Test
    void updatesAnActiveEmployee() {
        Employee employee = stored();

        service.handle(new UpdateEmployeeCommand(1L, "Lead", pen("4000"))).success().orElseThrow();

        assertThat(employee.getPosition()).isEqualTo("Lead");
        assertThat(employee.getMonthlySalary()).isEqualTo(pen("4000"));
    }

    @Test
    void terminatesOnceAndRejectsEarlyDatesAndLaterChanges() {
        Employee employee = stored();

        assertThat(service.handle(new TerminateEmployeeCommand(1L, HIRED.minusDays(1))).failure())
                .contains(EmployeeCommandFailure.INVALID_TERMINATION_DATE);
        assertThat(service.handle(new TerminateEmployeeCommand(1L, HIRED.plusMonths(8))).isSuccess()).isTrue();
        assertThat(employee.isActive()).isFalse();
        assertThat(service.handle(new TerminateEmployeeCommand(1L, HIRED.plusMonths(9))).failure())
                .contains(EmployeeCommandFailure.ALREADY_TERMINATED);
        assertThat(service.handle(new UpdateEmployeeCommand(1L, "Lead", pen("4000"))).failure())
                .contains(EmployeeCommandFailure.ALREADY_TERMINATED);
    }

    @Test
    void reportsAnUnknownEmployee() {
        when(employees.findById(9L)).thenReturn(Optional.empty());

        assertThat(service.handle(new UpdateEmployeeCommand(9L, null, pen("1"))).failure())
                .contains(EmployeeCommandFailure.EMPLOYEE_NOT_FOUND);
        assertThat(service.handle(new TerminateEmployeeCommand(9L, HIRED)).failure())
                .contains(EmployeeCommandFailure.EMPLOYEE_NOT_FOUND);
    }
}
