package com.brasper.platform.staff.interfaces.rest.transform;

import com.brasper.platform.staff.domain.model.aggregates.Employee;
import com.brasper.platform.staff.interfaces.rest.resources.EmployeeResource;

public final class EmployeeResourceAssembler {

    private EmployeeResourceAssembler() {
    }

    public static EmployeeResource toResource(Employee employee) {
        return new EmployeeResource(employee.getId(), employee.getFullName(), employee.getDni().value(),
                employee.getPosition(), employee.getMonthlySalary().amount(),
                employee.getMonthlySalary().currency().name(), employee.getHiredOn(), employee.getTerminatedOn(),
                employee.isActive());
    }
}
