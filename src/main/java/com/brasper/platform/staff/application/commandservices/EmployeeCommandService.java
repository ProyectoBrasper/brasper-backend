package com.brasper.platform.staff.application.commandservices;

import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.staff.domain.model.aggregates.Employee;
import com.brasper.platform.staff.domain.model.commands.HireEmployeeCommand;
import com.brasper.platform.staff.domain.model.commands.TerminateEmployeeCommand;
import com.brasper.platform.staff.domain.model.commands.UpdateEmployeeCommand;
import com.brasper.platform.staff.domain.repositories.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeCommandService {

    private final EmployeeRepository employees;

    public EmployeeCommandService(EmployeeRepository employees) {
        this.employees = employees;
    }

    @Transactional
    public Result<Employee, EmployeeCommandFailure> handle(HireEmployeeCommand command) {
        if (command.dni() != null && employees.existsByDni(command.dni())) {
            return Result.failure(EmployeeCommandFailure.DUPLICATE_DNI);
        }
        return Result.success(employees.save(new Employee(command)));
    }

    @Transactional
    public Result<Employee, EmployeeCommandFailure> handle(UpdateEmployeeCommand command) {
        Employee employee = employees.findById(command.employeeId()).orElse(null);
        if (employee == null) {
            return Result.failure(EmployeeCommandFailure.EMPLOYEE_NOT_FOUND);
        }
        if (!employee.isActive()) {
            return Result.failure(EmployeeCommandFailure.ALREADY_TERMINATED);
        }
        employee.update(command);
        return Result.success(employees.save(employee));
    }

    @Transactional
    public Result<Employee, EmployeeCommandFailure> handle(TerminateEmployeeCommand command) {
        Employee employee = employees.findById(command.employeeId()).orElse(null);
        if (employee == null) {
            return Result.failure(EmployeeCommandFailure.EMPLOYEE_NOT_FOUND);
        }
        if (!employee.isActive()) {
            return Result.failure(EmployeeCommandFailure.ALREADY_TERMINATED);
        }
        if (command.terminatedOn().isBefore(employee.getHiredOn())) {
            return Result.failure(EmployeeCommandFailure.INVALID_TERMINATION_DATE);
        }
        employee.terminate(command.terminatedOn());
        return Result.success(employees.save(employee));
    }
}
