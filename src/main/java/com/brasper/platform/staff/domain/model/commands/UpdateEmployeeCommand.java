package com.brasper.platform.staff.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.Money;

public record UpdateEmployeeCommand(Long employeeId, String position, Money monthlySalary) { }
